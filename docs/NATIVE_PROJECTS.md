
# AndroidIDE Pro — Native Projects

Este documento define os três formatos oficiais de projeto C/C++ do AndroidIDE Pro.

## 1. Aplicativo somente C

Estrutura:

~~~
app/
├── src/main/AndroidManifest.xml
├── src/main/cpp/
│   ├── main.c
│   └── androidide-native.properties
└── src/main/res/
~~~

O manifest declara:

~~~xml
<activity
    android:name="android.app.NativeActivity"
    android:exported="true">
    <meta-data
        android:name="android.app.lib_name"
        android:value="main" />
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
~~~

O Build Engine reconhece NativeActivity, compila android_native_app_glue.c do Core LLVM e produz:

~~~
lib/arm64-v8a/libmain.so
~~~

O ponto de entrada padrão é ANativeActivity_onCreate, preservado pelo linker.

A documentação oficial do Android define NativeActivity como uma forma de implementar uma Activity puramente nativa; o nome da biblioteca pode ser definido por android.app.lib_name e o ponto de entrada por android.app.func_name. citeturn634849search0turn634849search7

## 2. Aplicativo somente C++

A estrutura é igual, mas as fontes podem usar .cpp, .cc, .cxx, .cppm e .ixx.

O backend usa:

~~~
clang++
C++20
libc++_shared.so
LLD
~~~

O projeto pode usar:

- threads;
- RAII;
- exceções;
- RTTI;
- OpenGL ES/EGL;
- Vulkan;
- android;
- log;
- APIs NDK;
- bibliotecas nativas próprias.

A biblioteca final também pode ser renomeada pela configuração nativa.

## 3. Aplicativo híbrido Java/Kotlin + C/C++

Estrutura:

~~~
app/
├── src/main/java/
│   └── ...Java...
├── src/main/kotlin/
│   └── ...Kotlin...
├── src/main/cpp/
│   ├── native_bridge.cpp
│   └── androidide-native.properties
└── src/main/jniLibs/
    └── arm64-v8a/
        └── libthirdparty.so
~~~

O Java/Kotlin pode carregar:

~~~kotlin
companion object {
    init {
        System.loadLibrary("appnative")
    }
}
~~~

O Build Engine:

1. compila Java;
2. gera headers JNI com javac -h;
3. disponibiliza os headers no diretório generated/debug/jni;
4. compila C/C++;
5. linka libappnative.so;
6. empacota a biblioteca;
7. gera o APK;
8. instala e pode executar o aplicativo.

Isso permite um desenho em que:

~~~
Kotlin/Java
   ↓
UI, lifecycle, state, Android framework
   ↓ JNI
C/C++
   ↓
motor de desenho / animação / áudio / física / imagem / jogo
~~~

## Configuração nativa sem Gradle

Crie:

~~~
src/main/cpp/androidide-native.properties
~~~

Exemplo:

~~~properties
libraryName=appnative

includeDirs=include,third_party/foo/include
libraryDirs=third_party/foo/lib

linkLibraries=log,android,EGL,GLESv3

staticLibraries=libs/libbrush.a

cFlags=-Wall -Wextra
cppFlags=-Wall -Wextra -fno-rtti

linkerFlags=-Wl,--gc-sections
~~~

Paths relativas começam em src/main/cpp.

### libraryName

Define o nome da biblioteca compartilhada final.

Sem configuração:

~~~
NativeActivity → main
híbrido → appnative
~~~

### includeDirs

Adiciona diretórios de include.

### libraryDirs

Adiciona diretórios de biblioteca para o linker.

### linkLibraries

Cada item vira uma biblioteca -l.

### staticLibraries

Aceita caminhos para .a e outras entradas de link consumíveis pelo Clang/LLD.

### cFlags, cppFlags, linkerFlags

São argumentos nativos diretos. Eles existem para engines que precisam de otimizações, warnings, macros, sanitizers ou opções específicas sem introduzir CMake/Gradle.

## Bibliotecas nativas pré-compiladas

Bibliotecas compartilhadas podem ser colocadas em:

~~~
src/main/jniLibs/arm64-v8a/
~~~

Elas entram no APK em:

~~~
lib/arm64-v8a/
~~~

Bibliotecas estáticas podem ficar em:

~~~
src/main/cpp/libs/
~~~

e ser listadas em staticLibraries.

Dependências AAR que já possuem jni, assets e runtime JARs entram no pipeline nativo através do Workspace.

## NativeActivity e Hybrid

O AndroidIDE Pro trata os dois modelos de forma diferente:

| Tipo | Biblioteca padrão | Entrada |
|---|---|---|
| NativeActivity | main | ANativeActivity_onCreate |
| Hybrid Java/Kotlin | appnative | JNI escolhido pelo app |

Isso evita colocar Java/Kotlin artificialmente dentro de um app que o desenvolvedor quer 100% nativo.

## Exemplos de uso

### Motor de pincel

~~~
Kotlin/Java
  ├── Toolbar
  ├── Layers
  ├── Canvas UI
  └── Gestures
          ↓ JNI
C++
  ├── BrushEngine
  ├── PressureProcessor
  ├── StrokeSmoother
  ├── TileCache
  └── Renderer
~~~

### Editor de animação

~~~
Kotlin
  ├── timeline
  ├── panels
  └── project UI
          ↓ JNI
C++
  ├── frame cache
  ├── interpolation
  ├── compositing
  └── raster/vector engine
~~~

### Jogo nativo

~~~
NativeActivity
   ↓
C/C++
   ├── input
   ├── renderer
   ├── audio
   ├── physics
   └── game loop
~~~

### App de imagem

~~~
Kotlin
   ↓ JNI
C/C++
   ├── SIMD/image kernels
   ├── codecs
   ├── filters
   └── tile processing
~~~

## O que não é necessário

O caminho nativo não exige:

- Gradle para compilar C/C++;
- CMake;
- ndk-build;
- Android Studio;
- um NDK desktop instalado no telefone.

CMake e outros sistemas podem futuramente ser suportados como geradores de projetos, mas não são o backend obrigatório do AndroidIDE Pro.

## Limitação atual do primeiro ABI

O vertical slice atual compila e empacota arm64-v8a.

A expansão para:

- armeabi-v7a;
- x86_64;

fica como próxima etapa de ABI, não como requisito para o backend C/C++ deixar de ser nativo.
