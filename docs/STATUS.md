# Status — AndroidIDE Pro

**Data:** 2026-09-24  
**Branch:** `work/androidide-pro-dev-foundation`  
**Base:** `dev` / `77ee1a315f34b9ed74a9da94f94a0dc276f72ff6`

## Estado geral

- [x] Repositório acessível
- [x] Permissão de escrita confirmada
- [x] Branch de trabalho baseada em `dev`
- [x] README mestre criado
- [x] Diferença entre `apk-v3-signing` e `dev` identificada
- [x] Arquitetura de módulos inicial inventariada
- [x] Stack Gradle/AGP/Kotlin inicial inventariada
- [x] App entry points identificados
- [x] Manifest e serviços principais identificados
- [x] GradleBuildService auditado em nível arquitetural
- [x] ProjectManager/Workspace/WorkspaceModelBuilder auditados
- [x] Tooling API server/client auditados
- [x] Java LSP e registro de language servers auditados parcialmente
- [x] Shell de UI principal auditado parcialmente
- [x] Auditoria estrutural inicial dos módulos concluída
- [x] Auditoria inicial de Indexing concluída
- [x] Auditoria inicial de Termux/toolchain concluída
- [x] Auditoria inicial de UI/Editor concluída
- [x] UI Designer/templates/resources/XML/CI mapeados em nível arquitetural
- [~] Matriz de riscos
- [ ] Build/test do novo módulo validado
- [x] Primeiro código funcional do AndroidIDE Pro iniciado

## Constatações principais

### Build atual

O app usa um `GradleBuildService` em foreground que inicia um processo Java separado para hospedar o Tooling API.

Esse servidor usa:

- `GradleConnector`;
- `ProjectConnection`;
- Gradle Wrapper/instalação/versão;
- cancelamento via `CancellationTokenSource`;
- execução de tasks por nome;
- callbacks de progresso/log;
- override do AAPT2 para um binário compatível com Android.

O sistema atual impede builds concorrentes no mesmo serviço.

### Modelo de projeto

O `ProjectManagerImpl` abre o diretório e, após inicialização, usa `WorkspaceModelBuilder` para transformar o modelo vindo do Tooling API em:

- GradleProject;
- AndroidModule;
- JavaModule;
- WorkspaceImpl.

Em seguida executa indexação de sources/classpaths e leitura de recursos.

Esse modelo é uma base forte para o Project Model do AndroidIDE Pro.

### LSP

Existe uma API de language server com registry e interfaces de cliente/servidor. O Java LSP integra análise, completion, definition, references, formatting e code actions ao editor por eventos.

### UI

O shell principal ainda usa Views/XML, FragmentContainerView, AppBarLayout e MaterialToolbar. A modernização visual deverá acontecer por superfícies sem descartar o shell funcional antes de criar equivalentes.

### Risco de plataforma

O manifest atual inclui permissões amplas de armazenamento, instalação/remoção de pacotes e foreground service, além de `largeHeap`. Essas escolhas deverão ser reavaliadas para as versões Android atuais durante a etapa de modernização.

## Primeiro código funcional

O módulo `:build:api` foi adicionado sem alterar o backend Gradle existente.

Implementado:

- modelo leve de projeto/módulo;
- `BuildSystem` SPI;
- `BuildTask` e `TaskResult`;
- `BuildGraph` com validação de dependências/ciclos e ordenação topológica determinística;
- diagnósticos estruturados;
- cancelamento cooperativo;
- contrato de `BuildExecutor`;
- testes unitários do grafo.

Ainda não implementado:

- executor real;
- Gradle adapter;
- cache/fingerprints;
- integração com `core:projects`/`GradleBuildService`.

## Próximo item

Validar o módulo em ambiente de build/teste disponível e então implementar o Gradle Adapter sem alterar o comportamento atual do backend Gradle.
