# AndroidIDE Pro — Current Development Status

Date: 2026-09-25

## Source of truth

This file is a working status snapshot for the AndroidIDE Pro development branch.

Branch: `compose-glass-foundation`

The repository README contains the broader project continuity notes. This file records the current sprint state and the immediate next steps.

## Active sprint

### Workspace V2

| Area | Status | Notes |
| --- | --- | --- |
| Explorer V2 | In progress | Compose tree, search/filtering, expansion state and row context actions are present. |
| Editor Host V2 | In progress | Compose host is integrated into the existing editor activity; tabs can select and close real editor instances. |
| Modified tabs | Implemented | Existing editor modification state is reflected in Compose tabs. |
| Breadcrumbs | Implemented | Active file path is exposed as Compose breadcrumbs. |
| Build Center V2 | Integrated | Direct BuildService start/stop, real prepare/progress/output/success/failure events, structured Problems and clickable source navigation. |
| Command Palette | Integrated | Real editor/project actions and currently open files are exposed alongside grouped search and shortcuts. |
| Status Bar | Implemented | Language, ABI, Git branch, cursor position and editor state are modeled. |
| Git status | Partial | Current branch is read from the project repository when available. |
| UI foundation | Implemented | Glass surfaces and shared IDE icons are available to Compose UI. |

## Important implementation decisions

The Compose migration is incremental. The existing editor container and file lifecycle remain authoritative.

The visible editor tab presentation is Compose-driven, while the underlying `TabLayout` is retained internally so existing selection and editor lifecycle code continues to work.

New UI should continue to prefer Compose over adding new XML layouts.

The workspace UI should stay restrained: simple surfaces, clear hierarchy, modest corner radii and minimal transparency.

## Recent implementation checkpoints

- Build Center now starts `assembleDebug` through `BuildService`, exposes cancellation through `cancelCurrentBuild()`, mirrors real build events/output and opens structured Problems at source locations.
- Compose editor tabs, breadcrumbs, status bar and Explorer actions remain connected to the existing editor lifecycle.
- Command Palette exposes real navigation, build, save and open-file actions.
- The next native sprint starts with Toolchain Manager and tool discovery; a completed NDK/Clangd stack is not yet present in this branch.

## Validation

GitHub Actions for the branch are currently queued/in progress because the workflow is configured to build and test on pushes. The latest repository state has not yet produced a completed workflow result.

Local repository cloning from this environment was not available because outbound GitHub DNS/network access is blocked, so local Gradle validation was not possible here.

## Immediate next work

1. Validate the latest GitHub Actions build and inspect any compile errors.
2. Polish Explorer interactions and workspace spacing.
3. Finish Command Palette coverage for real project/editor actions.
4. Complete Workspace V2 cleanup before Toolchain Manager.
5. Implement Toolchain Manager and real device tool discovery.
6. Add C/C++ templates, Clang/Clangd, JNI and NativeActivity support.
7. Implement the native compiler/NDK pipeline, then debugger and profiler work.

## NDK boundary

The branch does not yet contain a completed native compiler/NDK product. The planned native stack remains separated from the existing Gradle service so it can become the primary AndroidIDE Pro build backend later.

The planned native stack remains:

- C17
- C++20
- JNI
- NativeActivity
- arm64-v8a first
- later armeabi-v7a, x86 and x86_64
- later LLDB and native profiling

Do not jump the project into full NDK/toolchain work while the Workspace V2 foundation is still being validated.

## Continuity rule

After each meaningful repository change, update this file and the README continuity section with the new state, decisions and next point of work.
