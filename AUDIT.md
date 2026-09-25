# AndroidIDE Pro — Verified Repository Audit

Date: 2026-09-25

## Baseline

- Before-commit: d673b02875228f2fc19be5c37e2c464bc1c40295
- Current HEAD: 0b91d8dec2a5bed610fa1d279bd4f41c7614955c
- Commits between baseline and HEAD: 271
- Files changed: 105
- Added lines: 9,552
- Deleted lines: 519
- Added files: 89
- Modified files: 16

## Before

The baseline was the existing AndroidIDE architecture: XML home/editor surfaces, the legacy AndroidTreeView file tree, Sora editor lifecycle, Java/XML language servers, Gradle BuildService, Gradle-oriented project templates, existing AAPT2 infrastructure and the existing APK signing/install infrastructure.

## Current UI

- Compose + Material 3 workspace foundation.
- Compose home shell and command palette.
- Compose explorer with filtering/search and file actions.
- Compose editor workspace header.
- Real editor tabs mirrored into Compose and closable through the existing editor lifecycle.
- Breadcrumbs and status information.
- Build Center with live status, logs, problems and cancellation.
- Toolchain Manager with filesystem discovery.
- Legacy editor infrastructure is intentionally retained underneath the new presentation.

## Current native model

- Native project/module/target/source-set model.
- Build variants and library types.
- arm64-v8a is the first implemented native ABI.
- .androidide/native.json configuration.
- Deterministic source scanning and native build graph.

## Current native compiler backend

- app-level Clang/Clang++/LLD/llvm-ar backend.
- C17 and C++20 compilation.
- Android target triple generation.
- Native ProcessBuilder execution with streaming output.
- Race-safe cancellation.
- Async NativeBuildService.
- Native build tasks are visible in Build Center.

## Current JNI

- Java native-method detection.
- javac -h header generation.
- Generated JNI headers added to native include paths.
- JNI project generator.
- JNI App wizard is registered in the existing template provider.
- Kotlin external fun header generation is not implemented.

## Current clangd

- compile_commands.json.
- clangd process session and JSON-RPC transport.
- Request/response routing and notification routing.
- initialize, didOpen, didChange, didClose, completion, shutdown and exit messages.
- Diagnostic mapping to AndroidIDE DiagnosticResult.
- Completion mapping to AndroidIDE CompletionResult.
- Native ILanguageServer adapter is registered through LspHandler.
- C/C++ document events are synchronized with clangd.

Not yet implemented in the adapter: request cancellation, formatting, definition, references and signature help.

## Current templates

- NativeActivity project wizard is registered.
- JNI App project wizard is registered.
- Both generate Gradle-free native.json configuration.

## Current APK packaging

- AAPT2 compile planning.
- AAPT2 resource link planning.
- Native APK package planner/executor.
- Native shared-library merge into lib/arm64-v8a/lib<module>.so.

Still missing from this native path: Java/Kotlin compilation, DEX, zipalign, v2/v3 signing and installation.

## Critical finding: duplicate native backends

The repository contains two native execution models:

1. app/src/main/java/com/itsaky/androidide/native/build
   Direct Clang/Clang++/LLD/llvm-ar backend used by the editor Native Build action.

2. subprojects/build-engine
   NativeBuildEngine plus NdkBuild and a custom NdkToolchain expecting libllvmtools.so and libld-gnu-lld.so.

The second backend is included in settings.gradle.kts but is not an app dependency in app/build.gradle.kts. It is therefore not the backend used by the editor Native Build action.

No matching libllvmtools.so or libld-gnu-lld.so payload is present in the repository tree.

These two backends need to converge to one primary native architecture.

## Critical finding: Gradle is still the actual normal build path

The normal Build Center Build action still calls BuildService.executeTasks(assembleDebug). Native Build is a separate command. Therefore the native backend is not yet the primary AndroidIDE Pro APK backend.

## Critical finding: current native APK path is not end-to-end

Current flow:

AAPT2 compile -> AAPT2 link -> native library ZIP merge

Missing:

Java/Kotlin compile -> DEX -> zipalign -> APK signing -> install

Existing Gradle infrastructure still supplies the normal complete APK pipeline.

## Toolchain reality

ToolchainManager and NativeToolchainLocator perform real filesystem discovery, but the repository does not contain a complete Android NDK/LLVM payload. Native builds depend on external toolchain provisioning.

## Validation reality

The latest GitHub Actions run for HEAD 0b91d8de is queued. No successful or failed CI result for the current HEAD was observed during this audit.

Therefore the branch is not yet CI-validated.

## What is implemented

- Compose workspace foundation.
- Explorer and editor workspace presentation.
- Build Center integration.
- Toolchain discovery UI.
- Native project model.
- Native build graph.
- Direct Clang build executor.
- JNI header generation.
- compile_commands generation.
- clangd transport/session/adapter foundation.
- NativeActivity wizard.
- JNI App wizard.
- AAPT2/native APK package planning and native merge.

## What is integrated

- Compose workspace is integrated with real editor lifecycle.
- Build Center receives real Gradle events.
- Native Build feeds real native task state/output into Build Center.
- clangd is registered in the existing language-server registry.
- clangd consumes native document events and publishes mapped diagnostics.
- native templates are registered in the existing template provider.

## What is not end-to-end

- Native backend as the complete APK producer.
- Complete bundled NDK/toolchain.
- Java/Kotlin plus DEX in the native graph.
- zipalign/sign/install in the native graph.
- Multiple native ABIs.
- LLDB/debugger/profiler.

## Next engineering order

1. Stabilize CI against current HEAD.
2. Choose one native backend and eliminate the duplicate model.
3. Define and ship the AndroidIDE Pro native toolchain/NDK payload.
4. Add Java/Kotlin compilation and DEX to the native graph.
5. Complete unsigned APK assembly with native libraries.
6. Add zipalign and v2/v3 signing.
7. Make Native Build the main Build Center backend; keep Gradle as compatibility mode.
8. Expand ABI support.
9. Add LLDB/debugger and profiler.

## Status terminology

Implemented = code exists and has focused tests.
Integrated = connected to the real AndroidIDE lifecycle.
End-to-end = a real user flow can exercise the complete subsystem.
Validated = current CI/device build proves it.

Do not treat file existence as proof of integration or end-to-end operation.
