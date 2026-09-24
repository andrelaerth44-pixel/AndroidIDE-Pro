# AndroidIDE Pro — Architectural Decisions

## ADR-001 — Native build first

**Status:** Accepted

The AndroidIDE Pro native-compatible path should not require a Gradle daemon.

**Reason:** on-device memory and startup cost are core product constraints.

Gradle remains a compatibility/fallback backend.

## ADR-002 — Reuse the existing Workspace

**Status:** Accepted

The existing AndroidIDE Workspace/AndroidModule model remains authoritative during migration.

**Reason:** creating a second project model would duplicate logic and increase synchronization bugs.

Adapters are preferred.

## ADR-003 — Compose migration is incremental

**Status:** Accepted

New Pro surfaces use Compose + Material 3, while stable legacy surfaces can remain until their replacement is justified.

**Reason:** a whole-app rewrite would increase risk and temporarily reduce functionality.

## ADR-004 — UI stays restrained

**Status:** Accepted

No neon/cyberpunk/RGB/glow-heavy visual language.

**Reason:** the product is a professional IDE. Readability, density and performance matter more than visual spectacle.

## ADR-005 — Build graph is task based

**Status:** Accepted

Build stages are independent tasks with declared inputs/outputs.

**Reason:** required for incremental execution, diagnostics and future parallelism.

## ADR-006 — Documentation is part of implementation

**Status:** Accepted

Important architectural changes must update the relevant documentation.

**Reason:** the project is large enough that memory cannot depend on conversation history.

## ADR-007 — Asset repositories are extension points

**Status:** Accepted

Bibliotecas de ícones são providers atrás de um contrato IconRepository, e não integrações específicas da UI.

**Reason:** novas bibliotecas devem poder ser adicionadas sem alterar a camada visual central.

## ADR-008 — Kotlin compiler plugins are build extensions

**Status:** Accepted

Compiler plugins Kotlin, como Compose, devem ser selecionados por capacidades do módulo e fornecidos por um contrato genérico de compiler plugins.

**Reason:** extensões do compilador não devem ficar codificadas diretamente na implementação da tarefa Kotlin.