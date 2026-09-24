<p align="center">
  <img src="./images/icon.png" alt="AndroidIDE" width="80" height="80"/>
</p>

<h2 align="center"><b>AndroidIDE</b></h2>
<p align="center">
  An IDE to develop real, Gradle-based Android applications on Android devices.
<p><br>

<p align="center">
<!-- Latest release -->
<img src="https://img.shields.io/github/v/release/AndroidIDEOfficial/AndroidIDE?include_prereleases&amp;label=latest%20release" alt="Latest release">
<!-- Build and test -->
<img src="https://github.com/AndroidIDEOfficial/AndroidIDE/actions/workflows/build.yml/badge.svg" alt="Builds and tests">
<!-- CodeFactor -->
<img src="https://www.codefactor.io/repository/github/androidideofficial/androidide/badge/main" alt="CodeFactor">
<!-- Crowdin -->
<a href="https://crowdin.com/project/androidide"><img src="https://badges.crowdin.net/androidide/localized.svg" alt="Crowdin"></a>
<!-- License -->
<img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License"></p>

<p align="center">
  <a href="https://docs.androidide.com/">Explore the docs »</a> &nbsp; &nbsp;
</p>

<p align="center">
  <a href="https://github.com/AndroidIDEOfficial/AndroidIDE/issues/new?labels=bug&template=BUG.yml&title=%5BBug%5D%3A+">Report a bug</a> &nbsp; &#8226; &nbsp;
  <a href="https://github.com/AndroidIDEOfficial/AndroidIDE/issues/new?labels=feature&template=FEATURE.yml&title=%5BFeature%5D%3A+">Request a feature</a> &nbsp; &#8226; &nbsp;
  <a href="https://t.me/androidide_discussions">Join us on Telegram</a>
</p>

> [!WARNING]
> 
> THIS PROJECT IS NOT MAINTAINED ANYMORE.

---

# AndroidIDE Pro

> Transformar o AndroidIDE em um IDE Android completo, moderno e on-device: criar, editar, compilar, assinar, instalar e executar aplicativos diretamente no celular ou tablet.

O AndroidIDE Pro não é apenas uma atualização visual. A evolução acontece em conjunto: IDE, editor, workspace, sistema de projetos, build system, toolchain, UX e suporte a linguagens.

## Visão

~~~text
AndroidIDE Pro
├── UI / UX
│   └── Kotlin + Jetpack Compose + Material 3
├── IDE Services
│   ├── Workspace
│   ├── Indexing
│   ├── LSP
│   ├── Diagnostics
│   ├── Search
│   └── Refactoring
├── Build Router
│   ├── Native Build (principal)
│   └── Gradle (fallback)
└── Native Build Engine
    ├── Resources
    ├── AAPT2
    ├── Java / Kotlin
    ├── D8 / R8
    ├── APK / AAB
    ├── Align
    └── Sign
~~~

## Princípios

### On-device first

O Android é a plataforma de desenvolvimento. O fluxo desejado é:

~~~text
criar projeto → editar → compilar no aparelho → gerar APK → assinar → instalar → executar
~~~

### Native build first

Projetos simples devem usar o build engine nativo sem iniciar o Gradle daemon. Gradle permanece como backend de compatibilidade para projetos que realmente precisem dele.

~~~text
NativeBuildRouter
├── NativeAndroidBuildSystem  ← caminho principal
└── GradleBackend             ← fallback
~~~

### Leveza acima de espetáculo

Preferir hierarquia visual, espaçamento consistente, Material 3, dynamic color, transparência moderada, animações funcionais e excelente densidade de informação.

Evitar blur pesado permanente, animações excessivas, sombras exageradas, gradientes decorativos, excesso de cards e efeitos que desperdicem RAM, CPU ou bateria.

### Kotlin-first

Todo código novo do AndroidIDE Pro deve priorizar Kotlin. Java continua sendo linguagem de projeto e pode permanecer em áreas onde a compatibilidade ou integração justificar.

### Compose para a nova UI

A nova superfície visual será construída progressivamente com Kotlin, Jetpack Compose e Material 3. A migração será incremental; não é necessário reescrever o IDE inteiro de uma vez.

## UI Pro

A primeira superfície Compose já existe em:

~~~text
core/app/src/main/java/com/itsaky/androidide/ui/pro/
├── AndroidIDEProTheme.kt
└── ProHomeScreen.kt
~~~

Ela já usa Material 3, tema claro/escuro, dynamic color em Android compatível, superfícies translúcidas moderadas e os fluxos existentes de projeto, terminal, Git e preferências.

A próxima evolução visual será Workspace, Project Explorer, Editor, Build panel, Problems, Terminal, Settings, SDK Manager e Git.

## Native Build Engine

A fundação inicial está em:

