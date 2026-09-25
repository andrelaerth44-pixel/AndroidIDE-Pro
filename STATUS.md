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
| Build Center V2 | UI implemented | Pipeline, progress, problems and logs are modeled; real build-event bridge remains. |
| Command Palette | In progress | Search, categories, keywords and shortcuts are modeled; more real commands/files remain. |
| Status Bar | Implemented | Language, ABI, Git branch, cursor position and editor state are modeled. |
| Git status | Partial | Current branch is read from the project repository when available. |
| UI foundation | Implemented | Glass surfaces and shared IDE icons are available to Compose UI. |

## Important implementation decisions

The Compose migration is incremental. The existing editor container and file lifecycle remain authoritative.

The visible editor tab presentation is now Compose-driven, while the underlying `TabLayout` is retained internally so existing selection and editor lifecycle code continues to work.

New UI should continue to prefer Compose over adding new XML layouts.

The workspace UI should stay restrained: simple surfaces, clear hierarchy, modest corner radii and minimal transparency.

## Recent commits

- `f8ad812acb035235712fc9fc0f5cccf1af5cf230` — Explorer context menu actions.
- `9a5fff55cdab11a9313bc744dc2e4f51a29675a1` — shared icon use in the Compose shell.
- `a465556d87e45cf24cb5f4c7f6c1b7f26fa9c858` — expose real editor tab/breadcrumb/status state to Compose.
- `67f2e8bb57d6f47271689da073fb64e9774c4dd5` — closable editor tabs, breadcrumbs and status bar UI.
- `a37aa4d8ab1830b35afee5b35c4e14b57d807ab4` — connect Compose tab closing to real editor instances.
- `756099715bae5a6dd713c14a4cc09e9fa8ff6435` — clarify Command Palette dismissal hint.
- `628be33bcfe423468ae4f70dfea3879bf3addec8` — update project README with Workspace V2 continuity state.

## Immediate next work

1. Validate the latest GitHub Actions build and inspect any compile errors.
2. Polish Explorer context interactions and preserve existing file-action flows.
3. Finish wiring Build Center to real build events/log streams.
4. Make Command Palette execute real project/file/build actions.
5. Continue Workspace V2 cleanup before moving into Toolchain Manager and C/C++ tooling.
6. Keep NDK implementation behind the UI sprint unless a concrete integration dependency requires it.

## NDK boundary

Native build engine and NDK-related source already exist in the branch, but they are not treated as the completed NDK product.

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
