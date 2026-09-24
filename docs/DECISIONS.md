# Decisões Arquiteturais

## ADR-0001 — Base de desenvolvimento

**Data:** 2026-09-24  
**Status:** aceito

O AndroidIDE Pro será desenvolvido a partir do branch `dev`.

O branch `apk-v3-signing` será tratado como fonte histórica de mudanças específicas de distribuição/assinatura que podem precisar de portabilidade.

---

## ADR-0002 — AndroidIDE como base

**Status:** aceito

A base AndroidIDE será preservada em vez de começar uma nova IDE.

---

## ADR-0003 — CodeAssist como referência seletiva

**Status:** aceito

CodeAssist será usado como referência para:

- Task Engine;
- build graph;
- incrementalidade;
- diagnostics;
- cache;
- Android pipeline;
- Icon Manager;
- extensões.

Nenhuma cópia integral será feita.

---

## ADR-0004 — Build do projeto separado do build da IDE

**Status:** aceito

Gradle continua permitido para compilar a própria IDE.

Para os projetos do usuário, o build deverá ser escondido atrás de uma abstração comum, permitindo:

- Gradle adapter;
- Build Engine próprio;
- native build.

---

## ADR-0005 — Preservar ProjectManager/Workspace

**Data:** 2026-09-24  
**Status:** aceito

`ProjectManagerImpl`, `WorkspaceModelBuilder` e `WorkspaceImpl` já representam projetos Gradle/Android/Java dentro do IDE.

Eles serão preservados e evoluídos em vez de criar um segundo Project Manager.

---

## ADR-0006 — Gradle como adapter

**Data:** 2026-09-24  
**Status:** aceito

O Tooling API atual não será removido antes que exista cobertura suficiente no novo engine.

A fronteira será:

`Application -> BuildSystem -> Gradle Adapter / Native Engine`

---

## ADR-0007 — UI progressiva

**Status:** aceito

Compose/Material 3 será introduzido progressivamente.

Views/XML existentes permanecem durante a migração quando forem a opção mais segura ou eficiente.

---

## ADR-0008 — Build Engine fora da UI

**Data:** 2026-09-24  
**Status:** aceito

Tasks de build não poderão depender diretamente de Activity, Fragment ou View.

O build será acionado por serviços e interfaces estáveis, permitindo trocar o backend sem reconstruir a UI.
