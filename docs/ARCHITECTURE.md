# AndroidIDE Pro — Architecture

## High-level

```text
UI / Compose
     ↓
IDE Services
     ↓
Workspace / ProjectModel
     ↓
Build Router
 ┌───┴─────────────────────┐
Native Build            Gradle Fallback
     ↓
Native TaskGraph
     ↓
Android Toolchain
```

## Layers

### UI

Kotlin + Jetpack Compose + Material 3.

A UI screen should not execute compiler internals directly.

### IDE Services

Coordinates project lifecycle, files, LSP, diagnostics, Git, build and installation.

### Workspace

The existing AndroidIDE workspace model remains authoritative during the migration. We should not create a parallel project model unless a concrete incompatibility requires an adapter.

### Build Router

Decides whether a project can use the native backend.

```text
NativeCompatible?
    ├── yes → NativeAndroidBuildSystem
    └── no  → Gradle backend
```

### Native Build Engine

Pure build logic where possible. Android Context and UI concerns stay outside this layer.

## Package boundaries

```text
core/build-api
    contracts only

core/build-engine
    task graph / cache / scheduling

core/android-build
    Android-specific build tasks

core/app
    Workspace adapter + UI + lifecycle
```

## Reuse rule

Prefer adapting existing AndroidIDE components over replacing them without evidence.

Keep and evolve:

- editor
- LSP
- terminal
- Git
- Workspace
- XML tooling
- existing indexing

Replace only components whose architecture blocks on-device goals or modern UX.

## Performance rule

Heavy operations must be:

- lazy
- incremental
- cancellable
- cache-aware

No persistent daemon should exist solely because a task was once executed.
