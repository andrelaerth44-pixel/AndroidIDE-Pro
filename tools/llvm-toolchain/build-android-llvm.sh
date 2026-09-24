#!/usr/bin/env bash
set -euo pipefail

# AndroidIDE Pro — Android-hosted LLVM toolchain builder.
#
# This produces the compiler/linker that RUN on an Android arm64 device.
# It is deliberately not the desktop NDK host toolchain. The desktop NDK is
# used only as a source of headers, runtime libraries, and a cross C++ runtime.
#
# The resulting layout is consumed by :core:toolchain-llvm:
#   out/
#     jniLibs/arm64-v8a/*.so
#     assets/toolchain/{sysroot,lib-clang,native_app_glue}/...
#
# Expected build host: Linux x86_64.
# Typical space requirement: 12–16 GB free.
#
# Examples:
#   ./build-android-llvm.sh all
#   ./build-android-llvm.sh package

LLVM_VERSION="${LLVM_VERSION:-18.1.8}"
NDK_VERSION="${NDK_VERSION:-r27c}"
ANDROID_API="${ANDROID_API:-26}"
LLVM_TARGETS="${LLVM_TARGETS:-AArch64}"
JOBS="${JOBS:-$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)}"

ROOT="${WORK:-$(pwd)/build}"
DOWNLOADS="$ROOT/downloads"
LLVM_SRC="$ROOT/llvm-project-$LLVM_VERSION.src"
NDK_ROOT="$ROOT/android-ndk-$NDK_VERSION"
HOST_BUILD="$ROOT/host-tools"
ANDROID_BUILD="$ROOT/android-tools"
OUT="${OUT:-$ROOT/out}"

CMAKE="${CMAKE:-cmake}"
NINJA="${NINJA:-ninja}"

log() {
  printf '\n==> %s\n' "$*"
}

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing host command: $1"
}

check_host() {
  [[ "$(uname -s)" == "Linux" ]] || die "The Android arm64 toolchain builder currently targets Linux hosts."
  [[ "$(uname -m)" == "x86_64" ]] || die "The CI builder currently expects Linux x86_64."
  for c in curl tar unzip "$CMAKE" "$NINJA" perl awk sed find; do
    need_cmd "$c"
  done
}

require_disk() {
  local free_gb
  mkdir -p "$ROOT"
  free_gb="$(df -Pk "$ROOT" | awk 'NR==2 {printf "%d", $4 / 1024 / 1024}')"
  (( free_gb >= 12 )) || die "At least 12 GB free is recommended; only ${free_gb} GB is available."
}

fetch_sources() {
  mkdir -p "$DOWNLOADS"

  if [[ ! -d "$LLVM_SRC" ]]; then
    log "Downloading LLVM $LLVM_VERSION"
    local llvm_archive="$DOWNLOADS/llvm-$LLVM_VERSION.tar.xz"
    if [[ ! -f "$llvm_archive" ]]; then
      curl -fL --retry 4 --retry-delay 2 -o "$llvm_archive" \
        "https://github.com/llvm/llvm-project/releases/download/llvmorg-$LLVM_VERSION/llvm-project-$LLVM_VERSION.src.tar.xz"
    fi

    tar -C "$ROOT" -xf "$llvm_archive" \
      "llvm-project-$LLVM_VERSION.src/llvm" \
      "llvm-project-$LLVM_VERSION.src/clang" \
      "llvm-project-$LLVM_VERSION.src/lld" \
      "llvm-project-$LLVM_VERSION.src/cmake" \
      "llvm-project-$LLVM_VERSION.src/third-party"
  fi

  if [[ ! -d "$NDK_ROOT" ]]; then
    log "Downloading Android NDK $NDK_VERSION"
    local ndk_archive="$DOWNLOADS/android-ndk-$NDK_VERSION-linux.zip"
    if [[ ! -f "$ndk_archive" ]]; then
      curl -fL --retry 4 --retry-delay 2 -o "$ndk_archive" \
        "https://dl.google.com/android/repository/android-ndk-$NDK_VERSION-linux.zip"
    fi
    unzip -q "$ndk_archive" -d "$ROOT"
  fi
}

