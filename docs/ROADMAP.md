# Roadmap

## R0 — Fundação e auditoria

Objetivo: entender completamente o sistema antes de refatorar.

Saída:

- documentação;
- mapa de módulos;
- mapa de serviços;
- mapa de dependências;
- riscos;
- baseline reproduzível.

## R1 — Build Core

Criar abstrações independentes do Gradle:

- Project Model;
- Build System SPI;
- Task Engine;
- diagnostics;
- cancellation;
- cache.

## R2 — Primeiro pipeline Android

Implementar um caminho mínimo:

`project -> resources -> compile -> dex -> package -> sign -> APK`

A primeira meta não será suportar todas as features de Gradle, mas produzir um APK confiável em projetos controlados.

## R3 — Gradle compatibility

Converter/importar progressivamente projetos Gradle existentes.

## R4 — Native/NDK

Adicionar C/C++, CMake, NDK e JNI.

## R5 — UI/UX

Modernizar workspace e tool windows.

## R6 — Resource/Asset tooling

Icon Center, Asset Center, Compose Preview e APK inspector.

## R7 — Plugins

Introduzir extensões independentes.

## R8 — IA opcional

Adicionar providers via API sem contaminar o core.

## Regra

Nenhum marco será considerado concluído sem testes e documentação.
