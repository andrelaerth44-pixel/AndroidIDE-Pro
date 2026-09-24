# NDK / C / C++

## Objetivo

Dar ao AndroidIDE Pro suporte real a projetos com código nativo.

## Componentes

- NDK Manager;
- toolchain registry;
- Clang/LLVM;
- CMake;
- JNI;
- ABI selection;
- native packaging.

## NDK Manager

Deverá permitir:

- detectar versões;
- instalar;
- remover;
- selecionar;
- verificar integridade.

## Project model

Native targets precisam representar:

- source roots;
- include paths;
- compiler flags;
- linker flags;
- C/C++ standard;
- CMake targets;
- ABI;
- minSdk;
- output.

## ABI inicial

- arm64-v8a;
- armeabi-v7a;
- x86_64;
- x86.

## Segurança

Ferramentas nativas baixadas precisam de origem conhecida e verificação de integridade.

## Estado

Ainda não implementado.
