# AndroidIDE Pro — Core Toolchains

## Regra

Java, Kotlin, C e C++ são linguagens built-in.

Core Toolchain Packs são componentes oficiais do produto. Não são plugins de linguagem e não criam toggles.

## Kotlin

core/toolchain-kotlin fornece Kotlin compiler 1.9.24 e Compose compiler hosted 1.5.14.

O AndroidIDE Pro carrega o compilador pelo classloader do pacote e não coloca o compilador no APK-base.

## LLVM

core/toolchain-llvm fornece um LLVM executável no próprio Android:

~~~text
assets/toolchain/bin/
├── clang
├── clang++
├── clangd
└── ld.lld

assets/toolchain/lib/
├── libLLVM.so
├── libclang-cpp.so
└── libc++_shared.so

assets/toolchain/
├── sysroot/
├── lib-clang/
└── native_app_glue/
~~~

Depois da instalação, o Core LLVM fica em filesDir/toolchains/llvm/<version>/ e é executado dali.

## Integridade

O pack possui versão, LLVM major, host ABI e SHA-256.

O Core Toolchain Manager valida o checksum, rejeita path traversal, extrai em diretório temporário, aplica permissões executáveis e troca a versão ativa de forma transacional.

## C/C++ e JNI

O mesmo Core LLVM é usado para C17, C++20, NativeActivity, JNI, bibliotecas nativas, engines, apps puros nativos e apps híbridos.

## clangd

clangd já faz parte do Core LLVM Pack.

A integração com o editor ainda é uma etapa separada.

## CI

.github/workflows/build-llvm-toolchain.yml produz o APK do pack, relatório de tamanho, SHA-256 e uma validação estrutural do toolchain.zip.

