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
- [ ] Auditoria de todos os módulos concluída
- [ ] Auditoria detalhada de Indexing concluída
- [ ] Auditoria detalhada de Termux/toolchain concluída
- [ ] Auditoria detalhada de UI/Editor concluída
- [ ] Matriz de riscos concluída
- [ ] Build local validado
- [ ] Primeiro código funcional do AndroidIDE Pro iniciado

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

## Próximo item

Completar o mapa de Indexing, Editor/Sora, Termux/Environment e Toolchain e então validar o build da própria IDE.