~~~text
core/build-api/
core/build-engine/
core/android-build/
~~~

O primeiro grafo de compilação é:

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
dexBuilderDebug
        ↓
packageApkDebug
        ↓
zipalignDebug
        ↓
signDebug
~~~

O alvo é chegar a um APK instalável sem depender do Gradle para esse caminho.

## Task Graph

Build não será um método monolítico. Cada operação deve declarar entradas, saídas e dependências explícitas, permitindo up-to-date checks, cache incremental, cancelamento, logs por tarefa e paralelismo futuro.

## Workspace e Project Model

O projeto externo será convertido para um modelo interno:

~~~text
Arquivos de projeto
      ↓
Parser / importador
      ↓
ProjectModel
      ↓
AndroidModule
      ↓
Native Build Graph
~~~

A meta não é copiar o Gradle. A meta é interpretar as partes relevantes do projeto e construir um modelo declarativo que o IDE controla.

## Editor Pro

O editor deve continuar leve e poderoso no telefone e no tablet. A evolução inclui tabs eficientes, code folding, autocomplete, diagnostics inline, quick fixes, goto definition, symbol search, multi-cursor, busca avançada, command palette, restauração de sessão e otimização para toque e teclado.

## Linguagens

Prioridade inicial:

~~~text
Java
Kotlin
XML
JSON
Markdown
~~~

Expansão prevista:

~~~text
C
C++
JavaScript / TypeScript
Python
Rust
Shell
YAML
~~~

Suporte completo significa editor + language services + build/run, e não apenas syntax highlighting.

## Android moderno

Evoluir gradualmente para Android SDK moderno, Java moderno, Kotlin, AndroidX, Jetpack, Compose, Material 3, AAR/JAR, resource processing, desugaring, multidex, R8/ProGuard, NDK, CMake e JNI.

## Diagnósticos

Saídas do javac, kotlinc, AAPT2, D8, R8 e toolchains nativos devem virar diagnósticos estruturados com arquivo, linha, coluna, severidade, mensagem, origem e quick fix quando possível.

## Install / Run

O build deve terminar com uma ação clara:

~~~text
Build → Verify → Install → Launch
~~~

## Performance

Toda feature nova deve considerar RAM, CPU, I/O, bateria, tempo de inicialização, tamanho do APK, cache e processos externos. O objetivo é executar somente o trabalho necessário.

## Tablet

O tablet será tratado como uma experiência própria, aproveitando espaço para Explorer, Editor e Problems/Build simultaneamente, enquanto o telefone usa painéis e bottom sheets quando necessário.

## Current implementation snapshot

~~~text
Editor toolbar
    ↓
NativeBuildAction
    ↓
NativeBuildCoordinator
    ↓
Existing IWorkspace / AndroidModule
    ↓
NativeAndroidBuildSystem
    ↓
AAPT2 → javac → D8 → package → zipalign → apksigner
~~~

Esta ponte foi projetada para reutilizar o modelo de projeto existente do AndroidIDE em vez de criar um segundo Workspace paralelo.

## Roadmap

### Fase 1 — Fundação

- [x] Build API inicial
- [x] TaskGraph inicial
- [x] AndroidModule inicial
- [x] AAPT2 compile/link
- [x] BuildConfig generation
- [x] Java compilation path
- [x] D8 invocation path
- [x] APK packaging
- [x] zipalign
- [x] debug signing
- [x] primeira superfície Compose
- [x] adapter Workspace → Native Build Engine
- [x] ação Build nativa no editor
- [ ] Build Center com progresso e logs estruturados
- [ ] gerar e instalar Hello World no dispositivo

### Fase 2 — Workspace Pro

- [ ] Project Explorer moderno
- [ ] recent projects
- [ ] module model
- [ ] build status
- [ ] build output
- [ ] install/run actions
- [ ] diagnostics estruturados
- [ ] build cancellation

### Fase 3 — Editor Pro

- [ ] tabs
- [ ] command palette
- [ ] symbol search
- [ ] quick actions
- [ ] inline diagnostics
- [ ] multi-cursor
- [ ] touch/keyboard optimization
- [ ] tablet layout

### Fase 4 — Kotlin e Android moderno

- [ ] Kotlin language services
- [ ] Kotlin compilation
- [ ] Compose project support
- [ ] modern AndroidX
- [ ] desugaring
- [ ] AAR/JAR handling
- [ ] multidex

### Fase 5 — Build Pro

- [ ] persistent build cache
- [ ] parallel task execution
- [ ] dependency graph
- [ ] static Gradle parser
- [ ] flavors
- [ ] release signing
- [ ] R8
- [ ] APK/AAB pipeline

### Fase 6 — Native

- [ ] NDK toolchain
- [ ] C/C++
- [ ] CMake
- [ ] JNI
- [ ] native diagnostics
- [ ] native incremental builds

