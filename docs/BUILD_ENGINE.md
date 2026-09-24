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

A primeira implementação está em `:build:gradle-adapter`.

Ela já:

1. converte `IProject` para `BuildProject`;
2. preserva o diretório e os caminhos dos módulos;
3. planeja `assemble`, `assemble<Variant>` e tasks requisitadas;
4. adiciona `clean` como dependência quando solicitado;
5. mantém a execução atrás de `GradleTaskExecutor`.

Ainda faltam:

6. descobrir o catálogo completo de tasks;
7. conectar `GradleTaskExecutor` ao `GradleBuildService`;
8. converter logs/diagnósticos do Tooling API em `BuildDiagnostic`;
9. mapear de forma autoritativa Android application vs library.

O executor default do adapter é deliberadamente não-operacional para impedir execução acidental.

Para o caminho real, `core:app` fornece `GradleBuildServiceTaskExecutor`, que chama o `BuildService` existente, traduz resultados para `TaskResult` e encaminha cancelamento. `GradleBuildService.createBuildSystemAdapter()` cria essa combinação.

A migração do fluxo principal ainda não foi feita: o novo pipeline existe como caminho reversível de integração.

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



## Regra de peso do AndroidIDE Pro

O aplicativo não deve carregar Gradle como biblioteca de execução dentro do processo principal.

A cadeia Gradle existente continua em processo separado, e a política de compatibilidade reduz o impacto com:

- heap limitado;
- um worker;
- sem paralelismo;
- sem daemon persistente;
- sem file-system watching;
- sem build cache pelo backend de compatibilidade.

A meta arquitetural é que builds Android comuns sejam executados pelo engine próprio, mantendo Gradle para casos de compatibilidade.
