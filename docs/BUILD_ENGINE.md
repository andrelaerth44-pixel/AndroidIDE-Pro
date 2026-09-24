# Build Engine

## Objetivo

Executar builds Android diretamente no dispositivo com controle fino de tarefas, cache, memória e
toolchains.

A arquitetura-alvo é inspirada no modelo público do CodeAssist: um engine incremental próprio que
transforma o projeto em um DAG de tarefas e chama as ferramentas Android diretamente, sem hospedar
um daemon Gradle como caminho normal.

## Situação atual

O build legado ainda é Gradle-first:

- `GradleBuildService` vive como foreground service;
- `ToolingServerRunner` inicia um processo Java;
- `ToolingApiServerImpl` abre `ProjectConnection`;
- Gradle tasks são executadas por nome;
- cancelamento usa `CancellationTokenSource`;
- o sistema não permite dois builds concorrentes no mesmo serviço.

Esse caminho será preservado como **compatibilidade**. Não é a arquitetura-alvo para os builds APK
normais.

## Referência CodeAssist

A execução real observada no CodeAssist para um Hello World mostrou 17 etapas:

```
generateSourcesDebug
mergeResourcesDebug
checkAarMetadataDebug
mergeNativeLibsDebug
mergeJavaResourceDebug
aapt2CompileDebug
processManifestDebug
injectAppLogProviderDebug
aapt2LinkDebug
generateRFileDebug
compileJavaDebug
dexBuilderDebug
mergeProjectDexDebug
mergeExtDexDebug
packageApkDebug
signDebug
assembleDebug
```

A documentação pública atual do CodeAssist confirma a ideia arquitetural: projeto próprio, task DAG
incremental, fingerprints/cache persistente, ferramentas Java como JDT/ecj/D8/R8/apksigner no processo
e AAPT2 nativo como subprocesso.

A referência detalhada está em `docs/CODEASSIST_BUILD_PIPELINE.md`.

## Fronteira de substituição

`BuildService -> BuildSystem SPI -> {Native Android Engine | Gradle Compatibility}`

O app não deve precisar saber se uma operação específica veio do backend nativo ou do fallback.

assemble já tenta o engine nativo primeiro quando o Workspace oferece uma aplicação Android compatível com o conjunto de limitações atual. Quando a capacidade não existe, o fluxo cai para o adapter Gradle.

## API implementada

O módulo `:build:api` contém:

### Modelos

- `BuildProject`;
- `BuildModule`;
- `BuildRequest`;
- `BuildContext`;
- `TaskContext`.

### Build System

`BuildSystem` define:

- `id`;
- `supports(moduleType)`;
- `createBuildGraph(...)`;
- `tasks(project)`.

### Tasks

`BuildTask` declara:

- identidade;
- dependências;
- inputs;
- outputs;
- execução.

### Graph

`BuildGraph` valida:

- IDs vazios;
- IDs duplicados;
- dependências inexistentes;
- ciclos;

e fornece ordenação topológica determinística.

### Executor

`SequentialBuildExecutor` é a primeira implementação concreta do `BuildExecutor`. Ele fornece
a baseline correta para dependências, falha, cancelamento e diagnósticos antes de adicionarmos
concorrência limitada, fingerprints e cache.

## Grafo Android-alvo

```
generateSources
      |
      +---- mergeResources
      +---- checkAarMetadata
      +---- mergeNativeLibs
      +---- mergeJavaResource
                 |
            aapt2Compile
                 |
          processManifest
                 |
             aapt2Link
                 |
          generateRFile
                 |
        compileJava/Kotlin
                 |
            dexBuilder
          /             \
mergeProjectDex     mergeExtDex
          \             /
             packageApk
                 |
                sign
                 |
              assemble
```

O DAG final será variante-aware e poderá executar ramos independentes com paralelismo limitado por
memória. A execução atual permanece sequencial até existir um scheduler que conheça custo/memória.

## Incrementalidade

Fingerprint deve incluir somente o estado necessário para decidir se a task é válida:

- conteúdo dos inputs;
- caminhos lógicos;
- configuração/variant;
- classpath/dependências;
- toolchain;
- versão da implementação da task.

O estado da task deverá sobreviver ao processo do app para que uma nova compilação consiga pular
tarefas realmente inalteradas.

## Cache

O cache nativo deverá possuir:

