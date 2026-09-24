# AndroidIDE Pro — Roadmap

## Fase 0 — Fundação

[x] Native Build Engine
[x] Build API + TaskGraph
[x] built-in language core
[x] documentação

## Fase 1 — Native vertical slice

[x] Java build
[x] Kotlin build
[x] C build
[x] C++ build
[x] pure native projects
[x] hybrid JNI projects
[x] NativeActivity
[x] AAPT2
[x] D8
[x] APK packaging
[x] zipalign
[x] signing
[x] Build / Install / Run surfaces
[x] standalone native project descriptor
[x] pure/hybrid native project scaffolder
[ ] physical device validation

## Fase 2 — Core Toolchains

[x] Core Kotlin Toolchain
[x] Compose compiler hosted pack
[x] Android-hosted LLVM
[x] clang / clang++ / lld
[x] clangd packaging
[x] sysroot/runtime
[x] checksums
[ ] authenticated update service
[ ] update/resume
[ ] multiple device ABIs

## Fase 3 — Native IDE integration

[x] compile_commands.json
[~] clangd LSP integration
[ ] native diagnostics
[ ] native completion
[ ] native navigation
[ ] native refactoring
[ ] native test runner
[ ] LLDB

## Fase 4 — Android dependencies

[~] AAR/JAR handling
[ ] Maven resolver independent from Gradle
[ ] dependency cache
[ ] version conflict solver
[ ] desugaring
[ ] Compose runtime/compiler end-to-end
[ ] multidex
[ ] R8
[ ] AAB

## Fase 5 — Native project tooling

[ ] CMake importer/generator
[ ] ndk-build importer
[ ] JNI project wizard
[ ] NativeActivity project wizard
[ ] ABI manager
[ ] static-library project graph

## Fase 6 — Workspace Pro

[ ] Project Explorer Compose
[ ] recent projects
[ ] module/dependency view
[ ] structured build diagnostics
[ ] command palette
[ ] Problems panel
[ ] tablet workspace
[ ] theme manager

## Fase 7 — Native performance/debug

[ ] native profiler
[ ] LLDB
[ ] breakpoints
[ ] memory inspection
[ ] GPU/native diagnostics

## Fase 8 — Multi-language workspace

[ ] JavaScript / TypeScript
[ ] Python
[ ] Rust
[ ] Go
[ ] Bash
[ ] YAML
[ ] SQL
[ ] HTML/CSS
