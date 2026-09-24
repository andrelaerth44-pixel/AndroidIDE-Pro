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
- [ ] selecionar backend nativo antes do fallback Gradle
- [ ] migrar `compileJava` para o engine próprio
- [ ] migrar `assembleDebug` completo para o engine próprio
- [ ] implementar engine Android leve completo
- [x] tornar Gradle explicitamente fallback de compatibilidade em arquitetura/documentação
- [ ] descoberta completa de tasks do projeto

## Fase 2 — Android pipeline

- [ ] generateSources
- [x] implementar task `mergeResources` nativa (integração no grafo ainda pendente)
- [ ] checkAarMetadata
- [ ] mergeNativeLibs
- [ ] mergeJavaResource
- [x] implementar task `aapt2Compile` nativa (integração no grafo ainda pendente)
- [ ] processManifest
- [ ] injectAppLogProvider
- [x] implementar task `aapt2Link` nativa (integração no grafo ainda pendente)
- [ ] integrar geração de R ao grafo nativo; AAPT2 link já produz `R.java`
- [x] implementar task `compileJava` nativa (integração no grafo ainda pendente)
- [ ] Kotlin
- [ ] KSP
- [ ] Compose
- [ ] dexBuilder
- [ ] mergeProjectDex
- [ ] mergeExtDex
- [ ] package APK
- [ ] signing
- [ ] APK verification
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