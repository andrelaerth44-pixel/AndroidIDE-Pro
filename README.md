# AndroidIDE Pro

> Continuação e evolução do AndroidIDE original para uma IDE de desenvolvimento Android on-device moderna, modular, extensível e tecnicamente rastreável.

**Status atual:** auditoria estrutural inicial concluída; primeira fundação funcional do Build System e o Gradle Adapter de planejamento adicionados.  
**Branch de desenvolvimento:** `work/androidide-pro-dev-foundation`  
**Base:** `dev` / `77ee1a315f34b9ed74a9da94f94a0dc276f72ff6`  
**Data:** 2026-09-24

---

## 1. Regra central

O AndroidIDE Pro não é uma IDE nova criada do zero e não é uma cópia integral do CodeAssist.

A regra é:

> preservar o AndroidIDE, modernizar onde houver ganho real e adaptar ideias/componentes externos somente depois de verificar arquitetura, compatibilidade, licença, desempenho e integração.

---

## 2. Controle do projeto

Documentação oficial de acompanhamento:

- [STATUS](docs/STATUS.md)
- [TASKS](docs/TASKS.md)
- [CHANGELOG](docs/CHANGELOG.md)
- [DECISIONS](docs/DECISIONS.md)
- [ARCHITECTURE](docs/ARCHITECTURE.md)
- [ROADMAP](docs/ROADMAP.md)
- [BUILD ENGINE](docs/BUILD_ENGINE.md)
- [NDK](docs/NDK_SUPPORT.md)
- [ICON CENTER](docs/ICON_CENTER.md)
- [UI GUIDELINES](docs/UI_GUIDELINES.md)

Toda alteração relevante precisa atualizar essa documentação.

---

## 3. Baseline

O repositório possui uma linha `apk-v3-signing` histórica, baseada em uma arquitetura anterior, e uma linha `dev` significativamente mais recente.

**Decisão atual:** desenvolvimento novo baseado em `dev`.

Diferenças de `apk-v3-signing` serão auditadas e portadas seletivamente. Não faremos merge indiscriminado.

---

## 4. Arquitetura real auditada

O branch `dev` possui:

### Core
`actions`, `app`, `common`, `indexing-api`, `indexing-core`, `lsp-api`, `lsp-models`, `projects`, `resources`.

### Editor
`api`, `impl`, `lexers`, `treesitter`.

### Tooling
`api`, `builder-model-impl`, `events`, `impl`, `model`, `plugin`, `plugin-config`.

### Java/XML
Java compiler/LSP e AAPT/XML tooling.

### Termux
Application, emulator, shared e view.

### Utilities
Templates, UI Designer, tree view, preferences e recursos compartilhados.

### Build
`build:api` — contratos independentes do futuro Build Engine.

`build:gradle-adapter` — ponte de planejamento entre o modelo Tooling API existente e o BuildSystem SPI.

### Testing
Unit, Android, LSP, Gradle Tooling e benchmarks.

---

## 5. Stack auditada

Na base `dev`:

- Gradle 8.8;
- AGP 8.5.0;
- Kotlin 1.9.24;
- Coroutines 1.8.1;
- KSP 1.9.24-1.0.20;
- Tree-sitter 4.3.1;
- Sora Editor 0.23.4-ce8de8e-SNAPSHOT;
- Glide 4.16.0;
- Navigation 2.7.7;
- JGit 6.8.0;
- LSP4J JSON-RPC 0.22.0;
- Material Components 1.11.0;
- AAPT2 compatível com a cadeia AGP;
- JUnit/Robolectric/Espresso/MockK/Mockito.

Essas versões são o baseline auditado. A atualização futura dependerá de compatibilidade entre toda a cadeia.

---

## 6. Fluxo de build atual

