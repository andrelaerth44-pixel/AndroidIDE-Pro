# AndroidIDE Pro — Current Development Status

Date: 2026-09-25

## Source of truth

This file is a working status snapshot for the AndroidIDE Pro development branch.

Branch: `compose-glass-foundation`

## Active sprint

### Native Project Model Foundation

Concluded foundations:
- Workspace V2
- Explorer V2 base
- Editor Host V2 base
- Build Center V2 integration
- Command Palette integration
- Toolchain Manager base

New native model layer added:
- AbiTarget
- BuildVariant
- NativeLibraryType
- NativeSourceSet
- NativeTarget
- NativeModule

## Next work

1. Add Native Build Graph foundation.
2. Add NativeBuildRequest and NativeBuildResult.
3. Add NativePipeline stages.
4. Expand Toolchain Manager with real discovery.
5. Add JNI Wizard.
6. Add NativeActivity Wizard.
7. Begin Clang integration.

## Architecture direction

Project -> Project Model -> Build Graph -> Java/Kotlin Pipeline + Native Pipeline -> APK

Gradle must not become the primary AndroidIDE Pro backend.
