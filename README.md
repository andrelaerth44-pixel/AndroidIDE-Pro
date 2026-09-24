# AndroidIDE Pro

> Continuação e evolução do AndroidIDE original para uma IDE de desenvolvimento Android on-device moderna, modular, extensível e tecnicamente rastreável.

**Status atual:** fase de auditoria e fundação documental. Nenhum código funcional do AndroidIDE foi alterado nesta etapa.  
**Branch de desenvolvimento:** `work/androidide-pro-dev-foundation`  
**Base desta branch:** `dev`  
**Commit-base:** `77ee1a315f34b9ed74a9da94f94a0dc276f72ff6`  
**Data da auditoria:** 2026-09-24

---

## 1. Visão

O AndroidIDE Pro é uma evolução do AndroidIDE. Não é uma IDE nova criada do zero e não é uma cópia do CodeAssist.

A regra central do projeto é:

> Preservar o que já funciona no AndroidIDE, modernizar onde existe ganho técnico real e reutilizar ideias ou componentes do CodeAssist apenas depois de verificar arquitetura, compatibilidade, licença, desempenho e integração.

O produto final deverá permitir desenvolvimento Android diretamente no dispositivo, com editor profissional, análise semântica, build on-device, Java/Kotlin, XML/Compose, C/C++, NDK/CMake, Git, terminal, ferramentas de recursos e uma plataforma extensível.

---

## 2. Regra de rastreabilidade

O projeto deverá sempre responder:

- o que existe hoje;
- o que foi alterado;
- por que foi alterado;
- o que ainda falta;
- qual é o próximo trabalho;
- quais decisões arquiteturais já foram tomadas;
- quais riscos estão abertos.

Arquivos de controle:

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

Depois de cada mudança relevante, a documentação deverá ser atualizada no mesmo ciclo.

---

## 3. Base real do repositório

O repositório possui várias linhas históricas importantes.

### Branch padrão atual

`apk-v3-signing`

Ela aponta para um commit de agosto de 2023 e contém uma linha de build antiga do AndroidIDE.

### Branch de desenvolvimento escolhida para o AndroidIDE Pro

`dev`

É uma linha muito mais recente do AndroidIDE, com commit-base de outubro de 2024. Ela possui a arquitetura moderna de módulos `core`, `editor`, `tooling`, `utilities`, `xml`, `termux`, `java` e `testing`.

### Decisão

O trabalho novo será baseado em `dev`, não em `apk-v3-signing`.

As diferenças específicas de `apk-v3-signing` serão auditadas e portadas somente quando forem necessárias, especialmente as relacionadas à distribuição/APK e assinaturas.

Não será feito um merge indiscriminado dos históricos.

---

## 4. Estado auditado

A arquitetura do branch `dev` contém, entre outros:

### Annotation

- `annotation:annotations`
- `annotation:processors`
- `annotation:processors-ksp`

### Core

- `core:actions`
- `core:app`
- `core:common`
- `core:indexing-api`
- `core:indexing-core`
- `core:lsp-api`
- `core:lsp-models`
- `core:projects`
- `core:resources`

### Editor

- `editor:api`
- `editor:impl`
- `editor:lexers`
- `editor:treesitter`

### Eventos

- `event:eventbus`
- `event:eventbus-android`
- `event:eventbus-events`

### Java/LSP

- `java:javac-services`
- `java:lsp`

### Logging

- `logging:idestats`
- `logging:logger`
- `logging:logsender`
- `logging:logsender-sample`

### Termux

- `termux:application`
- `termux:emulator`
- `termux:shared`
- `termux:view`

### Tooling

- `tooling:api`
- `tooling:builder-model-impl`
- `tooling:events`
- `tooling:impl`
- `tooling:model`
- `tooling:plugin`
- `tooling:plugin-config`

### Utilities

- `utilities:build-info`
- `utilities:flashbar`
- `utilities:framework-stubs`
- `utilities:lookup`
- `utilities:preferences`
- `utilities:shared`
- `utilities:templates-api`
- `utilities:templates-impl`
- `utilities:treeview`
- `utilities:uidesigner`
- `utilities:xml-inflater`

