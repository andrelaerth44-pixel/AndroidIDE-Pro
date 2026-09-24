# AndroidIDE Pro — Tasks

Legenda:

- [ ] não iniciado
- [~] em andamento
- [x] concluído

## Build Engine

[x] Build API  
[x] TaskGraph  
[x] fingerprints SHA-256 persistentes  
[x] invalidação por output ausente  
[x] AndroidModule adapter  
[x] AAPT2 compile/link  
[x] Java build path — JavacTool/nb-javac  
[x] Kotlin build path — Core Kotlin Toolchain classloader  
[x] C/C++ build path — Core LLVM Toolchain  
[x] D8 path  
[x] projeto puramente nativo sem D8 desnecessário  
[x] APK package  
[x] zipalign  
[x] debug signing  
[x] Build Center  
[x] APK install action  
[x] Native-only BuildRouter  
[~] AAR/JAR dependency/resource edge cases  
[ ] validação física de Hello World  
[ ] build cancellation  
[ ] structured task events  
[ ] parallel task execution  
[ ] persistent build output cache  
[ ] desugaring completo  
[ ] Compose compiler integration  
[ ] multidex de produção  
[ ] R8  
[ ] AAB  
[ ] release signing UI  

## UI Pro

[x] Compose  
[x] Material 3  
[x] Pro Home  
[x] Build Center  
[x] Core Toolchain Center  
[x] resizable workspace  
[x] built-in language core  
[ ] Project Explorer Pro  
[ ] Editor shell refresh  
[ ] tablet split layout  
[ ] command palette  
[ ] Problems panel  
[ ] Run/Install center  
[ ] theme manager  

## Core Toolchains

[x] Core Toolchain API  
[x] Core Kotlin Toolchain APK  
[x] Kotlin package classloader  
[x] Core LLVM toolchain model  
[x] Android arm64 LLVM build script  
[x] Core LLVM Toolchain APK module  
[x] Core Toolchain Center  
[x] pack checksum metadata  
[ ] online authenticated update  
[ ] resumable update  
[ ] multiple device ABIs  

## Native

[x] C source discovery  
[x] C++ source discovery  
[x] C17  
[x] C++20  
[x] Android-hosted clang/LLVM contract  
[x] lld integration  
[x] arm64-v8a .so packaging  
[ ] clangd integration  
[ ] CMake  
[ ] JNI Wizard  
[ ] ABI manager  
[ ] static libraries  
[ ] native unit-test runner  
[ ] LLDB/native debugging  
[ ] native profiler  

## Dependencies

[x] Workspace compile classpath connected to Java/Kotlin/D8  
[x] Android library resources enter AAPT2  
[~] complete AAR parsing/resource merging  
[ ] dependency artifact cache  
[ ] Maven resolver independent from Gradle  
[ ] version conflict solver  

## Extensibility

[x] Plugin API  
[x] plugin capability model excludes languages/builds  
[x] immutable built-in language registry  
[ ] permission model  
[ ] plugin sandbox  
[ ] asset provider manager  
[ ] theme providers  
[ ] tool providers  

## Documentation

[x] BUILD_ENGINE.md  
[x] LANGUAGE_SUPPORT.md  
[x] NDK_SUPPORT.md  
[x] TOOLCHAINS.md  
[x] ROADMAP.md  
[x] TASKS.md  
[x] CHANGELOG.md  
[x] DECISIONS.md  
[ ] architecture diagrams per subsystem  
[ ] API reference
