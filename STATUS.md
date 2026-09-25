# AndroidIDE Pro — Current Development Status

Date: 2026-09-25

## Source of truth

This file is the working status snapshot for the AndroidIDE Pro development branch.

Branch: `compose-glass-foundation`

## Active sprint

### Native Build Foundation

The Compose workspace, Build Center, Command Palette and Toolchain Manager foundations are already integrated with the existing editor lifecycle.

### Completed in the current native sprint

#### Project model
- `AbiTarget` with canonical Android ABI name; arm64-v8a is implemented first.
- `BuildVariant`
- `NativeLibraryType`
- `NativeSourceSet`
- `NativeTarget`
- `NativeModule`
- `.androidide/native.json` project configuration and round-trip persistence.
- Native source discovery under `src/main`.

#### Build graph
- Typed deterministic build tasks.
- Dependency validation and cycle detection.
- Dependency-aware topological order.
- Shared-library and static-library branches selected from the requested ABI/variant target.

#### Native command layer
- Clang C17 compilation.
- Clang++ C++20 compilation with libc++.
- Android arm64-v8a target triple.
- LLD shared linking.
- llvm-ar static archive generation.
- Generated JNI header directory is available to C/C++ include paths.
- Canonical `lib/arm64-v8a/` output naming.

#### Process and service layer
- Real `ProcessBuilder` execution.
- Streaming process output.
- Race-safe native process cancellation.
- Single-build asynchronous `NativeBuildService`.
- Build Center task/progress/output integration.

#### JNI
- Java native-method detection.
- Real `javac -h` command planning/execution.
- Explicit failure when a configured JDK does not contain `javac`.
- Dependency-free JNI project generator producing a Java bridge and C++ implementation.

#### Clangd
- `compile_commands.json` generated from the exact native compile commands.
- Database written at the native module root.
- Clangd launch command planner.
- Persistent clangd process session with stderr capture and lifecycle control.
- JSON-RPC framing transport using UTF-8 and `Content-Length`.
- Core initialize, document open/change/close, completion, shutdown and exit message generation.
- `publishDiagnostics` mapping to `DiagnosticResult`.
- LSP completion mapping to AndroidIDE `CompletionResult`.
- Native `ILanguageServer` registration through `LspHandler`.
- Workspace lifecycle synchronization so asynchronous editor events do not create duplicate clangd sessions.

The clangd transport is now connected to AndroidIDE Pro's existing language-server registry. The adapter consumes C/C++ document events, routes diagnostics to the existing language client, and exposes completion through the existing `CompletionResult` model.

#### NativeActivity
- Standalone NativeActivity template generator.
- NativeActivity project is available in the existing project wizard.
- Java NativeActivity host.
- Minimal `ANativeActivity_onCreate` native entry point.
- Manifest metadata for the generated native shared library.

#### JNI project wizard
- JNI App project is available in the existing project wizard.
- Generates `NativeBridge.java`, `MainActivity.java` and `native_bridge.cpp`.
- Generates a launcher manifest and Gradle-free `.androidide/native.json`.

#### APK native packaging
- AAPT2 resource compile planner.
- AAPT2 resource link planner.
- Native APK package planner/executor.
- Native shared-library merge stage for unsigned APKs.
- ABI-aware entry path: `lib/arm64-v8a/lib<module>.so`.
- Native libraries are emitted as ZIP `STORED` entries.
- Merge occurs before signing; v2/v3 signatures must be generated afterward.

### Important architecture finding
- A second native backend also exists under `subprojects/build-engine`.
- The editor Native Build action currently uses the app-level `native/build` backend.
- These native backends must be reconciled before AndroidIDE Pro has one authoritative native build architecture.

## Validation state

GitHub Actions for the newest commits are currently queued. No successful or failed result for the latest native changes has been observed yet, so the repository should not be considered CI-validated at this point.

## Next work

1. Stabilize CI against the current HEAD.
2. Reconcile the app-level native backend with subprojects/build-engine.
3. Add clangd request cancellation plus formatting, definition and references support.
4. Add Java/Kotlin compilation and DEX to the native APK graph.
5. Add zip alignment and v2/v3 APK signing to the native pipeline.
6. Promote Native Build to the primary Build Center path; keep Gradle only as compatibility infrastructure.
7. Package and provision the complete AndroidIDE Pro LLVM/NDK toolchain.
8. Add additional ABIs, then LLDB/debugger and profiler support.

## Architecture direction

```
Project
  -> Project Model
  -> Build Graph
  -> Java/Kotlin Pipeline + Native Pipeline
  -> Resource Merge
  -> Dex
  -> Native Library Merge
  -> APK Alignment
  -> APK Signing
  -> Install
```

Gradle must not become the planned primary AndroidIDE Pro backend.