### XML

- `xml:aaptcompiler`
- `xml:dom`
- `xml:lsp`
- `xml:resources-api`
- `xml:utils`

### Testes

- `testing:androidTest`
- `testing:benchmarks`
- `testing:commonTest`
- `testing:gradleToolingTest`
- `testing:lspTest`
- `testing:unitTest`

---

## 5. Stack atual auditada

Na linha `dev`, a infraestrutura do projeto usa atualmente:

- Gradle wrapper 8.8 para construir a própria IDE;
- AGP 8.5.0;
- Kotlin 1.9.24;
- Kotlin Coroutines 1.8.1;
- KSP 1.9.24-1.0.20;
- Tree-sitter 4.3.1;
- Sora Editor 0.23.4-ce8de8e-SNAPSHOT;
- Glide 4.16.0;
- Navigation 2.7.7;
- JGit 6.8.0;
- LSP4J JSON-RPC 0.22.0;
- Material Components 1.11.0;
- AAPT2 artifacts alinhados a AGP 8.x;
- JUnit 4 e JUnit 5;
- Robolectric;
- Espresso;
- MockK/Mockito;
- WorkManager.

Esses números representam a base auditada, não uma promessa de permanecerem como versões finais. A atualização para versões atuais será feita depois de validar compatibilidade entre Gradle, AGP, Kotlin, JDK, AndroidX e tooling.

---

## 6. Build da própria IDE x build do projeto do usuário

Essa separação será obrigatória.

### Build da IDE

Responsável por compilar o AndroidIDE Pro no ambiente de desenvolvimento.

Pode usar Gradle normalmente.

### Build do usuário

Responsável por gerar o aplicativo do usuário no próprio Android.

O objetivo arquitetural é evoluir de uma dependência central da Gradle Tooling API para um Build Engine on-device próprio, mantendo uma camada de compatibilidade/importação para projetos Gradle existentes.

---

## 7. Arquitetura-alvo

```
AndroidIDE Pro
├── UI Shell
├── Project Model
├── Editor
│   ├── Sora
│   ├── Tree-sitter
│   └── LSP
├── Indexing
├── Terminal
├── Git
├── Resource Tools
├── UI Designer
├── Compose Preview
├── Build Engine
│   ├── Project Graph
│   ├── Task Engine
│   ├── Incremental State
│   ├── Cache
│   ├── Dependency Resolution
│   ├── Java
│   ├── Kotlin
│   ├── Android Resources
│   ├── AAPT2
│   ├── D8/R8
│   ├── Packaging
│   └── Signing
├── Native Build
│   ├── Clang/LLVM
│   ├── NDK
│   ├── CMake
│   └── JNI
├── Extension System
└── Compatibility Layer
    └── Gradle Projects
```

Esse desenho é um objetivo arquitetural, não uma implementação já existente.

---

## 8. Build Engine

O Build Engine será dividido em:

### Project Model

Representa:

- workspace;
- projetos;
- módulos;
- source sets;
- variantes;
- build types;
- flavors;
- SDK;
- dependências;
- recursos;
- manifest;
- Kotlin/Java;
- Compose;
- C/C++;
- CMake/NDK.

### Task Engine

Cada task deverá possuir:

- identidade;
- entradas;
- saídas;
- propriedades;
- dependências;
- execução;
- diagnóstico;
- cancelamento.

### Incremental Engine

Usará:

- fingerprints;
- estado persistente;
- invalidação;
- cache;
- execução somente quando necessário.

### Diagnostics

Os erros deverão ser estruturados com:

- severidade;
- categoria;
- arquivo;
- linha;
- coluna;
- código;
- mensagem;
- task responsável.

---

## 9. Pipeline Android planejado

A implementação será incremental:

1. configuração do projeto;
2. dependências;
3. merge de manifest;
4. recursos;
5. AAPT2 compile;
6. AAPT2 link;
7. geração de R;
8. Java;
9. Kotlin;
10. KSP/annotation processing;
11. Compose;
12. D8;
13. R8;
14. packaging;
15. zipalign;
16. assinatura;
17. APK;
18. AAB;
19. verificação de artefatos.

---

## 10. Gradle compatibility

Projetos Gradle existentes continuarão sendo tratados como primeira classe.

A compatibilidade terá responsabilidades de:

- importação;
- descoberta de módulos;
- descoberta de variantes;
- leitura de dependências;
- sincronização;
- execução de tarefas incompatíveis com o Build Engine;
- explicação de recursos que não puderem ser convertidos.

A substituição do Gradle não será feita de maneira abrupta.

---

## 11. Editor e linguagem

A base atual será preservada e modernizada.

Prioridade:

1. Java;
2. Kotlin;
3. XML;
4. C;
5. C++;
6. JSON/YAML/TOML;
7. JavaScript/TypeScript;
8. Python;
9. Rust;
10. Go;
11. Shell;
12. SQL;
13. Markdown/HTML/CSS.

Uma linguagem só será considerada suportada quando tiver uma experiência efetiva de edição e diagnóstico adequada ao seu nível de integração.

---

## 12. LSP e Indexing

A infraestrutura de LSP e indexação existente é um ativo do projeto.

A evolução deverá suportar:

- diagnostics;
- completion;
- hover;
- definition;
- references;
- rename;
- formatting;
- code actions;
- symbols;
- workspace symbols.

Indexação deverá privilegiar processamento incremental e persistência em disco para reduzir pressão de memória.

---

## 13. C/C++/NDK

O AndroidIDE Pro terá suporte nativo para:

- NDK;
- Clang/LLVM;
- C;
- C++;
- CMake;
- JNI;
- arm64-v8a;
- armeabi-v7a;
- x86;
- x86_64.

Também deverá existir um NDK Manager para versões instaladas e seleção por projeto.

---

## 14. Interface

A interface será modernizada progressivamente.

Diretrizes:

- Material 3;
- Compose quando houver ganho;
- Views existentes mantidas onde a migração não compensar;
- tipografia clara;
- navegação previsível;
- painéis redimensionáveis;
- command palette;
- editor com abas;
- terminal integrado;
- build output;
- problems;
- Logcat;
- Project view.

Não serão usados como linguagem visual:

- neon;
- cyberpunk;
- RGB;
- brilho excessivo;
- gradientes decorativos;
- glassmorphism indiscriminado;
- elementos 3D decorativos.

---

## 15. Icon Center

Será criada uma plataforma integrada para:

- Material Symbols;
- bibliotecas de ícones licenciadas de forma compatível;
- SVG;
- VectorDrawable;
- PNG/WebP;
- App Icon Studio;
- inserção contextual no código.

O sistema deverá ser extensível e não amarrado a um único fornecedor.

---

## 16. Plugins

A arquitetura deverá permitir plugins para:

- linguagens;
- LSP;
- build systems;
- tasks;
- templates;
- icon repositories;
- ferramentas;
- temas;
- integrações externas.

Plugins deverão declarar:

- versão;
- permissões;
- dependências;
- APIs necessárias;
- compatibilidade.

---

## 17. IA

IA será opcional.

O core funcionará completamente sem IA.

A camada futura poderá aceitar provedores configuráveis via API, com:

- contexto do arquivo;
- seleção;
- projeto;
- chat;
- geração;
- edição;
- refatoração;
- explicação.

Segredos deverão utilizar armazenamento seguro do Android.

---

## 18. Performance

O projeto será tratado como software para hardware limitado.

Regras:

- jobs canceláveis;
- cache controlado;
- carga sob demanda;
- paralelismo adaptativo;
- descarregamento de serviços ociosos;
- evitar ASTs gigantes permanentes na RAM;
- evitar processos duplicados;
- operações pesadas em background;
- persistência quando apropriado.

---

## 19. Segurança