patch_llvm_for_android() {
  # LLVM 18 can reject the shared-Dylib configuration because a helper target
  # is linked once with keyword syntax and once without it. Apply the narrow
  # compatibility change only when the original form is still present.
  local file
  for file in \
    "$LLVM_SRC/lld/tools/lld/CMakeLists.txt" \
    "$LLVM_SRC/clang/cmake/modules/AddClang.cmake"; do
    [[ -f "$file" ]] || continue
    if grep -q 'target_link_libraries(obj.\${target} \${ARGN})' "$file"; then
      perl -0pi -e \
        's/target_link_libraries\(obj\.\$\{target\} \$\{ARGN\}\)/target_link_libraries(obj.\$\{target\} \$\{type\} \$\{ARGN\})/' \
        "$file"
    fi
  done

  # Android only needs ELF. Disable the other LLD flavors and make the
  # generated driver table agree with the libraries that remain.
  local lld_top="$LLVM_SRC/lld/CMakeLists.txt"
  local lld_tool="$LLVM_SRC/lld/tools/lld/CMakeLists.txt"
  local lld_main="$LLVM_SRC/lld/tools/lld/lld.cpp"
  if [[ -f "$lld_top" ]] && grep -q "^add_subdirectory(MachO)" "$lld_top"; then
    perl -pi -e 's/^add_subdirectory\\((COFF|MachO|MinGW|wasm)\\)/# removed for AndroidIDE Pro arm64 ELF build: $1/' "$lld_top"

    perl -0pi -e 's/lld_target_link_libraries\\(lld\\s+PRIVATE\\s+lldCommon\\s+lldCOFF\\s+lldELF\\s+lldMachO\\s+lldMinGW\\s+lldWasm\\s+\\)/lld_target_link_libraries(lld\\n  PRIVATE\\n  lldCommon\\n  lldELF\\n  )/s' "$lld_tool"

    perl -0pi -e 's/LLD_HAS_DRIVER\\(coff\\)\\nLLD_HAS_DRIVER\\(elf\\)\\nLLD_HAS_DRIVER\\(mingw\\)\\nLLD_HAS_DRIVER\\(macho\\)\\nLLD_HAS_DRIVER\\(wasm\\)/LLD_HAS_DRIVER(elf)\\n#undef LLD_ALL_DRIVERS\\n#define LLD_ALL_DRIVERS {{lld::Gnu, \\&lld::elf::link}}/' "$lld_main"
  fi
}

build_host_tools() {
  if [[ -x "$HOST_BUILD/bin/llvm-tblgen" && -x "$HOST_BUILD/bin/clang-tblgen" ]]; then
    return
  fi

  log "Building host tablegen tools"
  "$CMAKE" -G Ninja -S "$LLVM_SRC/llvm" -B "$HOST_BUILD" \
    -DCMAKE_BUILD_TYPE=Release \
    -DLLVM_ENABLE_PROJECTS=clang \
    -DLLVM_TARGETS_TO_BUILD="$LLVM_TARGETS" \
    -DLLVM_ENABLE_ASSERTIONS=OFF \
    -DLLVM_INCLUDE_TESTS=OFF \
    -DLLVM_INCLUDE_EXAMPLES=OFF \
    -DLLVM_INCLUDE_BENCHMARKS=OFF \
    -DLLVM_ENABLE_ZLIB=OFF \
    -DLLVM_ENABLE_ZSTD=OFF \
    -DLLVM_ENABLE_LIBXML2=OFF \
    -DLLVM_ENABLE_TERMINFO=OFF \
    -DLLVM_ENABLE_LIBEDIT=OFF

  "$NINJA" -C "$HOST_BUILD" -j "$JOBS" \
    llvm-tblgen clang-tblgen
}

