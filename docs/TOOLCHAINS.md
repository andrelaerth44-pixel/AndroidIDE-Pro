# AndroidIDE Pro — Core Toolchains

## Regra

Compiladores e toolchains são parte do AndroidIDE Pro, mas os binários pesados não precisam ficar dentro do APK-base.

Isso não cria plugins de linguagem.

A arquitetura é:

~~~text
AndroidIDE Pro APK
    ↓
Core Toolchain Manager
    ↓
app-specific files/toolchains/
    ├── kotlin/
    ├── llvm/
    ├── rust/
    ├── go/
    └── javascript/
~~~

O usuário não ativa ou desativa uma linguagem. O IDE conhece a linguagem e administra automaticamente os componentes necessários para compilá-la.

## Por que separar do APK

Um compiler/toolchain completo pode ser grande.

O Kotlin compiler embeddable 1.9.24, por exemplo, tem aproximadamente 60 MB como artefato JAR antes das demais dependências. A toolchain Android NDK completa r30 distribuída para Linux tem cerca de 739 MB. O AndroidIDE Pro não deve copiar um NDK de desktop inteiro para dentro do APK.

O Pro deve distribuir apenas o subconjunto necessário para executar o compilador no Android.

## Kotlin

O primeiro pack já é gerado pelo build:

~~~text
core/android-build/build/toolchains/kotlin/
~~~

O workflow do GitHub Actions publica esse pack separado do APK.

Próxima etapa: fazer o runtime Kotlin carregar esse pack por classloader interno em vez de depender das classes do compiler dentro do APK.

## LLVM C/C++

O pack nativo será:

~~~text
toolchains/llvm/
└── arm64-v8a/
    ├── bin/
    ├── lib/
    └── sysroot/
~~~

Somente binários capazes de executar no próprio Android serão incluídos.

## Regras de tamanho

1. Não embutir um SDK/NDK desktop inteiro.
2. Não duplicar compiler/runtime dentro do APK de projeto.
3. Preferir packs por host ABI.
4. Distribuir somente arquivos realmente usados pelo Build Engine.
5. Remover símbolos e ferramentas de desenvolvimento que não são necessários em produção.
6. Ter relatório automático do tamanho do APK e de cada toolchain pack.
7. Estabelecer orçamento máximo de tamanho antes de promover uma toolchain ao release.

## Estado

- [x] conceito de Core Toolchain Pack
- [x] Kotlin pack gerado pelo Gradle
- [x] Kotlin pack publicado pelo CI
- [ ] Kotlin runtime classloader
- [ ] Core Toolchain Manager
- [ ] LLVM Android arm64 pack
- [ ] checksum + assinatura dos packs
- [ ] atualização incremental de toolchains
- [ ] garbage collection de versões antigas
