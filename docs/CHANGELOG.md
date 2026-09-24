# Changelog de Engenharia

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

### Ainda não feito

Nenhuma mudança funcional no produto.
