
# AndroidIDE Pro — Native Build Engine

## Objetivo

Compilar, empacotar, alinhar e assinar aplicações Android diretamente no dispositivo, sem depender de Gradle daemon e sem trocar de backend quando o projeto usa outra linguagem nativa.

## Pipeline atual

~~~text
mergeResourcesDebug
        ↓
aapt2CompileDebug
        ↓
aapt2LinkDebug
        ↓
generateBuildConfigDebug
        ↓
compileJavaDebug
        ↓
compileKotlinDebug
        ↓
compileNativeDebug
        ↓
dexBuilderDebug
        ↓
packageApkDebug
        ↓
zipalignDebug
        ↓
signDebug
~~~

## Linguagens

Java usa `JavacTool`/nb-javac embarcado no próprio processo do Build Engine.

Kotlin usa K2JVMCompiler embutido em processo, produzindo classes.jar para D8.

C/C++ usam a toolchain LLVM executável no Android, compilando C17/C++20 e ligando libappnative.so para arm64-v8a. O link usa flags compatíveis com páginas de 16 KiB.

Dependências JAR entram no classpath do javac/kotlinc e no input do D8. Recursos de Android libraries são compilados separadamente por AAPT2 e entram no mesmo link final; isso evita sobrescrever arquivos `values/*.xml` inteiros durante o merge. Os diretórios de recursos também fazem parte dos fingerprints.

## Build Router

O Build Router agora tem uma única rota:

~~~text
Workspace + AndroidModule
        ↓
BuildRouter
        ↓
NativeAndroidBuildSystem
        ↓
Built-in language pipeline
~~~

Não existe mais GradleBackend no roteador. Quando uma capacidade nativa ainda está incompleta, o build falha com diagnóstico explícito.

## Grafo e cache

Cada tarefa declara inputs, outputs e dependências. O TaskGraph usa fingerprints SHA-256 persistentes para UP-TO-DATE.

## Estado atual

Implementado no código:

- Build API;
- TaskGraph;
- fingerprints persistentes;
- AndroidModule;
- Android SDK/tool resolution;
- AAPT2;
- BuildConfig;
- Java build path com compilador embarcado;
- Kotlin compiler embutido;
- C/C++ native task;
- D8;
- APK packaging;
- zipalign;
- debug signing;
- Workspace adapter;
- Build Center;
- instalação de APK;
- rota única nativa.

Ainda falta a validação física em dispositivo e a distribuição real da LLVM executável no Android.

## Core Toolchains

Toolchains pesados pertencem ao produto, mas devem ser armazenados em `filesDir/toolchains/` e administrados pelo Core Toolchain Manager. O CI já gera um pack Kotlin separado para permitir essa evolução sem transformar linguagem em plugin.

## Próximas etapas

1. unificar o compilador Java de build com o javac embarcado usado pelo LSP;
2. distribuir/gerenciar a LLVM toolchain Android;
3. AAR/JAR classpath;
4. desugaring;
5. multidex;
6. Compose compiler plugins;
7. R8;
8. release/AAB;
9. múltiplas ABIs;
10. CMake/JNI.