configure_android_tools() {
  patch_llvm_for_android
  local ndk_toolchain="$NDK_ROOT/build/cmake/android.toolchain.cmake"

  log "Configuring Android arm64 LLVM tools"
  "$CMAKE" -G Ninja -S "$LLVM_SRC/llvm" -B "$ANDROID_BUILD" \
    -DCMAKE_SYSTEM_NAME=Android \
    -DCMAKE_ANDROID_NDK="$NDK_ROOT" \
    -DCMAKE_TOOLCHAIN_FILE="$ndk_toolchain" \
    -DCMAKE_SYSTEM_VERSION="$ANDROID_API" \
    -DCMAKE_ANDROID_ARCH_ABI=arm64-v8a \
    -DCMAKE_ANDROID_STL_TYPE=c++_shared \
    -DCMAKE_BUILD_TYPE=MinSizeRel \
    -DCMAKE_INSTALL_PREFIX=/toolchain \
    -DLLVM_ENABLE_PROJECTS="clang;lld" \
    -DLLVM_TARGETS_TO_BUILD="$LLVM_TARGETS" \
    -DLLVM_NATIVE_TOOL_DIR="$HOST_BUILD/bin" \
    -DLLVM_TABLEGEN="$HOST_BUILD/bin/llvm-tblgen" \
    -DCLANG_TABLEGEN="$HOST_BUILD/bin/clang-tblgen" \
    -DLLVM_BUILD_LLVM_DYLIB=ON \
    -DLLVM_LINK_LLVM_DYLIB=ON \
    -DLLVM_DYLIB_SONAME=libLLVM.so \
    -DCLANG_LINK_CLANG_DYLIB=ON \
    -DCLANG_DYLIB_SONAME=libclang-cpp.so \
    -DLLVM_TOOL_LLVM_DRIVER_BUILD=ON \
    -DLLVM_ENABLE_ASSERTIONS=OFF \
    -DLLVM_INCLUDE_TESTS=OFF \
    -DLLVM_INCLUDE_EXAMPLES=OFF \
    -DLLVM_INCLUDE_BENCHMARKS=OFF \
    -DLLVM_INCLUDE_DOCS=OFF \
    -DLLVM_ENABLE_BINDINGS=OFF \
    -DLLVM_ENABLE_PLUGINS=OFF \
    -DLLVM_ENABLE_ZLIB=OFF \
    -DLLVM_ENABLE_ZSTD=OFF \
    -DLLVM_ENABLE_LIBXML2=OFF \
    -DLLVM_ENABLE_TERMINFO=OFF \
    -DLLVM_ENABLE_LIBEDIT=OFF \
    -DLLVM_ENABLE_LIBPFM=OFF \
    -DCLANG_ENABLE_STATIC_ANALYZER=OFF \
    -DCLANG_ENABLE_ARCMT=OFF \
    -DCLANG_PLUGIN_SUPPORT=OFF \
    -DCLANG_TOOL_CLANG_IMPORT_TEST_BUILD=OFF \
    -DCMAKE_FIND_ROOT_PATH_MODE_PROGRAM=NEVER \
    -DCMAKE_FIND_ROOT_PATH_MODE_LIBRARY=ONLY \
    -DCMAKE_FIND_ROOT_PATH_MODE_INCLUDE=ONLY \
    -DCMAKE_EXE_LINKER_FLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384" \
    -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384" \
    -DCMAKE_MODULE_LINKER_FLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"

  log "Building Android clang/lld/llvm-driver"
  "$NINJA" -C "$ANDROID_BUILD" -j "$JOBS" clang lld llvm-driver
}

copy_first() {
  local destination="$1"
  shift
  local pattern candidate
  for pattern in "$@"; do
    if [[ -f "$pattern" ]]; then
      cp "$pattern" "$destination"
      return
    fi
    while IFS= read -r candidate; do
      if [[ -f "$candidate" ]]; then
        cp "$candidate" "$destination"
        return
      fi
    done < <(compgen -G "$pattern" || true)
  done
  die "Required file was not found: $destination"
}

