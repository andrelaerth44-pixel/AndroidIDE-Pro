# AndroidIDE Pro — NDK Support

## Goal

Make C/C++ a first-class on-device development target.

## Toolchain

Target architecture:

```text
NDK Manager
   ↓
LLVM / Clang
   ↓
clangd
   ↓
CMake
   ↓
Native Build Engine
   ↓
.so / .a
   ↓
APK
```

## Languages

Initial targets:

- C11/C17
- C++17/C++20

Newer language modes are added when the on-device toolchain supports them reliably.

## ABI

- arm64-v8a
- armeabi-v7a
- x86
- x86_64

## Features

- syntax highlighting
- clangd completion
- navigation
- diagnostics
- CMake support
- JNI
- native packaging

## JNI Wizard

Future wizard can generate Kotlin/Java declarations plus C/C++ JNI boilerplate.

## Native build modules

Planned separation:

- androidide-build-native
- androidide-build-java
- androidide-build-kotlin
- androidide-build-compose
- androidide-build-resources

## Performance

NDK tools must be installed/downloaded independently and cached by version and ABI.

Do not embed every NDK toolchain into the base APK.
