# Tarefas — AndroidIDE Pro

Legenda: `[ ]` não iniciado · `[~]` em andamento · `[x]` concluído · `[-]` descartado

## Fase 0 — Auditoria

- [x] Criar branch de trabalho baseada em `dev`
- [x] Criar README mestre
- [x] Criar sistema de documentação
- [x] Identificar modules em `settings.gradle.kts`
- [x] Registrar stack inicial
- [x] Identificar entry points
- [x] Identificar manifest
- [x] Identificar GradleBuildService
- [x] Identificar ToolingServerRunner
- [x] Identificar ToolingApiServerImpl
- [x] Identificar ProjectManagerImpl
- [x] Identificar WorkspaceModelBuilder
- [x] Identificar WorkspaceImpl
- [x] Identificar LSP registry/API
- [x] Identificar Java LSP
- [~] Mapear Indexing
- [~] Mapear Editor/Sora/Tree-sitter
- [~] Mapear Termux/Environment
- [~] Mapear UI
- [x] Mapear UI Designer
- [x] Mapear recursos/XML pipeline
- [x] Mapear build fixtures/testes/CI
- [~] Matriz de riscos
- [x] Criar `:build:api`
- [x] Criar `:build:gradle-adapter`
- [x] Registrar referência do pipeline real do CodeAssist
- [x] Registrar IDs das 17 etapas observadas
- [ ] Validar build/test completo dos novos módulos

## Fase 1 — Build Core

- [x] BuildRequest
- [x] BuildResult
- [x] BuildDiagnostic
- [x] BuildSystem SPI
- [x] Task
- [x] TaskContext
- [x] TaskGraph
- [x] executor
- [x] cancellation
- [ ] fingerprints
- [ ] persistent cache
- [ ] dependency model
- [x] Gradle adapter de planejamento
- [x] bridge Gradle Adapter -> GradleBuildService
- [x] ponto de criação do BuildSystem no GradleBuildService
- [x] migrar `clean` para o engine leve sem Gradle
- [x] selecionar backend nativo antes do fallback Gradle para `assemble*` compatível
- [x] migrar `compileJava` para o primeiro pipeline nativo
- [x] conectar `assembleDebug` ao engine nativo para app Android compatível
- [~] implementar engine Android leve por etapas
- [x] tornar Gradle explicitamente fallback de compatibilidade em arquitetura/documentação
- [ ] descoberta completa de tasks do projeto

## Fase 2 — Android pipeline

- [ ] generateSources
- [x] implementar e integrar `mergeResources` no primeiro DAG nativo
- [ ] checkAarMetadata
- [ ] mergeNativeLibs
- [ ] mergeJavaResource
- [x] implementar e integrar `aapt2Compile` no primeiro DAG nativo
- [ ] processManifest
- [ ] injectAppLogProvider
- [x] implementar e integrar `aapt2Link` no primeiro DAG nativo
- [ ] integrar geração de R ao grafo nativo; AAPT2 link já produz `R.java`
- [x] implementar e integrar `compileJava` no primeiro DAG nativo
- [ ] Kotlin
- [ ] KSP
- [ ] Compose
- [x] integrar `dexBuilder` nativo no primeiro DAG
- [x] integrar `mergeProjectDex` nativo no primeiro DAG
- [ ] mergeExtDex
- [x] integrar `packageApk` nativo no primeiro DAG
- [x] integrar assinatura debug nativa no primeiro DAG
- [ ] APK verification no dispositivo/build real
- [ ] AAB

## Fase 3 — Native

- [ ] NDK manager
- [ ] toolchain model
- [ ] Clang
- [ ] CMake
- [ ] JNI
- [ ] ABI variants
- [ ] .so packaging

## Fase 4 — UI

- [ ] Compose foundation
- [ ] Material 3 theme
- [ ] workspace shell
- [ ] tabs
- [ ] command palette
- [ ] bottom tool window
- [ ] problems
- [ ] build output
- [ ] terminal integration

## Fase 5 — Tooling

- [ ] Icon Center
- [ ] SVG conversion
- [ ] App Icon Studio
- [ ] Compose Preview
- [ ] APK inspector
- [ ] Logcat tools
- [ ] SDK Manager UI
- [ ] NDK Manager UI

## Fase 6 — Extensibilidade

- [ ] plugin API
- [ ] extension registry
- [ ] lifecycle
- [ ] permissions
- [ ] compatibility checks
- [ ] plugin catalog

## Fase 7 — IA opcional

- [ ] provider API
- [ ] secret storage
- [ ] project context
- [ ] chat
- [ ] code actions
- [ ] patch application