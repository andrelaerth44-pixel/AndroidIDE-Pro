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
- [x] compilação independente da API validada com Kotlin
- [x] smoke test do `BuildGraph` validado
- [ ] build Gradle completo do repositório validado
- [x] Primeiro código funcional do AndroidIDE Pro iniciado
- [x] Gradle Adapter de planejamento adicionado sem substituir o backend atual
- [x] correção do registro dos módulos no `settings.gradle.kts` aplicada e conferida
- [x] referência do pipeline de APK do CodeAssist documentada
- [x] 17 IDs de tarefas Android registrados
- [x] engine nativo possui DAG parcial até APK assinado
- [x] GradleBuildService tenta backend nativo antes do Tooling API para assemble
- [x] Workspace/AndroidModule alimenta variant, fontes, recursos, classpath e android.jar
- [x] toolchain nativo resolve AAPT2, D8/R8, zipalign, apksigner e debug keystore no Build API

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

### Arquitetura-alvo

O engine nativo seguirá o formato observado no CodeAssist:

```text
Project Model
    ↓
Task DAG + incremental state
    ↓
Resources / Manifest / AAPT2
    ↓
Java / Kotlin
    ↓
D8 / R8
    ↓
Package / Sign
    ↓
APK
```

Gradle permanece como compatibilidade e não como caminho normal.

### Peças já existentes no repositório

- composite `java-compiler`;
- composite `javac`;
- `:java:javac-services`;
- `:xml:aaptcompiler`;
- infraestrutura de Android/SDK já presente no projeto.

## Primeiro código funcional

Implementado:

- modelo leve de projeto/módulo;
- `BuildSystem` SPI;
- `BuildTask` e `TaskResult`;
- `BuildGraph` com validação de dependências/ciclos e ordenação topológica determinística;
- diagnósticos estruturados;
- cancelamento cooperativo;
- contrato e implementação inicial de `BuildExecutor` via `SequentialBuildExecutor`;
- testes unitários do grafo;
- `GradleBuildSystem` com planejamento de tasks;
- `GradleProjectAdapter` para converter `IProject` em `BuildProject`;
- `GradleTaskExecutor` injetável, com fallback seguro que não executa Gradle;
- política de recursos do fallback Gradle;
- engine local com `clean` executado sem Gradle;
- IDs das 17 etapas do pipeline Android observadas no CodeAssist.

Validação:

- [x] árvore e arquivos verificados via GitHub;
- [x] inclusão de módulos corrigida no `settings.gradle.kts`;
- [x] implementação do `BuildGraph` revisada estaticamente;
- [x] compilação independente e smoke test executados localmente;
- [ ] compilação/testes completos via Gradle/CI;
- [x] bridge `GradleTaskExecutor` -> `GradleBuildService` implementado;
- [x] `SequentialBuildExecutor` adicionado;
- [x] ponto de criação `createBuildSystemAdapter()` adicionado;
- [x] `clean` migrado para o engine leve;
- [x] dependências do build engine corrigidas no `core:app`;
- [ ] primeiro APK real completo validado no dispositivo/CI
- [ ] validar assembleDebug com projeto Hello World real, sem dependências externas
- [ ] integrar dependências AAR/JAR e módulos de projeto ao native build graph

## Próximo item

Validar o primeiro assembleDebug nativo contra um projeto Hello World real no dispositivo, sem dependências externas, e então substituir o adapter convencional por resolução de dependências real.
A compilação Java usa JDT/ecj diretamente; AAPT2, D8, zipalign e apksigner são ferramentas diretas; Gradle só entra quando a cobertura nativa recusa o projeto.