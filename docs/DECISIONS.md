# AndroidIDE Pro — Architectural Decisions

## ADR-001 — Native build is the product route

**Status:** Accepted

O AndroidIDE Pro usa o Native Build Engine como rota de compilação do produto.

O Build Router não possui um backend Gradle de fallback para linguagens.

Se uma capacidade ainda não estiver pronta, o usuário recebe diagnóstico explícito.

## ADR-002 — Workspace existente permanece autoritativo

**Status:** Accepted

O Workspace/AndroidModule do AndroidIDE continua sendo a fonte de verdade durante a migração.

Adaptadores alimentam o Native Build Engine.

## ADR-003 — Compose migration is incremental

**Status:** Accepted

Novas superfícies Pro usam Compose + Material 3 enquanto as superfícies legadas continuam funcionando até serem substituídas com segurança.

## ADR-004 — UI restraint

**Status:** Accepted

A UI evita neon, cyberpunk, RGB, glow excessivo e animações decorativas.

A prioridade é leitura, densidade, toque e performance.

## ADR-005 — Build graph is task based

**Status:** Accepted

Cada estágio declara inputs, outputs e dependências.

Isso permite incrementalidade, diagnósticos, cache e futura execução paralela.

## ADR-006 — Documentation is part of implementation

**Status:** Accepted

Mudanças arquiteturais devem atualizar a documentação.

## ADR-007 — Asset repositories are extension points

**Status:** Accepted

Repositórios de ícones e assets são providers atrás de contratos estáveis.

## ADR-008 — Kotlin compiler extensions are build extensions

**Status:** Accepted

Compiler plugins Kotlin, como Compose, pertencem ao contrato de extensão do compilador.

Eles não transformam Kotlin em linguagem-plugin.

## ADR-009 — Languages are built-in

**Status:** Accepted

Java, Kotlin, C e C++ são capacidades do núcleo.

Toolchains pesados podem ser distribuídos como Core Toolchain Packs, mas isso não transforma a linguagem em plugin.

Não existe toggle para ligar/desligar uma linguagem.

## ADR-010 — No Gradle fallback

**Status:** Accepted

Nenhuma falha de uma implementação nativa deve redirecionar o projeto para Gradle.

Gradle pode continuar existindo no repositório para tarefas de engenharia, mas não é o backend do Build Router do AndroidIDE Pro.

## ADR-011 — Core Toolchain Packs

**Status:** Accepted

Compiladores pesados podem ficar fora do APK-base quando isso reduzir tamanho e RAM.

O AndroidIDE Pro administra esses componentes como produto oficial através do Core Toolchain Manager.

Os packs possuem versão, checksum e ciclo de atualização próprios.

## ADR-012 — Android-hosted LLVM

**Status:** Accepted

C/C++ usa LLVM/Clang/LLD construído para executar no Android arm64.

Um NDK desktop não é executado diretamente no aparelho.

O pack reduzido contém apenas o target e runtime necessários ao primeiro ABI suportado.
