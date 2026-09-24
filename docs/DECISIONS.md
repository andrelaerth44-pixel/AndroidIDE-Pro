# Decisões Arquiteturais

## ADR-0001 — Base de desenvolvimento

**Data:** 2026-09-24  
**Status:** aceito

### Decisão

O AndroidIDE Pro será desenvolvido a partir do branch `dev`.

### Motivo

O branch `dev` é significativamente mais recente que `apk-v3-signing` e contém uma decomposição de módulos mais avançada.

### Consequência

Diferenças específicas de `apk-v3-signing` serão avaliadas e portadas seletivamente.

---

## ADR-0002 — AndroidIDE como base

**Status:** aceito

O projeto preserva a base AndroidIDE em vez de começar uma nova IDE.

---

## ADR-0003 — CodeAssist como referência seletiva

**Status:** aceito

CodeAssist será usado como fonte para padrões de arquitetura e componentes candidatos, principalmente build incremental, diagnósticos e Icon Manager.

Nenhuma cópia integral será feita.

---

## ADR-0004 — Build do projeto separado do build da IDE

**Status:** aceito

Gradle pode continuar sendo usado para compilar o próprio AndroidIDE Pro, enquanto o build de projetos do usuário evolui para um Build Engine próprio com adapter de compatibilidade Gradle.

---

## ADR-0005 — UI progressiva

**Status:** aceito

Jetpack Compose/Material 3 será introduzido progressivamente. Views/XML existentes só serão substituídos quando houver benefício claro.