Áreas obrigatórias:

- API keys;
- credenciais Git;
- keystores;
- validação de caminhos;
- confirmação para ações destrutivas;
- verificação de downloads;
- integridade de caches;
- permissões mínimas possíveis;
- isolamento de tarefas quando viável.

---

## 20. Testes

Cada camada relevante deverá possuir:

- unit tests;
- integration tests;
- build fixtures;
- regression projects;
- UI tests;
- performance/benchmark tests quando necessário.

A aceitação de uma feature exigirá mais do que uma compilação local.

---

## 21. CodeAssist: reaproveitamento controlado

Serão estudados e eventualmente adaptados:

- Task Engine;
- Build System SPI;
- build graph;
- incremental execution;
- cache;
- diagnostics;
- Android pipeline;
- icon repository;
- Icon Manager;
- App Icon Studio;
- mecanismos de extensão.

Antes de portar código:

1. verificar licença;
2. identificar autoria;
3. conferir dependências;
4. entender o fluxo;
5. adaptar interfaces ao AndroidIDE;
6. preservar avisos de copyright quando exigidos;
7. escrever testes;
8. registrar a decisão.

---

## 22. Critério de pronto

Uma feature somente será considerada concluída quando:

- implementada;
- integrada;
- testada;
- documentada;
- compatível com as partes preservadas;
- registrada no changelog;
- marcada como concluída no status.

---

## 23. O que não será feito

- reescrever o AndroidIDE inteiro sem necessidade;
- copiar o CodeAssist inteiro;
- trocar estabilidade por efeitos visuais;
- transformar IA em requisito;
- declarar suporte de linguagem apenas por syntax highlighting;
- remover recursos sem estratégia de migração;
- misturar indiscriminadamente branches históricas;
- atualizar dependências sem validar a cadeia de compatibilidade.

---

## 24. Próximas etapas

### Fase 0 — Fundação

- [x] selecionar base `dev`;
- [x] criar branch de trabalho;
- [x] criar especificação;
- [x] criar rastreamento de status/tarefas;
- [ ] finalizar inventário de módulos;
- [ ] mapear dependências por módulo;
- [ ] mapear entry points;
- [ ] mapear serviços;
- [ ] mapear pipelines atuais;
- [ ] registrar riscos.

### Fase 1 — Build

- [ ] abstração Build System;
- [ ] Task Engine;
- [ ] diagnóstico estruturado;
- [ ] incrementalidade;
- [ ] cache;
- [ ] dependências;
- [ ] Android pipeline inicial;
- [ ] APK.

### Fase 2 — Native

- [ ] NDK;
- [ ] Clang/LLVM;
- [ ] CMake;
- [ ] JNI;
- [ ] variantes ABI.

### Fase 3 — UI/UX

- [ ] Compose foundation;
- [ ] Material 3;
- [ ] workspace;
- [ ] command palette;
- [ ] painéis;
- [ ] preferences.

### Fase 4 — Tooling

- [ ] Icon Center;
- [ ] Asset Center;
- [ ] Compose Preview;
- [ ] APK inspector;
- [ ] Logcat;
- [ ] device tools.

### Fase 5 — Extensibilidade

- [ ] plugin API;
- [ ] plugin lifecycle;
- [ ] marketplace/catalog;
- [ ] external tool providers.

### Fase 6 — IA opcional

- [ ] provider API;
- [ ] contexto;
- [ ] chat;
- [ ] code actions;
- [ ] edição assistida.

---

## 25. Estado neste commit

**Implementação funcional:** 0 alterações nesta etapa.

**Fundação documental:** em andamento.

**Próximo ponto de trabalho:** terminar a auditoria estrutural do branch `dev`, especialmente o fluxo de build atual, serviços do app, editor/LSP/indexação, sistema de projetos e infraestrutura de Termux.

---

## Licença

O AndroidIDE Pro mantém a GPLv3 da base AndroidIDE. Componentes externos terão sua licença e atribuição avaliadas individualmente antes de integração.