- versionamento;
- content addressing onde útil;
- limites de espaço;
- limpeza/LRU;
- detecção de corrupção;
- invalidação por toolchain/configuração.

Não devemos construir um cache gigante que transforme armazenamento em novo gargalo.

## Toolchains existentes a reutilizar

O repositório já possui:

- composite build `java-compiler`;
- composite build `javac`;
- `:java:javac-services`;
- `:xml:aaptcompiler`;
- dependências AAPT2/protobuf relacionadas a recursos.

A regra é reutilizar primeiro. O engine não deve criar uma segunda implementação de javac/AAPT2 sem
justificativa técnica.

## Gradle Adapter

A implementação está em `:build:gradle-adapter`.

Ela já:

1. converte `IProject` para `BuildProject`;
2. preserva diretórios e módulos;
3. planeja `assemble`, `assemble<Variant>` e tasks requisitadas;
4. adiciona `clean` quando solicitado;
5. mantém execução atrás de `GradleTaskExecutor`;
6. aplica a política de recursos de compatibilidade.

O executor default do adapter continua deliberadamente não-operacional para impedir execução
acidental.

No app, `GradleBuildServiceTaskExecutor` faz a ponte para o serviço legado somente quando o
backend Gradle é selecionado.

## Regra de peso do AndroidIDE Pro

O aplicativo não deve carregar Gradle como biblioteca de execução no processo principal.

O fallback Gradle continua isolado e limitado. A própria documentação do Gradle observa que
`--no-daemon` pode resultar em uma JVM de uso único dependendo dos argumentos da JVM, portanto a
política não deve ser interpretada como "zero processos Gradle". A solução estrutural continua
sendo remover Gradle do caminho normal. [Gradle Daemon](https://docs.gradle.org/current/userguide/gradle_daemon.html)

A política existente mantém:

- heap limitado;
- `max-workers=1`;
- sem paralelismo;
- daemon desabilitado;
- file-system watching desabilitado;
- build cache desabilitado pelo fallback.

As opções são suportadas pelas interfaces de linha de comando/configuração do Gradle. [Gradle CLI](https://docs.gradle.org/current/userguide/command_line_interface.html) e [Gradle Build Environment](https://docs.gradle.org/current/userguide/build_environment.html)

## Operações migradas

O root task `clean` usa o engine leve sem iniciar Gradle.

Além disso, `GradleBuildService.executeTasks("assemble...")` já tenta um DAG nativo completo para o primeiro conjunto controlado:

```text
mergeResources
  -> aapt2Compile
  -> aapt2Link / R.java
  -> compileJava
  -> dexBuilder
  -> mergeProjectDex
  -> packageApk
  -> sign
  -> assemble
```

O primeiro caminho nativo usa apenas um módulo Android application, sem dependências externas ou módulos de projeto, e usa assinatura debug local. Projetos mais complexos continuam no fallback Gradle.

## Limitações atuais

O primeiro adapter do Workspace ainda recusa:

- múltiplos módulos de projeto;
- dependências locais entre módulos;
- bibliotecas AAR/JAR externas;
- Kotlin/KSP/Compose;
- manifest merger avançado;
- checkAarMetadata, mergeNativeLibs, mergeJavaResource, generateSources e injectAppLogProvider.

Essas recusas são intencionais: o native backend não inventa semântica de dependências que ainda não possui.

## Próxima sequência

A implementação deve seguir a sequência mostrada pelo CodeAssist:

1. Project/ClassPath snapshot;
2. fingerprint + estado persistente;
3. `generateSources`;
4. `mergeResources`;
5. `checkAarMetadata`;
6. `processManifest`;
7. `aapt2Compile`;
8. `aapt2Link` + R;
9. `compileJava`;
10. `compileKotlin`;
11. `dexBuilder`;
12. merges de dex;
13. `packageApk`;
14. `sign`;
15. `assemble/install`;
16. R8 para release;
17. AAB;
18. NDK/Clang/JNI.

A primeira integração executável deve ser um Hello World Java completo dentro do engine nativo,
sem Gradle, reutilizando os módulos de javac e AAPT já existentes.

## Validação

Ainda não foi executado o build Gradle completo do repositório, CI ou um APK real pelo novo engine.
Existem testes com toolchains falsos para o DAG e as tarefas, e a integração do serviço está conectada estruturalmente. A validação real no Android é o próximo passo.
