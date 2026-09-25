<p align="center">
  <img src="./images/icon.png" alt="AndroidIDE" width="80" height="80"/>
</p>

<h2 align="center"><b>AndroidIDE</b></h2>
<p align="center">
  An on-device Android IDE track focused on a lightweight native build backend, CodeAssist-inspired workflow, and a simple modern workspace.
<p><br>

<p align="center">
<!-- Latest release -->
<img src="https://img.shields.io/github/v/release/AndroidIDEOfficial/AndroidIDE?include_prereleases&amp;label=latest%20release" alt="Latest release">
<!-- Build and test -->
<img src="https://github.com/AndroidIDEOfficial/AndroidIDE/actions/workflows/build.yml/badge.svg" alt="Builds and tests">
<!-- CodeFactor -->
<img src="https://www.codefactor.io/repository/github/androidideofficial/androidide/badge/main" alt="CodeFactor">
<!-- Crowdin -->
<a href="https://crowdin.com/project/androidide"><img src="https://badges.crowdin.net/androidide/localized.svg" alt="Crowdin"></a>
<!-- License -->
<img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License"></p>

<p align="center">
  <a href="https://androidide.com/docs/">Explore the docs »</a> &nbsp; &nbsp;
  <a href="https://androidide.com/blogs/">Read our blog »</a>
</p>

<p align="center">
  <a href="https://github.com/AndroidIDEOfficial/AndroidIDE/issues">Report a bug</a> &nbsp; &#8226; &nbsp;
  <a href="https://github.com/AndroidIDEOfficial/AndroidIDE/issues">Request a feature</a> &nbsp; &#8226; &nbsp;
  <a href="https://t.me/androidide_discussions">Join us on Telegram</a>
</p>

## Features

- [x] Gradle support.
- [x] `JDK 11` and `JDK 17` available for use.
- [x] Terminal with necessary packages.
- [x] Custom environment variables (for Build & Terminal).
- [x] SDK Manager (Available via terminal).
- [x] API information for classes and their members (since, removed, deprecated).
- [ ] Language servers
    - [x] Java
    - [x] XML
    - [ ] Kotlin
- [ ] UI Designer
    - [x] Layout inflater
    - [x] Resolve resource references
    - [x] Auto-complete resource values when user edits attributes using the attribute editor
    - [x] Drag & Drop
    - [x] Visual attribute editor
    - [x] Android Widgets
- [ ] String Translator
- [ ] Asset Studio (Drawable & Icon Maker)
- [x] Git

## AndroidIDE Pro Development Track

This repository contains the AndroidIDE Pro development track. The current work focuses on a modern on-device IDE workspace built with Jetpack Compose and Material 3 while preserving the existing editor, project, and language infrastructure during the migration.

### Working branch

Current development branch:

`compose-glass-foundation`

The branch is intentionally incremental. Existing AndroidIDE editor services remain in place while the Compose workspace is introduced around them.

### Current UI direction

- Simple, fast, organized IDE workspace.
- Jetpack Compose + Material 3 for new UI.
- Light glass surfaces with restrained transparency.
- Shared IDE icons through `IdeIcons`.
- No neon/RGB/cyberpunk styling.
- Existing functionality is reused instead of being rewritten without a concrete reason.

### AndroidIDE Pro progress

Current sprint: **Native Build Foundation**.

Implemented in the current branch:

- Compose project/file Explorer with expandable tree, state restoration, file filtering and row context actions.
- Compose editor workspace header backed by the real editor tab state.
- Closable editor tabs connected to the existing `EditorHandlerActivity` file lifecycle.
- Modified-file indicators and save action.
- File breadcrumbs derived from the active project path.
- Status bar data for language, device ABI, Git branch, cursor position and editor state.
- Command Palette with grouped actions, search, keywords and shortcut labels.
- Build Center UI with pipeline state, live status/progress, structured Problems and logs.
- Shared Compose icon set and glass surface primitives.
- Native Project Model with ABI, build variant, library type, source set, module and target models.
- Native Build Graph with dependency validation, cycle detection, semantic topological ordering and ready-task calculation.
- Native source scanning and deterministic C/C++ object planning.
- Native command generation for C17/C++20, Android ARM64 targeting, libc++, LLD and llvm-ar.
- Native build executor with asynchronous service, real process cancellation and Build Center integration.
- Clangd `compile_commands.json` generation from the same compiler commands.
- Clangd launch planning and JSON-RPC `Content-Length` transport foundation.
- Real JNI `javac -h` header-generation stage with explicit JDK/javac validation.
- Reusable JNI project template generator with a C++ JNI bridge.
- Reusable NativeActivity project template with native entrypoint and manifest.
- Gradle-free `.androidide/native.json` project configuration with automatic discovery fallback.
- Native toolchain discovery for Clang, Clang++, Clangd, LLD, LLDB, llvm-ar and libc++.
- Toolchain Manager connected to real filesystem discovery instead of hard-coded readiness.
- arm64-v8a native library packaging stage that merges `lib/arm64-v8a/*.so` into an unsigned APK.

