# AndroidIDE Pro — Current Development Status

Date: 2026-09-25

## Source of truth

Branch: `compose-glass-foundation`

Current HEAD:
`c7d99d9e8f2fdb1cfa4b082ac4aa5e4920f4065d`

## Active sprint

### Native Android Backend

The native backend now spans the entire intended project-build shape:

```
Project model
  -> resource processing
  -> Java/Kotlin compilation
  -> JNI/native compilation
  -> D8
  -> DEX merge
  -> native-library merge
  -> asset merge
  -> zipalign
  -> signing
  -> installation
```

The implementation is wired to Build Center, but the newest GitHub Actions runs are still queued, so this state is not yet CI/device validated.

## Completed

### Workspace
- Compose workspace shell.
- Explorer V2 foundation.
- Editor workspace/header.
- Command Palette.
- Build Center.
- Toolchain Manager.
- Legacy editor lifecycle retained underneath.

### Native compiler
- Native project model.
- Four Android ABI targets.
- Build variants.
- Shared/static library model.
- Deterministic source scanning.
- Dependency-aware native build graph.
- Clang C17.
- Clang++ C++20.
- LLD.
- llvm-ar.
- Process streaming and cancellation.
- Incremental native command cache with header-aware fingerprints.
- JNI `javac -h`.
- `compile_commands.json`.
- libc++ shared-runtime discovery/packaging.

### Clangd
- Persistent clangd session.
- JSON-RPC transport.
- Request/response router.
- initialize lifecycle.
- document synchronization.
- diagnostics.
- completion.
- definition.
- references.
- location mapping.
- `ILanguageServer` adapter registered in the existing LSP registry.

### Templates
- NativeActivity wizard.
- JNI App wizard.
- Both registered through the existing template provider.
- Generated projects are Gradle-free and include `.androidide/native.json`.
- Native library naming is shared between templates and the build model.

### Native APK
- AAPT2 compile/link.
- Java/Kotlin compilation.
- D8.
- DEX merge.
- Native library merge.
- libc++ runtime merge.
- assets merge.
- zipalign.
- debug keystore generation.
- apksigner.
- Build & Install action.
- Device ABI selection.

## Important architecture debt

The repository currently contains two native build architectures:

1. `app/native/build` — the backend actually used by the editor and Build Center.
2. `subprojects/build-engine` — a second generic/native engine containing `NdkBuild`.

The second engine is not currently the user-facing backend. These must be reconciled before declaring the native architecture finished.

## Validation state

- Current GitHub Actions runs for the latest changes are queued.
- No successful result for the current HEAD has been observed yet.
- No real-device APK smoke test has been observed yet.

## Next work

1. Get current CI green and fix any compiler/test failures.
2. Converge the two native backends into one architecture.
3. Provision/ship the complete AndroidIDE Pro LLVM/NDK toolchain.
4. Harden the native APK pipeline with broader dependency/resource/classpath handling.
5. Add clangd cancellation, formatting and richer LSP features.
6. Expand APK integration tests and device smoke tests.
7. Add LLDB/debugger/profiler.
8. Remove the need for Gradle in the AndroidIDE Pro user-project path, retaining it only as compatibility infrastructure.

Gradle must not become the planned primary backend for AndroidIDE Pro user projects.
