# AndroidIDE Pro — Native Build Engine

## Objetivo

Compilar, empacotar, alinhar e assinar aplicações Android diretamente no dispositivo, sem Gradle daemon no build do projeto.

O Build Router possui somente uma rota:

~~~text
Workspace
  ↓
BuildRouter
  ↓
NativeAndroidBuildSystem
  ↓
TaskGraph
~~~

## Grafo atual

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

## Java

Java usa o JavacTool/nb-javac já integrado ao AndroidIDE.

O processo não chama java.home/bin/javac.

Classpath inclui android.jar, classes do próprio módulo e JARs do Workspace.

As versões source/target vêm do modelo do módulo.

## Kotlin

O compilador Kotlin não fica mais no APK-base.

~~~text
Core Kotlin Toolchain APK
        ↓
package classloader
        ↓
K2JVMCompiler
        ↓
classes.jar
        ↓
D8
~~~

A tarefa Kotlin usa reflexão para carregar as classes do compiler e encaminha diagnósticos para o BuildContext.

Quando não existem fontes .kt, a tarefa produz um output vazio estável para manter o DAG incremental.

## C/C++

~~~text
Core LLVM Toolchain APK
        ↓
AndroidNativeToolchain
        ↓
clang / clang++ / lld
        ↓
libappnative.so
~~~

O primeiro backend usa C17, C++20 e arm64-v8a.

O link passa flags de páginas de 16 KiB.

## Projeto puramente nativo

Um projeto pode não possuir Java nem Kotlin.

Nesse cenário:

- BuildConfig não precisa ser gerado;
- javac é pulado;
- Kotlin é pulado;
- D8 é pulado;
- libraries nativas continuam no APK;
- AAPT2 processa manifesto/resources;
- APK segue para zipalign e assinatura.

## Recursos

~~~text
merged resources
      ↓
aapt2 compile
      ↓
dependency .flat files
      +
application .flat files
      ↓
aapt2 link
~~~

Recursos de Android libraries são compilados separadamente e entram no link final.

## D8

D8 recebe classes Java/Kotlin, android.jar, compile classpath e minSdk.

Quando não existe bytecode de programa, o estágio grava um marcador e não executa D8.

## Packaging

O empacotador inclui:

- resources.ap_;
- classes*.dex, quando existem;
- lib/*/*.so;
- assets/*.

C++ recebe libc++_shared.so quando necessário.

## Incrementalidade

O TaskGraph usa fingerprints SHA-256 persistentes.

A versão atual inclui:

- versão do algoritmo;
- ID da tarefa;
- inputs recursivos;
- outputs;
- inventário de diretórios;
- tamanho e mtime dos arquivos.

Se um output desaparece, a tarefa é executada novamente. Se um filho desaparece de um diretório de output, a tarefa também é invalidada.

## Assinatura

~~~text
package
  ↓
zipalign
  ↓
apksigner
~~~

A debug keystore é provisionada pelo próprio AndroidIDE Pro. Ela é somente de desenvolvimento.

## Limites atuais

Ainda faltam:

- AAR dependency graph completo;
- desugaring completo;
- Compose compiler integration;
- multidex de produção;
- R8;
- AAB;
- flavors/build types completos;
- release signing UI;
- múltiplas ABIs;
- CMake;
- clangd;
- JNI Wizard;
- debugging nativo;
- Run/Debug integrado.

Nada disso deve reintroduzir Gradle no Build Router.
