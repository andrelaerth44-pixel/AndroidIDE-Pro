# AndroidIDE Pro — Changelog

## Unreleased

### Added

- Native Build Engine modules:
  - `core/build-api`
  - `core/build-engine`
  - `core/android-build`
- Initial Android TaskGraph.
- AAPT2 compile/link pipeline.
- Java compilation path.
- D8 path.
- APK package, zipalign and debug signing.
- Workspace → native build adapter.
- Native Build action in the editor toolbar.
- Compose Material 3 Pro Home.
- Compose Build Center.
- APK install action from Build Center.
- Resizable application workspace for phone/tablet.
- Engineering documentation under `docs/`.
- Built-in language core for Java, Kotlin, C, C++, XML, JSON and Markdown.
- Immutable built-in language backend contract with no language register/unregister API.
- Native-only Build Router with no Gradle language fallback.
- Embedded `JavacTool`/nb-javac Java compilation.
- Embedded Kotlin compiler invocation through `K2JVMCompiler`.
- Native C/C++ compilation task with Android-hosted LLVM toolchain contract.
- Native library packaging for `arm64-v8a`.

### Changed

- Root settings now include the native build modules.
- `core:app` can compile the new Compose UI layer.

### Not yet verified

- End-to-end physical-device Hello World build/install.
- Physical validation of embedded Kotlin and Java compiler paths.
- Distribution of the Android-hosted LLVM binaries.
- Full dependency resolution.
- R8.
- NDK build.
