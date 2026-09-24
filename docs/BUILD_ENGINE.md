# Build Engine

## Objetivo

Executar builds Android diretamente no dispositivo com controle fino de tarefas, cache, memória e toolchains.

## Situação atual

O build existente é Gradle-first:

- `GradleBuildService` vive como foreground service;
- `ToolingServerRunner` inicia um processo Java;
- `ToolingApiServerImpl` abre `ProjectConnection`;
- Gradle tasks são executadas por nome;
- cancelamento usa `CancellationTokenSource`;
- o AAPT2 usado no build é sobrescrito para um binário compatível com Android;
- o sistema não permite dois builds concorrentes no mesmo serviço.

Este sistema será preservado como caminho de compatibilidade até o novo engine possuir cobertura suficiente.

## Fronteira de substituição

A fronteira recomendada é:

`BuildService -> BuildSystem SPI -> {Gradle Adapter | Native Engine}`

O app não deve precisar saber se uma tarefa foi executada por Gradle ou pelo engine próprio.

## API inicial implementada

O módulo `:build:api` agora contém a primeira fronteira independente do Gradle.

### Modelos

- `BuildProject`;
- `BuildModule`;
- `BuildRequest`;
- `BuildContext`;
- `TaskContext`.

### Build System

`BuildSystem` define:

- `id`;
- `supports(moduleType)`;
- `createBuildGraph(...)`;
- `tasks(project)`.

### Tasks

`BuildTask` declara:

- `id`;
- dependências;
- inputs;
- outputs;
- execução.

### Graph

`BuildGraph` valida:

- IDs vazios;
- IDs duplicados;
- dependências inexistentes;
- ciclos;

e fornece ordenação topológica determinística.

### Diagnostics/Cancellation

A API possui:

- `BuildDiagnostic`;
- `BuildDiagnosticSink`;
- `CancellationToken`.

### Executor

`BuildExecutor` e `BuildResult` definem o contrato do executor, mas ainda não existe uma implementação concreta.

## Contratos iniciais

### BuildSystem

- `id`;
- tipos de projeto suportados;
- criação de build graph;
- tarefas disponíveis;
- execução;
- ações.

### Task

- identidade;
- entradas;
- saídas;
- parâmetros;
- dependências;
- executor;
- recursos estimados.

### BuildDiagnostic

- severity;
- kind;
- source;
- location;
- code;
- detail;
- task.

## Grafo

```
resolveDependencies
        |
projectModel
    +---+-----------------+
    |                     |
resources             compileJava/Kotlin
    |                     |
AAPT2                generatedSources
    +---------+-----------+
              |
             dex
              |
           package
              |
            sign
```

O grafo será variante-aware.

## Incrementalidade

Fingerprint deve incluir somente o estado necessário para decidir se a task é válida:

- conteúdos;
- paths lógicos;
- configuração;
- dependências;
- toolchain;
- versão do task implementation.

## Cache

O cache deverá possuir:

- versionamento;
- content addressing onde útil;
- limites;
- limpeza;
- detecção de corrupção;
- invalidação por toolchain/configuração.

## Gradle Adapter

O adapter deverá:

1. abrir um projeto Gradle;
2. obter metadata/model;
3. produzir Project Model;
4. mapear tasks;
5. executar Gradle quando não houver equivalente interno;
6. converter diagnósticos e resultados.

## Migração

A primeira implementação não deverá tentar substituir todo o Gradle.

A ordem deve ser:

1. interface;
2. adapter;
3. projeto controlado;
4. build Android mínimo;
5. diagnóstico;
6. incremental;
7. dependências;
8. variantes;
9. Gradle compatibility completa.

