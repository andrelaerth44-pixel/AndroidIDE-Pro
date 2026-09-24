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

    
---

## ADR-0009 — Build System API independente

**Data:** 2026-09-24  
**Status:** aceito

A primeira implementação funcional do Build Engine será um módulo `:build:api` independente do Gradle e da UI.

### Motivo

A IDE já possui um backend Gradle funcional. Substituí-lo diretamente aumentaria o risco.

A API permite estabelecer a fronteira arquitetural primeiro e migrar os backends em etapas.

### Consequência

O próximo backend será um Gradle Adapter compatível com o comportamento atual. O executor próprio e o pipeline Android serão implementados depois.


## Decisão — Gradle não é o engine principal

**Data:** 2026-09-24

O AndroidIDE Pro não deve depender de um Gradle pesado no caminho normal de build do aplicativo.

### Consequências

- o engine próprio deve ser incremental e orientado a tarefas;
- o processo Android principal não deve hospedar Gradle;
- Gradle fica atrás de um adapter de compatibilidade;
- builds Gradle são executados isoladamente;
- o adapter aplica orçamento de RAM/CPU;
- --no-daemon impede residência normal do daemon;
- --max-workers=1 evita explosão de workers;
- heap e metaspace são explicitamente limitados;
- a migração de builds reais deve aumentar cobertura do engine próprio antes de retirar o fallback.

### Motivo técnico

Gradle pode iniciar processos JVM adicionais e usar um daemon persistente. Para um IDE on-device, isso cria pressão de RAM e pode aumentar trabalho de CPU. A política atual, portanto, privilegia isolamento e limites rígidos enquanto o engine próprio amadurece.