The visible editor-tab presentation is now Compose-driven, while the legacy `TabLayout` remains internally available so existing selection and editor lifecycle code continues to work.

### Build architecture direction

AndroidIDE Pro is moving toward a native on-device build pipeline inspired by CodeAssist, with explicit stages for resource processing, Java/Kotlin compilation, native compilation, dexing, packaging, alignment, signing and installation.

The current branch does **not** yet contain a completed Toolchain Manager/Clangd/NDK product. The existing AndroidIDE tool extraction layer remains useful infrastructure, while the native toolchain will be introduced as a separate AndroidIDE Pro build stack.

The project must not regress into making Gradle the planned primary build backend for the AndroidIDE Pro architecture.

### Immediate next work

1. Let the queued GitHub Actions validation run and fix any compile errors it reports.
2. Connect the clangd JSON-RPC transport to the existing `ILanguageServer` / editor lifecycle.
3. Add clangd `initialize`, document sync, diagnostics, completion and shutdown handling.
4. Add NativeActivity template integration to the existing project-template wizard.
5. Turn the native APK library merge stage into a complete unsigned-package pipeline with resource/APK merge.
6. Add zip alignment and signing as explicit post-package stages.
7. Make the native pipeline the primary path while keeping Gradle only as compatibility infrastructure.
8. Expand the toolchain to remaining ABIs, then debugger and profiler work.

### Continuity rule

Repository documentation is part of the project's continuity mechanism. After meaningful architectural or UI changes, update this section and `STATUS.md` so a future development session can continue from the repository instead of relying on conversation history.

## Installation

> _Please install AndroidIDE from trusted sources only i.e._
> - [_The AndroidIDE website_](https://androidide.com)
> - [_GitHub Releases_](https://github.com/AndroidIDEOfficial/AndroidIDE/releases)
> - [_GitHub Actions_](https://github.com/AndroidIDEOfficial/AndroidIDE/actions)

- Download the AndroidIDE APK
  from [releases](https://github.com/AndroidIDEOfficial/AndroidIDE/releases). You
  can also download APKs
  from [GitHub actions](https://github.com/AndroidIDEOfficial/AndroidIDE/actions).
- Follow the
  instructions [here](https://androidide.com/docs/installation/) to
  install the build tools.

## Limitations

- For working with projects in AndroidIDE, your project must use Android Gradle Plugin v7.2.0 or
  newer. Projects with older AGP must be migrated to newer versions.
- SDK Manager is already included in Android SDK and is accessible in AndroidIDE via its Terminal.
  But, you cannot use it to install some tools (like NDK) because those tools are not built for
  Android.
- No official NDK support because we haven't built the NDK for Android.

The app is still being developed actively. It's in beta stage and may not be stable. if you have any
issues using the app, please let us know.

## Contributing

See the [contributing guide](./CONTRIBUTING.md).

For translations, visit the [Crowdin project page](https://crowdin.com/project/androidide).

## Thanks to

- [Rosemoe](https://github.com/Rosemoe) for the
  awesome [CodeEditor](https://github.com/Rosemoe/sora-editor)
- [Termux](https://github.com/termux) for [Terminal Emulator](https://github.com/termux/termux-app)
- [Bogdan Melnychuk](https://github.com/bmelnychuk)
  for [AndroidTreeView](https://github.com/bmelnychuk/AndroidTreeView)
- [George Fraser](https://github.com/georgewfraser) for
  the [Java Language Server](https://github.com/georgewfraser/java-language-server)

Thanks to all the developers who have contributed to this project.

## Contact Us

- [Website](https://androidide.com)
- [Telegram](https://t.me/androidide_discussions)

## License

```
AndroidIDE is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

AndroidIDE is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
```

Any violations to the license can be reported either by opening an issue or writing a mail to us
directly.
