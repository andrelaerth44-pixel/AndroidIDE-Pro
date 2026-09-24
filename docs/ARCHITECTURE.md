# Arquitetura — AndroidIDE Pro

## 1. Arquitetura atual auditada

O branch `dev` separa o projeto em domínios:

- `core`: app, projetos, recursos, indexação e contratos LSP;
- `editor`: API, implementação, lexers e Tree-sitter;
- `tooling`: API, modelos, eventos, implementação e configuração;
- `java`: javac services e Java LSP;
- `xml`: AAPT compiler, DOM, LSP e recursos;
- `termux`: aplicação, emulador, shared e view;
- `utilities`: templates, UI Designer, tree view, preferences e utilitários;
- `event`: EventBus;
- `logging`: logging e métricas;
- `testing`: testes unitários, Android, LSP, Gradle Tooling e benchmarks.

## 2. Fluxo atual de build

```
Activity / UI
    |
BuildService / ProjectManager
    |
GradleBuildService
    |
ToolingServerRunner
    |
processo Java separado
    |
IToolingApiServer
    |
ToolingApiServerImpl
    |
Gradle Tooling API
    |
Gradle Wrapper / Installation / Version
    |
Gradle tasks
    |
AAPT2 / javac / Kotlin / D8 / R8 / packaging
```

O `GradleBuildService` também injeta o AAPT2 compatível com Android por propriedade do Gradle.

## 3. Fluxo atual de projeto

```
openProject(directory)
      |
ProjectManagerImpl
      |
Tooling API root model
      |
WorkspaceModelBuilder
      |
WorkspaceImpl
  |       |        |
Gradle  Android   Java
Project  Module   Module
      |
index sources/classpaths
      |
read Android resources
```

O `WorkspaceModelBuilder` transforma modelos de projeto Gradle/Android/Java já disponíveis na Tooling API para o modelo interno.

## 4. Decisão de arquitetura

Esse Project Model existente será preservado.

O futuro Build Engine deverá fornecer dados compatíveis com o modelo interno, ou uma nova versão do modelo que possa ser adaptada sem quebrar consumidores do core.

Não será criado um segundo Project Manager paralelo.

## 5. Arquitetura-alvo

```
UI
 |
Application Services
 |
Project Model
 |
Build System SPI
 +-------------------------+
 |                         |
Gradle Adapter          Native/Native-like Engine
 |                         |
Tooling API            Task Graph
                         |
                Toolchain + Cache
```

## 6. LSP

A API LSP existente já possui:

- language server registry;
- client/server contracts;
- settings;
- completion provider;
- Java implementation;
- eventos de documento.

Isso permite adicionar Kotlin/XML/C/C++ sem criar um segundo sistema de transporte.

## 7. UI

O shell atual em Views/XML será tratado como infraestrutura existente.

A migração para Compose/Material 3 será incremental:

1. tokens/tema;
2. componentes;
3. navegação;
4. telas;
5. tool windows;
6. superfícies especializadas.

## 8. Segurança arquitetural

O manifest atual usa permissões amplas e `largeHeap`. A modernização deverá separar:

- permissões necessárias ao IDE;
- permissões necessárias ao projeto do usuário;
- permissões opcionais;
- operações elevadas.

A regra futura será a menor autoridade possível compatível com a funcionalidade.

## 9. Limites

Nenhum Build Task novo deverá depender diretamente de Activity, Fragment ou View.

Nenhuma tela nova deverá executar compilação pesada diretamente na UI thread.

Nenhum plugin deverá precisar acessar classes internas quando uma API de extensão estiver disponível.