```
UI
 |
ProjectManager / BuildService
 |
GradleBuildService
 |
ToolingServerRunner
 |
processo Java
 |
IToolingApiServer
 |
ToolingApiServerImpl
 |
Gradle Tooling API
 |
Gradle Wrapper / Installation / Version
 |
Gradle Tasks
 |
AAPT2 / Java / Kotlin / D8 / R8 / Packaging
 |
APK
```

O `GradleBuildService` é um foreground service, controla um build por vez, encaminha logs/progresso/cancelamento e injeta um AAPT2 preparado para Android.

O `ToolingServerRunner` cria um processo Java separado e comunica com o app através da API de tooling.

O `ToolingApiServerImpl` usa `GradleConnector` e `ProjectConnection`, aceita Gradle Wrapper, instalação local ou versão de Gradle e usa cancellation tokens.

---

## 7. Project Model atual

O AndroidIDE já possui uma separação útil:

```
Gradle Tooling API
       |
IProject / IAndroidProject / IJavaProject
       |
WorkspaceModelBuilder
       |
WorkspaceImpl
  |       |       |
Gradle  Android  Java
Project  Module  Module
```

Depois da montagem do workspace, o Project Manager atualiza variantes e indexa sources/classpaths.

**Decisão:** preservar essa estrutura e usá-la como base/adaptador para o Project Model futuro.

Não haverá dois Project Managers concorrentes.

---

## 8. LSP atual

Existe uma API de linguagem com registry e contratos client/server.

O Java LSP já integra:

- completion;
- diagnostics;
- definition;
- references;
- signature help;
- smart selection;
- formatting;
- code actions;
- análise reativa a eventos do editor.

Essa infraestrutura será a base para expandir suporte a Kotlin, XML, C/C++ e outras linguagens.

---

## 9. Build Engine alvo

A fronteira será:

```
Application Services
        |
   BuildSystem SPI
     /          \
Gradle       Native/Local
Adapter       Engine
                  |
              Task Graph
                  |
       Toolchain / Cache
```

A primeira camada implementada em `build:api` fornece:

- `BuildProject` / `BuildModule`;
- `BuildRequest` / `BuildResult`;
- `BuildDiagnostic`;
- `BuildSystem`;
- `BuildTask` / `TaskResult`;
- `BuildGraph` com validação de dependências, ciclos e ordenação topológica;
- `BuildExecutor` como contrato;
- `CancellationToken`;
- `BuildDiagnosticSink`.

Ainda faltam:

- bridge de execução real para GradleBuildService;
- fingerprints;
- cache;
- dependency resolver;
- Android pipeline;
- native pipeline.

O Gradle atual será mantido somente como backend de compatibilidade até haver cobertura equivalente. Ele não é o engine principal do AndroidIDE Pro. O caminho normal deverá ser leve, incremental e próprio. Quando Gradle for inevitável, será executado isoladamente e sob política rígida de recursos; o GradleResourcePolicy atual limita heap, workers, paralelismo e residência do daemon.

---

## 10. Pipeline Android alvo

Progressivamente:

1. project model;
2. dependency resolution;
3. manifest;
4. resource merge;
5. AAPT2;
6. R generation;
7. Java;
8. Kotlin;
9. KSP/annotation processing;
10. Compose;
11. D8;
12. R8;
13. packaging;
14. zipalign;
15. signing;
16. APK/AAB;
17. verification.

---

## 11. Native

Objetivo:

- C;
- C++;
- NDK;
- Clang/LLVM;
- CMake;
- JNI;
- múltiplas ABIs.

O Native Build Engine será acoplado ao mesmo BuildSystem SPI.

---

## 12. UI alvo

Modernização progressiva para:

- Material 3;
- Jetpack Compose onde trouxer vantagem;
- workspace com abas;
- project tree;
- editor;
- bottom tool windows;
- Problems;
- Build Output;
- Terminal;
- Logcat;
- command palette.

Views/XML atuais permanecem durante a migração quando não houver benefício técnico suficiente para reescrevê-las.

### Visual

Não usar:

