# Changelog de Engenharia

## 2026-09-24 — Sequential executor and build execution bridge

### Adicionado

- `GradleBuildServiceTaskExecutor`, que converte o `BuildService` atual em `GradleTaskExecutor`;
- cancelamento cooperativo e conversão de falhas para `BuildDiagnostic`;
- `SequentialBuildExecutor` como primeiro executor concreto do grafo;
- `GradleBuildService.createBuildSystemAdapter()` como ponto de integração reversível.

### Preservado

O fluxo de build existente continua sendo o caminho padrão.

## 2026-09-24 — Gradle Adapter registration correction

### Corrigido

- `settings.gradle.kts` passou a registrar efetivamente `:build:api` e `:build:gradle-adapter`;
- arquivos novos do Gradle Adapter receberam cabeçalhos GPLv3 consistentes com a base do projeto.

## 2026-09-24 — Gradle Adapter foundation

### Adicionado

- módulo `:build:gradle-adapter`;
- `GradleBuildSystem` para planejamento de tasks Gradle;
- `GradleProjectAdapter` para converter o `IProject` existente em `BuildProject`;
- `GradleTaskExecutor` como ponte injetável;
- fallback seguro que impede execução acidental antes da integração com `GradleBuildService`;
- testes de planejamento de variante, limpeza e suporte a tipos de módulo.

### Preservado

O `GradleBuildService`, `ToolingServerRunner` e `ToolingApiServerImpl` continuam sendo o backend de execução atual.

### Limitações conhecidas

- descoberta completa do catálogo de tasks ainda não está conectada;
- classificação Android application/library continua propositalmente UNKNOWN até existir uma fonte autoritativa no modelo AGP;
- o factory novo ainda não foi conectado ao fluxo principal de build;
- build Gradle/CI dos módulos novos ainda não foi validado.

## 2026-09-24 — Build System API foundation

### Adicionado

- novo módulo `:build:api`;
- `BuildProject` e `BuildModule`;
- `BuildRequest`, `BuildContext` e `TaskContext`;
- `BuildSystem` SPI;
- `BuildTask`, `TaskResult` e `TaskDescriptor`;
- `BuildGraph` com validação de dependências, detecção de ciclos e ordenação topológica;
- `BuildDiagnostic` e `BuildDiagnosticSink`;
- `CancellationToken`;
- contrato `BuildExecutor` e `BuildResult`;
- testes unitários do grafo de tarefas.

### Preservado

O `GradleBuildService`, `ToolingServerRunner` e backend Gradle continuam sem alterações funcionais.

### Correções da etapa

- `settings.gradle.kts` passou a incluir `:build:api` e `:build:gradle-adapter`;
- verificação de dependências do `BuildGraph` foi tornada explícita para reduzir ambiguidade de inferência Kotlin;
- compilação/testes do fork continuam pendentes porque o ambiente de execução desta sessão não possui acesso de rede ao repositório para montar o checkout local.

### Validação

- compilação independente dos arquivos de produção da API concluída com `kotlinc`;
- smoke test confirmou ordenação topológica, rejeição de ciclo e rejeição de dependência inexistente;
- build Gradle completo do repositório ainda não foi executado.

### Não concluído

- executor real;
- Gradle adapter;
- fingerprints;
- cache persistente;
- dependency resolver;
- Android/native pipelines;
- validação de build/test do fork.

## 2026-09-24 — Auditoria estrutural inicial

### Confirmado

- base de desenvolvimento: `dev`;
- build da própria IDE: Gradle 8.8 / AGP 8.5.0 / Kotlin 1.9.24;
- app principal: `core:app`;
- build do usuário: Gradle Tooling API;
- Project Manager e Workspace model existentes;
- LSP API e Java LSP existentes;
- UI principal ainda baseada em Views/XML;
- AAPT2 com override para binário compatível com Android;
- tooling executado em processo Java separado;
- cancelamento de build já existente.

### Decisões

- preservar Project Manager/Workspace;
- criar Build System SPI em torno do fluxo atual;
- manter Gradle como adapter de compatibilidade;
- não implementar o novo engine diretamente dentro do Service/UI.
