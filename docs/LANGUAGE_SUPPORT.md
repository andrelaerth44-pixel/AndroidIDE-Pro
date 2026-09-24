
# AndroidIDE Pro — Native Language Core

## Regra principal

Java, Kotlin, C e C++ fazem parte do núcleo do AndroidIDE Pro.

Não existe linguagem de programação como plugin, nem ativação/desativação de linguagem. Uma linguagem só é declarada suportada quando seu editor/language services e seu caminho de build/run fazem parte do próprio IDE.

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
  ├── Java compiler
  ├── Kotlin compiler
  └── Android-hosted LLVM
      ├── C
      └── C++
~~~

core/language-support é uma camada interna do produto; não é um mecanismo de plugins.

## Estados de suporte

- `NATIVE_BUILD_READY`: editor + análise + compilação nativa disponíveis no núcleo.
- `NATIVE_BUILD_REQUIRES_CORE_TOOLCHAIN`: backend nativo integrado, mas o pack executável ainda precisa ser instalado/fornecido pelo Core Toolchain Manager.
- `EDITOR_AND_ANALYSIS_ONLY`: linguagem/documento incorporado para edição e análise, sem compilação Android.

## Linguagens incorporadas

| Linguagem | Editor | Build nativo |
|---|---:|---:|
| Java | sim | sim |
| Kotlin | sim | sim, compiler em processo |
| C | em evolução | backend nativo + pack LLVM pendente |
| C++ | em evolução | backend nativo + pack LLVM pendente |
| XML | sim | sim, AAPT2 |
| JSON | sim | infraestrutura |
| Markdown | sim | infraestrutura |

A validação física de todos os caminhos em aparelho ainda precisa ser concluída.

## Kotlin

~~~text
.kt
 ↓
K2JVMCompiler embutido
 ↓
classes.jar
 ↓
D8
 ↓
classes.dex
~~~

Não existe daemon Kotlin no caminho do Native Build Engine.

## C / C++

~~~text
<AndroidIDE Pro>/toolchains/llvm/
└── arm64-v8a/
    ├── bin/
    │   ├── clang
    │   └── clang++
    └── sysroot/
~~~

Essa toolchain será distribuída e gerenciada pelo próprio AndroidIDE Pro. Não será instalada como plugin.

## Regra para novas linguagens

Para adicionar uma linguagem:

1. implementar editor/language services;
2. implementar compiler/runtime on-device;
3. registrar em BuiltInLanguageRegistry;
4. integrar ao build graph;
5. documentar e testar.

Não se cria LanguagePlugin.

## Regra de build

Quando uma linguagem ainda não possui implementação nativa completa, o build retorna um diagnóstico explícito. Ele não muda silenciosamente para Gradle.