package_toolchain() {
  rm -rf "$OUT"
  mkdir -p "$OUT/jniLibs/arm64-v8a" "$OUT/assets/toolchain"

  local driver="$ANDROID_BUILD/bin/llvm"
  [[ -x "$driver" ]] || die "llvm-driver output is missing: $driver"

  cp "$driver" "$OUT/jniLibs/arm64-v8a/libllvmtools.so"
  cp "$driver" "$OUT/jniLibs/arm64-v8a/libld-gnu-lld.so"

  copy_first "$OUT/jniLibs/arm64-v8a/libLLVM.so" \
    "$ANDROID_BUILD/lib/libLLVM.so" \
    "$ANDROID_BUILD/lib/libLLVM.so.*"

  copy_first "$OUT/jniLibs/arm64-v8a/libclang-cpp.so" \
    "$ANDROID_BUILD/lib/libclang-cpp.so" \
    "$ANDROID_BUILD/lib/libclang-cpp.so.*"

  local ndk_prebuilt
  ndk_prebuilt="$NDK_ROOT/toolchains/llvm/prebuilt/$(ls "$NDK_ROOT/toolchains/llvm/prebuilt" | head -1)"
  copy_first "$OUT/jniLibs/arm64-v8a/libc++_shared.so" \
    "$(find "$ndk_prebuilt" -path '*/aarch64/libc++_shared.so' -print -quit)"

  log "Collecting clang resource directory"
  cp -R "$ANDROID_BUILD/lib/clang" "$OUT/assets/toolchain/lib-clang"

  local ndk_clang_runtime="$ndk_prebuilt/lib/clang/${LLVM_VERSION%%.*}/lib/linux"
  local runtime_out="$OUT/assets/toolchain/lib-clang/${LLVM_VERSION%%.*}/lib/linux/aarch64"
  mkdir -p "$runtime_out"

  copy_first "$OUT/assets/toolchain/lib-clang/${LLVM_VERSION%%.*}/lib/linux/libclang_rt.builtins-aarch64-android.a" \
    "$ndk_clang_runtime/libclang_rt.builtins-aarch64-android.a"

  copy_first "$runtime_out/libunwind.a" \
    "$ndk_clang_runtime/aarch64/libunwind.a"

  log "Collecting arm64 sysroot"
  local ndk_sysroot="$ndk_prebuilt/sysroot"
  mkdir -p "$OUT/assets/toolchain/sysroot/usr"
  cp -R "$ndk_sysroot/usr/include" "$OUT/assets/toolchain/sysroot/usr/include"

  local target_lib_dir="$OUT/assets/toolchain/sysroot/usr/lib/aarch64-linux-android"
  mkdir -p "$target_lib_dir/$ANDROID_API"
  cp -R "$ndk_sysroot/usr/lib/aarch64-linux-android/$ANDROID_API/." "$target_lib_dir/$ANDROID_API/"
  find "$ndk_sysroot/usr/lib/aarch64-linux-android" -maxdepth 1 -type f \
    \( -name '*.a' -o -name '*.so' \) -exec cp {} "$target_lib_dir/" \;

  local glue="$NDK_ROOT/sources/android/native_app_glue"
  if [[ -d "$glue" ]]; then
    mkdir -p "$OUT/assets/toolchain/native_app_glue"
    cp "$glue/android_native_app_glue.c" \
       "$glue/android_native_app_glue.h" \
       "$glue/NOTICE" \
       "$OUT/assets/toolchain/native_app_glue/"
  fi

  # APKs cannot preserve arbitrary symlink trees in the way a desktop LLVM
  # install expects, so the AndroidIDE Pro runtime uses two copies of the
  # same multicall driver under deterministic names.
  chmod 0755 "$OUT/jniLibs/arm64-v8a/libllvmtools.so"
  chmod 0755 "$OUT/jniLibs/arm64-v8a/libld-gnu-lld.so"

  local strip="$ndk_prebuilt/bin/llvm-strip"
  for lib in "$OUT"/jniLibs/arm64-v8a/*.so; do
    "$strip" --strip-all "$lib" || true
  done

  log "LLVM toolchain pack created"
  du -sh "$OUT/jniLibs" "$OUT/assets"
  find "$OUT/jniLibs/arm64-v8a" -maxdepth 1 -type f -printf '%f %s bytes\n' | sort
}

case "${1:-all}" in
  deps)
    check_host
    require_disk
    fetch_sources
    ;;
  native)
    check_host
    build_host_tools
    ;;
  cross)
    check_host
    configure_android_tools
    ;;
  package)
    check_host
    package_toolchain
    ;;
  all)
    check_host
    require_disk
    fetch_sources
    build_host_tools
    configure_android_tools
    package_toolchain
    ;;
  *)
    die "Usage: $0 [deps|native|cross|package|all]"
    ;;
esac