### Fase 7 — Multi-language IDE

- [ ] JavaScript / TypeScript
- [ ] Python
- [ ] Rust
- [ ] Shell
- [ ] YAML
- [ ] additional LSP integrations

## Regras de arquitetura

1. O núcleo não depende da UI.
2. A UI não conhece detalhes internos do toolchain.
3. O build nativo não depende do Gradle para executar.
4. Gradle continua como fallback de compatibilidade.
5. Cada tarefa tem entradas e saídas claras.
6. Recursos pesados são lazy e incrementais.
7. O editor permanece utilizável em aparelhos modestos.
8. Nova UI prioriza clareza sobre efeitos.
9. Recursos experimentais podem ser desativados.
10. Mudanças arquiteturais importantes são documentadas neste README.

## Regra de ouro

> O AndroidIDE Pro deve responder: “Consigo desenvolver este aplicativo diretamente neste Android?”

Quando a resposta for não, a próxima tarefa deve identificar qual camada está faltando — editor, language service, project model, dependências, compilador, AAPT2, D8, R8, NDK, packaging, signing, install, run ou debug — e construir essa capacidade de forma on-device.

---
## Features

- [x] Gradle support.
- [x] `JDK 11` and `JDK 17` available for use.
- [x] Terminal with necessary packages.
- [x] Custom environment variables (for Build & Terminal).
- [x] SDK Manager (Available via terminal).
- [x] API information for classes and their members (since, removed, deprecated).
- [x] Log reader (shows your app's logs in real-time)
- [ ] Language servers
    - [x] Java
    - [x] XML
    - [ ] Kotlin
- [ ] UI Designer
    - [x] Layout inflater
    - [x] Resolve resource references
    - [x] Auto-complete resource values when user edits attributes using the attribute editor
    - [x] Drag & Drop
    - [x] Visual attribute editor
    - [x] Android Widgets
- [ ] String Translator
- [ ] Asset Studio (Drawable & Icon Maker)
- [x] Git

## Installation

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.svg"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/com.itsaky.androidide)
[<img src="https://github.com/Kunzisoft/Github-badge/raw/main/get-it-on-github.svg"
    alt="Get it on F-Droid"
    height="80">](https://github.com/AndroidIDEOfficial/AndroidIDE/releases)

> _Please install AndroidIDE from trusted sources only i.e._
> - [_The AndroidIDE website_](https://androidide.com)
> - [_GitHub Releases_](https://github.com/AndroidIDEOfficial/AndroidIDE/releases)
> - [_GitHub Actions_](https://github.com/AndroidIDEOfficial/AndroidIDE/actions?query=branch%3Adev+event%3Apush)
> - [_F-Droid_](https://f-droid.org/packages/com.itsaky.androidide/)

- Download the AndroidIDE APK from the mentioned trusted sources.
- Follow the
  instructions [here](https://docs.androidide.com/tutorials/get-started.html) to
  install the build tools.

## Limitations

- For working with projects in AndroidIDE, your project must use Android Gradle Plugin v7.2.0 or
  newer. Projects with older AGP must be migrated to newer versions.
- SDK Manager is already included in Android SDK and is accessible in AndroidIDE via its Terminal.
  But, you cannot use it to install some tools (like NDK) because those tools are not built for
  Android.
- No official NDK support because we haven't built the NDK for Android.

The app is still being developed actively. It's in beta stage and may not be stable. if you have any
issues using the app, please let us know.

## Contributing

See the [contributing guide](./CONTRIBUTING.md).

For translations, visit the [Crowdin project page](https://crowdin.com/project/androidide).

## Thanks to

- [Rosemoe](https://github.com/Rosemoe) for the
  awesome [CodeEditor](https://github.com/Rosemoe/sora-editor)
- [Termux](https://github.com/termux) for [Terminal Emulator](https://github.com/termux/termux-app)
- [Bogdan Melnychuk](https://github.com/bmelnychuk)
  for [AndroidTreeView](https://github.com/bmelnychuk/AndroidTreeView)
- [George Fraser](https://github.com/georgewfraser) for
  the [Java Language Server](https://github.com/georgewfraser/java-language-server)

Thanks to all the developers who have contributed to this project.

<p>This project is supported by:</p>
<p>
  <a href="https://m.do.co/c/54add371d1d7">
    <img src="https://opensource.nyc3.cdn.digitaloceanspaces.com/attribution/assets/SVG/DO_Logo_horizontal_blue.svg" width="201px">
  </a>
</p>

## Contact Us

- [Website](https://m.androidide.com)
- [Telegram](https://t.me/androidide_discussions)

## License

```
AndroidIDE is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

AndroidIDE is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
```

Any violations to the license can be reported either by opening an issue or writing a mail to us
directly.
