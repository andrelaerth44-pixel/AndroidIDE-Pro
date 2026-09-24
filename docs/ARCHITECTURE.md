# Arquitetura — AndroidIDE Pro

## 1. Arquitetura atual auditada

O branch `dev` separa o projeto em domínios bem definidos:

`core` concentra aplicação, projetos, recursos, LSP API/modelos e indexação.

`editor` concentra API, implementação, lexers e Tree-sitter.

`tooling` concentra abstrações e implementação do tooling relacionado ao projeto/build.

`java` concentra serviços de javac e Java LSP.

`xml` concentra AAPT compiler, DOM, LSP, recursos e utilitários XML.

`termux` concentra a infraestrutura de terminal/emulação/integração.

`utilities` concentra recursos transversais como templates, UI Designer, tree view e preferences.

`event` concentra event bus.

`logging` concentra logs e estatísticas.

## 2. Regra de evolução

A arquitetura nova deve introduzir novas abstrações sem quebrar fronteiras estáveis desnecessariamente.

Não serão criadas duplicações como:

- dois Project Managers;
- dois modelos de editor;
- dois sistemas de eventos;
- dois sistemas de diagnóstico independentes;
- dois mecanismos de cache sem integração.

Sempre que possível, uma nova capacidade deverá adaptar uma abstração existente.

## 3. Arquitetura-alvo

```
App/UI
  |
Project Model
  |
Services / Commands
  |
+-----------------------------+
| Build System Abstraction    |
+-----------------------------+
  |               |
Gradle Adapter   Native Build Engine
  |               |
Tooling API       Task Graph
                  |
          Toolchain / Cache
```

O Build Engine próprio não deverá ficar acoplado à Activity ou aos componentes visuais.

## 4. Domínios

### UI

Responsável apenas pela apresentação e interação.

### Application Services

Responsáveis por casos de uso:

- abrir projeto;
- build;
- run;
- instalar;
- Git;
- terminal;
- indexar.

### Project Model

Fonte de verdade para a estrutura do workspace.

### Language Services

Editor + LSP + indexação.

### Build

Task graph, toolchains, diagnósticos, cache e artefatos.

### Toolchain

Executáveis e bibliotecas como AAPT2, D8/R8, javac, Kotlin compiler, Clang e CMake.

### Persistence

Estado do projeto, índices, caches e preferências.

## 5. Limite importante

O app Android e o Build Engine não devem depender diretamente uns dos outros de forma cíclica.

Interfaces devem ser preferidas aos acessos globais.

## 6. Estado

Este documento descreve arquitetura-alvo em evolução. Cada mudança estrutural deverá ser registrada em DECISIONS.md.
