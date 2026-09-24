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

### Changed

- Root settings now include the native build modules.
- `core:app` can compile the new Compose UI layer.

### Not yet verified

- End-to-end physical-device Hello World build/install.
- Kotlin compilation in the native build engine.
- Full dependency resolution.
- R8.
- NDK build.
