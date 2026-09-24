# AndroidIDE Pro — Native C/C++ Support

## Escopo

C e C++ são linguagens de primeira classe do AndroidIDE Pro e funcionam em:

1. aplicativos somente C;
2. aplicativos somente C++;
3. C + C++ no mesmo projeto;
4. Java/Kotlin + C/C++ com JNI.

O build do projeto não usa Gradle.

## Arquitetura

~~~text
src/main/cpp
    ↓
NativeLanguageScanner
    ↓
CompileNativeTask
    ├── C17 / clang
    ├── C++20 / clang++
    ├── JNI headers
    ├── NativeActivity glue
    └── native configuration
    ↓
LLD
    ↓
lib/<ABI>/lib<name>.so
    ↓
APK
~~~

## Core LLVM

O compilador que executa no dispositivo é um LLVM/Clang/LLD construído para Android arm64.

O Core LLVM Toolchain contém clang, clang++, clangd, LLD, LLVM shared libraries, libc++, clang resource directory, AArch64 sysroot, runtime builtins/unwind e android_native_app_glue.

## C

Fontes:

~~~text
.c
~~~

Headers reconhecidos pelo IDE:

~~~text
.h
.inc
.def
~~~

Padrão: C17.

## C++

Fontes:

~~~text
.cc
.cpp
.cxx
.cppm
.ixx
~~~

Headers:

~~~text
.hh
.hpp
.hxx
.ipp
.inl
.tpp
~~~

Padrão: C++20 com libc++.

## Projeto somente nativo

Quando não existem fontes Java/Kotlin:

- javac é pulado;
- Kotlin é pulado;
- D8 é pulado;
- AAPT2 continua processando o manifesto/resources;
- a biblioteca nativa é empacotada;
- APK é alinhado e assinado.

Para NativeActivity, o Pro pode compilar automaticamente o android_native_app_glue.c e gerar libmain.so.

## Projeto híbrido

~~~text
Java/Kotlin
  ↓
javac -h
  ↓
generated/jni/
  ↓
clang / clang++
  ↓
libappnative.so
  ↓
System.loadLibrary()
  ↓
JNI
~~~

Os headers JNI entram automaticamente no include path do compilador nativo.

## Configuração nativa

Arquivo:

~~~text
src/main/cpp/androidide-native.properties
~~~

Exemplo:

~~~properties
libraryName=brushengine
includeDirs=include,third_party/foo/include
libraryDirs=third_party/foo/lib
linkLibraries=log,android,EGL,GLESv3
staticLibraries=libs/libbrush.a
cFlags=-Wall -Wextra
cppFlags=-Wall -Wextra -fno-rtti
linkerFlags=-Wl,--gc-sections
~~~

As paths relativas começam em src/main/cpp.

## Bibliotecas nativas

Suporte atual:

~~~text
src/main/jniLibs/arm64-v8a/*.so
src/main/cpp/**/*.a
~~~

Dependências AAR já resolvidas pelo Workspace também podem fornecer JNI, assets e runtime JARs.

## Compile database

Cada build nativo gera:

~~~text
build/androidide/intermediates/native/debug/compile_commands.json
~~~

O arquivo usa as mesmas flags do build real.

## clangd

O Core LLVM Pack já contém clangd Android-hosted.

A integração do processo clangd com o LSP/Editor Pro ainda é uma etapa de IDE. O compilador e o compile_commands já estão prontos.

## ABI atual

O primeiro target é arm64-v8a / aarch64-linux-android.

Outras ABIs serão adicionadas depois.

## Validação física pendente

~~~text
Hello C
  → build
  → install
  → launch
  → native code

Hello C++
  → build
  → install
  → launch
  → native code

Kotlin + C++
  → javac/JNI
  → C++
  → signed APK
  → install
  → JNI call
~~~

