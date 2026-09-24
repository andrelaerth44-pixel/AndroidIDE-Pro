# AndroidIDE Pro — Changelog

## Unreleased

### Added

- Core Toolchain Center.
- Core Kotlin Toolchain APK.
- Kotlin compiler package classloader.
- Core LLVM Toolchain APK module.
- Android-hosted LLVM/Clang/LLD builder para arm64.
- LLVM sysroot/resource/runtime packaging.
- C/C++ build task com C17/C++20.
- native-only APK packaging quando não há Java/Kotlin.
- output-aware TaskGraph fingerprints.
- deterministic debug signing key seed.
- checksum metadata para o LLVM toolchain.
- workflow dedicado para construir o Core LLVM Toolchain.

### Changed

- core/android-build não carrega mais kotlin-compiler-embeddable.
- Kotlin usa Core Kotlin Toolchain.
- C/C++ usa Core LLVM Toolchain.
- Build Center mostra o estado do LLVM.
- Build Router continua com uma única rota nativa.
- D8 é pulado quando não existe bytecode Java/Kotlin.
- debug keystore não depende de java.home/bin/keytool.
- documentação de toolchains, linguagens, NDK e decisões foi sincronizada.

### Still to validate

- build/install/launch físico de Hello World;
- C físico;
- C++ físico;
- Kotlin físico;
- Java físico;
- execução do workflow LLVM;
- tamanho real dos APKs;
- AAR/JAR edge cases;
- desugaring;
- Compose compiler;
- R8;
- AAB;
- múltiplas ABIs;
- CMake/JNI/clangd;
- Run/Debug integrado.

## Foundation

- Build API.
- TaskGraph.
- persistent fingerprints.
- AAPT2.
- JavacTool/nb-javac.
- D8.
- APK package/zipalign/sign.
- Workspace adapter.
- Compose Material 3 surfaces.
- built-in language core.
- immutable language registry.
- plugin capability model without language/build capabilities.
