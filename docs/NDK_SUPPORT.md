# AndroidIDE Pro — Native C/C++ Support

## Escopo atual

C e C++ são capacidades nativas do AndroidIDE Pro.

Primeiro alvo:

- C17;
- C++20;
- Android arm64;
- ABI arm64-v8a;
- geração de .so;
- empacotamento automático no APK.

O caminho de build não usa Gradle.

## Arquitetura

~~~text
src/main/cpp
    ↓
NativeLanguageScanner
    ↓
CompileNativeTask
    ↓
Core LLVM Toolchain
    ├── clang
    ├── clang++
    └── lld
    ↓
libappnative.so
    ↓
lib/arm64-v8a/
    ↓
APK
~~~

## Android-hosted LLVM

O compilador precisa executar no próprio Android. Por isso o Pro não tenta executar o clang Linux/macOS/Windows de um NDK desktop.

O Core LLVM Toolchain é construído para Android arm64 e distribuído em core/toolchain-llvm e tools/llvm-toolchain.

## C

Extensões:

~~~text
.c
.h
~~~

Default:

~~~text
-std=c17
-fPIC
-O2
-fdata-sections
-ffunction-sections
-fstack-protector-strong
-DANDROID
~~~

## C++

Extensões:

~~~text
.cc
.cpp
.cxx
.hh
.hpp
.hxx
~~~

Default:

~~~text
-std=c++20
-fPIC
-O2
-fdata-sections
-ffunction-sections
-fstack-protector-strong
-DANDROID
-stdlib=libc++
~~~

O runtime libc++_shared.so é incluído no APK em lib/arm64-v8a quando C++ está presente.

## Páginas de 16 KiB

O linker recebe:

~~~text
-Wl,-z,max-page-size=16384
-Wl,-z,common-page-size=16384
~~~

## Source discovery

O Build Engine varre src/main/cpp e reconhece as extensões nativas suportadas.

Arquivos desconhecidos com extensão são reportados pelo scanner.

## Integrado

- descoberta C/C++;
- C17;
- C++20;
- compile + link;
- objetos determinísticos;
- arm64-v8a;
- .so;
- libc++_shared.so;
- packaging;
- TaskGraph;
- fingerprints;
- diagnóstico de ausência do Core LLVM.

## Próximas capacidades

Ainda separadas do primeiro vertical slice:

- CMake;
- múltiplas ABIs;
- clangd;
- JNI Wizard;
- debugging nativo;
- static libraries;
- native unit-test runner;
- LLDB;
- profiling.

## Validação final

A implementação só será marcada como fisicamente validada depois de:

~~~text
Hello C project
  → Build
  → signed APK
  → install
  → launch
  → native code executed

Hello C++ project
  → Build
  → signed APK
  → install
  → launch
  → native code executed
~~~

Essa etapa depende de execução em Android arm64 real ou ambiente Android equivalente.
