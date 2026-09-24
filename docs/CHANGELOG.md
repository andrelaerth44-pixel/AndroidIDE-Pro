# Changelog de Engenharia

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
