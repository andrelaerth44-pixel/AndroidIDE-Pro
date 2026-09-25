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

The next step is to connect this transport to AndroidIDE Pro's existing language-server registry and editor client rather than building a second editor protocol.

#### NativeActivity
- Standalone NativeActivity template generator.
- Java NativeActivity host.
- Minimal `ANativeActivity_onCreate` native entry point.
- Manifest metadata for the generated native shared library.

#### APK native packaging
- Native shared-library merge stage for unsigned APKs.
- ABI-aware entry path: `lib/arm64-v8a/lib<module>.so`.
- Native libraries are emitted as ZIP `STORED` entries.
- Merge occurs before signing; v2/v3 signatures must be generated afterward.

## Validation state

GitHub Actions for the newest commits are currently queued. No successful or failed result for the latest native changes has been observed yet, so the repository should not be considered CI-validated at this point.

## Next work

1. Connect clangd JSON-RPC lifecycle to `ILanguageServer` and `ILanguageClient`.
2. Implement clangd initialize, document open/change, publishDiagnostics and completion requests.
3. Integrate NativeActivity template into the existing project-template wizard.
4. Build the unsigned APK packaging pipeline around resource compilation, Java/Dex outputs and native-library merge.
5. Add zip alignment and APK signing as explicit stages after native merge.
6. Promote the native backend to the primary AndroidIDE Pro build path; keep Gradle only as compatibility infrastructure.
7. Add additional ABIs, then LLDB/debugger and profiler support.

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
