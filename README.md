# AndroidIDE Pro — Continuity and Development Notes

> AndroidIDE Pro is being developed as an on-device professional Android IDE. This fork is evolving its new workspace and build architecture incrementally while preserving the existing editor and project infrastructure.

## AndroidIDE Pro Development Track

The current work focuses on a modern on-device IDE workspace built with Jetpack Compose and Material 3 while preserving the existing editor, project, and language infrastructure during the migration.

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

### Workspace V2 progress

Current sprint: **Workspace V2**.

Implemented in the current branch:

- Compose project/file Explorer with expandable tree, state restoration, file filtering and row context actions.
- Compose editor workspace header backed by the real editor tab state.
- Closable editor tabs connected to the existing `EditorHandlerActivity` file lifecycle.
- Modified-file indicators and save action.
- File breadcrumbs derived from the active project path.
- Status bar data for language, device ABI, Git branch, cursor position and editor state.
- Command Palette with grouped actions, search, keywords and shortcut labels.
- Build Center UI with pipeline state, progress, problems and log models.
- Shared Compose icon set and glass surface primitives.

The visible editor-tab presentation is now Compose-driven, while the legacy `TabLayout` remains internally available so existing selection and editor lifecycle code continues to work.

### Build architecture direction

AndroidIDE Pro is moving toward a native on-device build pipeline inspired by CodeAssist, with explicit stages for resource processing, Java/Kotlin compilation, native compilation, dexing, packaging, alignment, signing and installation.

The native build engine/NDK work present in the branch is still experimental foundation work. It is **not** considered the completed NDK product and remains behind the Workspace V2 UI sprint.

The project must not regress into making Gradle the planned primary build backend for the AndroidIDE Pro architecture.

### Immediate next work

1. Validate the latest GitHub Actions build and fix any compile errors.
2. Polish Explorer context interactions and preserve existing file-action flows.
3. Bridge Build Center state to real build events and output streams.
4. Make Command Palette execute real file, build and navigation actions.
5. Finish Workspace V2 cleanup before Toolchain Manager and C/C++ tooling.
6. Add Clang/Clangd, C/C++ templates, JNI and NativeActivity tooling.
7. Return to the full NDK/toolchain implementation, then debugger and profiler work.

### Continuity rule

Repository documentation is part of the project's continuity mechanism. After meaningful architectural or UI changes, update this section and `STATUS.md` so a future development session can continue from the repository instead of relying on conversation history.

## Original AndroidIDE Documentation

The original AndroidIDE documentation, installation information, contributing notes and license remain below this development-track section.

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