- neon;
- cyberpunk;
- RGB;
- glow excessivo;
- gradientes decorativos;
- glassmorphism indiscriminado.

Usar:

- hierarquia;
- tipografia;
- espaçamento;
- Material 3;
- feedback funcional;
- animações curtas;
- adaptação a telefone/tablet.

---

## 13. Icon Center

Sistema extensível para:

- Material Symbols;
- outros repositórios licenciados de forma compatível;
- SVG;
- VectorDrawable;
- PNG/WebP;
- App Icon Studio;
- inserção contextual em XML/Kotlin/Java/Compose.

---

## 14. NDK Manager

Planejado:

- detectar versões;
- instalar;
- remover;
- selecionar;
- validar;
- configurar CMake;
- registrar toolchains.

---

## 15. Plugins

Plugins poderão acrescentar:

- linguagens;
- LSP;
- build systems;
- tasks;
- templates;
- icon repositories;
- ferramentas;
- temas;
- integrações.

---

## 16. IA

IA será opcional e externa por API.

O núcleo funcionará sem IA e sem modelo local.

A futura camada poderá oferecer chat, contexto de projeto, code actions, geração e edição.

Segredos deverão usar armazenamento seguro.

---

## 17. Performance

Regras:

- jobs canceláveis;
- cache com limite;
- carga sob demanda;
- paralelismo adaptativo;
- indexação incremental;
- uso consciente de disco/RAM;
- serviços ociosos descarregáveis.

---

## 18. Segurança

O manifest atual possui permissões amplas de armazenamento, instalação/remoção de pacotes e foreground service, além de `largeHeap`. Essas escolhas serão reavaliadas contra as versões Android atuais.

O estado final deverá aplicar menor autoridade possível.

---

## 19. Testes

Cada grande subsistema deverá possuir testes unitários, integração e regressão.

Build fixtures serão usados para validar:

- Java;
- Kotlin;
- Compose;
- XML/resources;
- NDK;
- APK;
- AAB;
- dependências;
- Git.

---

## 20. Estado atual

### Concluído

- [x] branch de desenvolvimento baseada em `dev`;
- [x] README mestre;
- [x] documentação de status/tarefas/roadmap;
- [x] inventário inicial de módulos;
- [x] stack inicial;
- [x] build flow;
- [x] Project Manager/Workspace;
- [x] Tooling API;
- [x] LSP API/Java LSP em nível inicial;
- [x] arquitetura alvo;
- [x] contrato inicial do Build System;
- [x] `:build:api` e `:build:gradle-adapter` registrados na árvore do projeto;
- [x] Gradle Adapter inicial criado sem alterar o caminho de execução atual;
- [x] bridge de execução protegido por executor injetável;
- [x] política de recursos Gradle criada para compatibilidade móvel;
- [x] testes do `BuildGraph` para ordenação, dependência ausente e ciclo;
- [x] checagem estática do `BuildGraph` simplificada para reduzir risco de inferência de referências Kotlin.

### Em andamento

- [~] Indexing;
- [~] Editor/Sora/Tree-sitter;
- [~] Termux/toolchain;
- [~] UI;
- [~] recursos/XML;
- [~] matriz de riscos.

### Ainda não iniciado

- [ ] Build Engine funcional;
- [ ] NDK;
- [ ] Icon Center;
- [ ] Compose migration;
- [ ] plugin runtime;
- [ ] IA provider layer.

**Correção registrada:** o `settings.gradle.kts` do branch agora registra explicitamente `:build:api` e `:build:gradle-adapter`, e os arquivos novos do adapter carregam cabeçalho GPLv3.

**Próxima ação:** adicionar integração coberta por testes para o `createBuildSystemAdapter()` e somente então migrar chamadas de build selecionadas para o novo pipeline.

---

## Licença

A base AndroidIDE é GPLv3. Código ou assets externos somente entrarão após verificação de licença, atribuição e compatibilidade.
