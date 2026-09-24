# Pipeline de compilação Android — referência CodeAssist

## Objetivo

Este documento registra o que foi observado no CodeAssist instalado em um dispositivo Android e
cruza a observação com a arquitetura pública atual do CodeAssist. O objetivo do AndroidIDE Pro é
adotar a mesma ideia central: **Gradle não é o engine de compilação do APK**.

O Gradle continua apenas como camada de compatibilidade para projetos que exigem comportamento que
o engine nativo ainda não cobre.

## Evidência observada

Em um projeto Hello World compilado no CodeAssist, a interface mostrou 17 etapas concluídas:

1. `:app:generateSourcesDebug`
2. `:app:mergeResourcesDebug`
3. `:app:checkAarMetadataDebug`
4. `:app:mergeNativeLibsDebug`
5. `:app:mergeJavaResourceDebug`
6. `:app:aapt2CompileDebug`
7. `:app:processManifestDebug`
8. `:app:injectAppLogProviderDebug`
9. `:app:aapt2LinkDebug`
10. `:app:generateRFileDebug`
11. `:app:compileJavaDebug`
12. `:app:dexBuilderDebug`
13. `:app:mergeProjectDexDebug`
14. `:app:mergeExtDexDebug`
15. `:app:packageApkDebug`
16. `:app:signDebug`
17. `:app:assembleDebug`

Esses nomes e essa ordem são tratados aqui como **observação de uma execução real**, não como uma
alegação de que toda versão do CodeAssist usará exatamente os mesmos IDs em todos os projetos.

## O que o código-fonte público do CodeAssist confirma

O README atual do CodeAssist descreve uma arquitetura sem daemon Gradle: o projeto é modelado
internamente, transformado em um DAG incremental de tarefas, e as ferramentas Android são acionadas
diretamente. A documentação também descreve fingerprint de inputs/outputs, cache persistente,
execução limitada e diagnósticos estruturados.

A própria documentação pública do build system separa:

- `BuildSystem`: cria o grafo de build;
- engine incremental: verifica fingerprints, cacheia outputs e executa o DAG;
- pipeline Java: usa backend de compilação próprio;
- pipeline Android: organiza recursos, dex, empacotamento e assinatura como tarefas;
- Gradle: compatibilidade/importação, não o runtime normal do build.

O README atual também explicita que ferramentas Java como JDT/ecj, D8/R8 e apksigner podem rodar no
processo do IDE, enquanto AAPT2 nativo pode ser executado como subprocesso.

## Pipeline conceitual do AndroidIDE Pro

O alvo passa a ser:

```
Project Model
    ↓
Dependency / Classpath Resolution
    ↓
Task DAG + Incremental State
    ↓
generateSources
    ↓
mergeResources ───────────────┐
checkAarMetadata ─────────────┤
mergeNativeLibs ──────────────┤
mergeJavaResource ────────────┤
                              ↓
                         aapt2Compile
                              ↓
                       processManifest
                              ↓
                    inject / generated sources
                              ↓
                         aapt2Link
                              ↓
                       generateRFile
                              ↓
                    compileJava / Kotlin
                              ↓
                         dexBuilder
                              ↓
              mergeProjectDex / mergeExtDex
                              ↓
                         packageApk
                              ↓
                           sign
                              ↓
                         assemble
```

O DAG real deverá permitir dependências paralelas onde isso reduzir trabalho e memória, mas o
executor inicial continuará deliberadamente conservador.

## Mapeamento para o AndroidIDE existente

Já existem peças úteis no repositório:

| Função | Peça existente | Papel pretendido |
|---|---|---|
| Modelo de projeto | `core:projects` / Workspace | alimentar o BuildProject |
| API do engine | `:build:api` | contratos de graph/task/diagnostics |
| Executor inicial | `SequentialBuildExecutor` | baseline determinístico |
| AAPT2 / recursos | `:xml:aaptcompiler` | reutilizar antes de criar outro compilador |
| Java | composite `java-compiler` / `javac` + `:java:javac-services` | compilação Java sem Gradle |
| D8/R8 | a incorporar ao toolchain próprio | dex |
| Assinatura | a incorporar ao toolchain próprio | APK signing |
| Compatibilidade | `:build:gradle-adapter` | fallback para projetos/recursos ainda não suportados |

## Princípios de implementação

### 1. Gradle não entra no caminho normal

O comando normal de APK deve:

```
AndroidIDE Pro → BuildSystem local → Task DAG → ferramentas Android
```

e não:

```
AndroidIDE Pro → Tooling API → Gradle → Android tools
```

### 2. Não recriar Gradle

Queremos apenas as propriedades úteis:

- DAG de tarefas;
- dependências explícitas;
- fingerprints;
- incrementalidade;
- cache;
- cancelamento;
- diagnósticos estruturados.

Não queremos um interpretador de scripts Gradle nem um daemon residente.

### 3. Reutilizar o que já existe

Antes de adicionar uma ferramenta nova, verificar:

1. composite builds existentes;
2. módulos Java/AAPT existentes;
3. toolchains distribuídos com o aplicativo;
4. bibliotecas Android já empacotadas;
5. APIs puras-Java que podem executar no ART.

### 4. Memória é requisito funcional

Cada tarefa deve evitar carregar o projeto inteiro em memória quando não for necessário.

O engine deve preferir:

- processamento por arquivo;
- outputs intermediários persistentes;
- cache por fingerprint;
- baixa concorrência;
- ferramentas de longa duração somente quando realmente úteis;
- subprocessos apenas para ferramentas nativas que necessitam disso.

### 5. Compatibilidade Gradle

Um projeto Gradle pode continuar sendo importado e compilado pelo adapter enquanto a cobertura
nativa cresce. A presença do adapter não significa que Gradle seja o engine principal.

## Próximas implementações

A ordem de implementação fica:

1. Project/ClassPath snapshot;
2. fingerprint incremental;
3. task state persistente;
4. `generateSources`;
5. `mergeResources`;
6. `checkAarMetadata`;
7. `processManifest`;
8. `aapt2Compile`;
9. `aapt2Link` + R;
10. `compileJava`;
11. `compileKotlin`;
12. `dexBuilder`;
13. merges de dex;
14. package APK;
15. assinatura;
16. assemble/install;
17. R8 para release;
18. AAB;
19. NDK/Clang/JNI.

A primeira meta de integração real deve ser um Hello World Java que execute essas etapas dentro do
engine nativo, ainda aceitando fallback Gradle quando uma etapa não estiver implementada.

## O que não foi concluído ainda

Este documento não afirma que o AndroidIDE Pro já consegue produzir um APK por esse caminho.

A implementação atual ainda tem apenas o primeiro runtime migrado (`clean`) e as fundações do
Build API. A compilação APK nativa será construída progressivamente.

## Referências

- CodeAssist README e arquitetura pública: https://github.com/tyron12233/CodeAssist
- CodeAssist build system: https://github.com/tyron12233/CodeAssist/blob/main/docs/build-system.md
- Android AAPT2: https://developer.android.com/tools/aapt2
