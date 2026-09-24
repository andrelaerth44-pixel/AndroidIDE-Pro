# AndroidIDE Pro — Native Language Core

## Regra principal

Java, Kotlin, C e C++ pertencem ao núcleo do AndroidIDE Pro.

Não existe LanguagePlugin.

Não existe toggle de linguagem.

Não existe fallback silencioso para Gradle.

A linguagem é parte do produto quando o editor, os serviços de linguagem e o caminho de build/run fazem parte do núcleo.

## Arquitetura

~~~text
Editor
  ↓
BuiltInLanguageRegistry
  ↓
Language services
  ├── parsing
  ├── completion
  ├── diagnostics
  ├── navigation
  └── refactoring
  ↓
Native Build Engine
  ├── Java
  ├── Kotlin + Core Kotlin Toolchain
  └── C/C++ + Core LLVM Toolchain
~~~

## Linguagens incorporadas

| Linguagem | Editor | Build |
|---|---:|---:|
| Java | sim | sim |
| Kotlin | sim | sim, Core Kotlin Toolchain |
| C | sim | sim, Core LLVM Toolchain |
| C++ | sim | sim, Core LLVM Toolchain |
| XML | sim | sim, AAPT2 |
| JSON | sim | edição/análise |
| Markdown | sim | edição/análise |

Os packs de toolchain não mudam a classificação das linguagens: continuam sendo linguagens built-in.

## Status de toolchain

NATIVE_BUILD_READY significa que o caminho central está disponível diretamente no núcleo.

NATIVE_BUILD_REQUIRES_CORE_TOOLCHAIN significa que o backend faz parte do IDE, mas o binário pesado é distribuído pelo Core Toolchain Manager.

Hoje C/C++ usam esse estado para o LLVM. Kotlin também possui pack separado para manter o APK-base menor.

## C/C++

~~~text
.c / .h
.cc / .cpp / .cxx / .hh / .hpp / .hxx
    ↓
CompileNativeTask
    ↓
Android-hosted LLVM
~~~

Padrões atuais:

- C17;
- C++20;
- arm64-v8a;
- alinhamento de 16 KiB no linker.

## Kotlin

~~~text
.kt
  ↓
Core Kotlin Toolchain classloader
  ↓
K2JVMCompiler
  ↓
classes.jar
  ↓
D8
~~~

Não existe Kotlin daemon no build nativo.

## Regra para novas linguagens

Uma nova linguagem só entra no core quando houver:

1. editor/language services;
2. compiler/runtime on-device;
3. integração com Build Engine;
4. testes;
5. documentação;
6. validação build/run.

Não se cria LanguagePlugin.

## Quando algo ainda não estiver pronto

O build retorna diagnóstico explícito. Ele não muda silenciosamente para Gradle.
