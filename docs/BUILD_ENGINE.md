# Build Engine

## Objetivo

Executar builds Android diretamente no dispositivo com controle fino de tarefas, cache, memória e toolchains.

## Princípios

1. Gradle não é a API interna do novo engine.
2. Tasks precisam declarar entradas e saídas.
3. Diagnósticos devem ser estruturados.
4. Tudo precisa ser cancelável.
5. Cache precisa ser invalidável e versionado.
6. Paralelismo precisa respeitar recursos do dispositivo.

## Contratos iniciais

### BuildSystem

Deverá representar:

- `id`;
- tipos de projeto suportados;
- `createBuildGraph`;
- tasks disponíveis;
- execução de tasks;
- ações especiais.

### Task

Deverá representar:

- `id`;
- entradas;
- saídas;
- parâmetros;
- dependências;
- executor.

### BuildDiagnostic

Deverá conter:

- severity;
- kind;
- source;
- location;
- code;
- detail;
- task.

## Task graph

Exemplo:

```
resolveDependencies
        |
mergeResources ----+
        |           |
aapt2Compile        |
        |           |
aapt2Link ----------+
        |
compileKotlin
        |
compileJava
        |
dex
        |
package
        |
sign
```

O grafo real será determinado pelo modelo de projeto.

## Incrementalidade

Fingerprint deve considerar:

- conteúdo;
- caminho lógico;
- metadados necessários;
- toolchain;
- configuração;
- dependências.

## Cache

O cache terá versão de formato e política de invalidação.

## Gradle adapter

Projetos Gradle poderão produzir um Project Model e, onde necessário, delegar para Gradle.

A compatibilidade será um adapter, não a fundação do engine.
