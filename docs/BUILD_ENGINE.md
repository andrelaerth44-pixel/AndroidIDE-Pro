# AndroidIDE Pro — Native Build Engine

## Goal

Build Android applications on-device without requiring a Gradle daemon for the native-compatible path.

## Current pipeline

```text
mergeResourcesDebug
        ↓
aapt2CompileDebug
        ↓
aapt2LinkDebug
        ↓
generateBuildConfigDebug
        ↓
compileJavaDebug
        ↓
dexBuilderDebug
        ↓
packageApkDebug
        ↓
zipalignDebug
        ↓
signDebug
```

## Components

### build-api

Defines:

- BuildTask
- BuildSystem
- BuildRequest
- BuildResult
- BuildContext

### build-engine

Owns:

- TaskGraph
- dependencies
- up-to-date checks
- future cache/scheduling

### android-build

Owns Android-specific tasks and SDK tools.

### app adapter

`NativeBuildCoordinator` translates the existing Workspace/AndroidModule into the native build model.

## Current V1 limits

- debug application modules
- Java source path
- local resources
- basic assets
- no complete dependency resolver
- no Kotlin compiler path
- no R8
- no NDK
- no full flavors/product flavors

## Next engineering priorities

1. Make the Hello World path physically verifiable.
2. Add structured task events.
3. Replace timestamp-only checks with fingerprints.
4. Add persistent cache.
5. Add AAR/JAR dependency model.
6. Add Kotlin.
7. Add release/AAB.
