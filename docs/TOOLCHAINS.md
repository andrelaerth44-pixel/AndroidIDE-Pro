# AndroidIDE Pro — Core Toolchains

## Princípio

Java, Kotlin, C e C++ são linguagens nativas do AndroidIDE Pro. Os compiladores e runtimes pesados podem ser distribuídos como **Core Toolchain Packs**, mas continuam sendo componentes oficiais do produto.

Core Toolchain Pack **não é plugin de linguagem**.

Não existe ativação/desativação de linguagem, instalação de linguagem como plugin ou fallback de compilação para Gradle.

## Estrutura

~~~text
AndroidIDE Pro
  ↓
Core Toolchain Manager
  ├── Kotlin Toolchain APK
  │     ↓
  │   package classloader
  │     ↓
  │   K2JVMCompiler
  │
  └── LLVM Toolchain APK
        ↓
      Android arm64 native libraries
        ↓
      clang / clang++ / lld
~~~

Os arquivos auxiliares extraídos ficam em filesDir/toolchains por versão.

## Kotlin

O módulo core/toolchain-kotlin produz o APK com o compilador Kotlin 1.9.24.

O AndroidIDE Pro resolve o pacote pela API de package context com inclusão de código e usa o classloader do pacote.

O Build Engine não depende mais de kotlin-compiler-embeddable no APK principal.

A tarefa Kotlin carrega K2JVMCompiler por reflexão e executa o compilador no processo do Build Engine. Não existe daemon Kotlin para builds de projeto.

## LLVM C/C++

O módulo core/toolchain-llvm produz o APK do compilador nativo Android arm64.

Alvo atual:

~~~text
host: Android arm64
target: aarch64-linux-android
ABI do app: arm64-v8a
C: C17
C++: C++20
~~~

O pack contém somente o necessário para compilar e linkar C/C++ no próprio Android:

- driver LLVM/Clang;
- LLD ELF;
- libLLVM;
- libclang-cpp;
- libc++_shared;
- clang resource directory;
- runtimes de compilação;
- sysroot AArch64 reduzido;
- headers;
- bibliotecas Android;
- android_native_app_glue quando disponível.

Não distribuímos um NDK desktop inteiro dentro do APK.

## Build do pack LLVM

~~~text
tools/llvm-toolchain/build-android-llvm.sh
        ↓
host tablegen
        ↓
LLVM/Clang/LLD cross-build para Android arm64
        ↓
redução para ELF/AArch64
        ↓
sysroot + runtime + driver
        ↓
core/toolchain-llvm APK
~~~

O builder reduz o produto com MinSizeRel, backend AArch64, LLVM/Clang compartilhados, LLD somente ELF e remoção de componentes de desenvolvimento.

## Integridade e atualização

Cada toolchain.zip gera SHA-256 e o valor fica em toolchain.properties.

O Core Toolchain Manager:

- valida a versão;
- extrai para diretório temporário;
- rejeita ZIP path traversal;
- troca o diretório de forma atômica;
- remove versões antigas.

O debug signing dos packs serve apenas para artefatos de desenvolvimento/CI. Release final usa assinatura de distribuição.

## CI

O workflow principal publica o Core Kotlin Toolchain separadamente.

O workflow .github/workflows/build-llvm-toolchain.yml gera o Core LLVM Android arm64 e publica:

- APK do pack;
- relatório de tamanho;
- SHA-256.

## Estado

- [x] Core Toolchain API
- [x] armazenamento por versão/ABI
- [x] Core Kotlin Toolchain APK
- [x] Kotlin package classloader
- [x] Core LLVM toolchain model
- [x] Android arm64 LLVM builder
- [x] Core LLVM Toolchain APK module
- [x] Toolchain Center
- [x] checksum metadata
- [ ] validação física em aparelho Android arm64
- [ ] atualização online autenticada
- [ ] download resumível
- [ ] múltiplos ABIs
