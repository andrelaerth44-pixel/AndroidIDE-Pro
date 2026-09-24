# AndroidIDE Pro — Changelog

## Unreleased

### Native core

- C e C++ tratados como linguagens built-in de primeira classe.
- suporte para projetos somente C.
- suporte para projetos somente C++.
- suporte para projetos mistos C + C++.
- suporte para Java/Kotlin + C/C++ híbridos.
- NativeActivity com android_native_app_glue.
- JNI headers gerados automaticamente por javac.
- src/main/cpp/androidide-native.properties.
- include paths e library paths nativos.
- static .a inputs.
- prebuilt .so em src/main/jniLibs.
- compile database compile_commands.json.
- Core LLVM com clang, clang++, lld e clangd Android-hosted.
- LLVM pack com sysroot, libc++, runtime e glue.
- biblioteca nativa configurável por projeto.
- link de bibliotecas Android e bibliotecas nativas próprias.

### Build Engine

- D8 é pulado para projetos sem bytecode Java/Kotlin.
- BuildConfig é pulado para projetos puramente nativos.
- Build Engine gera JNI headers antes da compilação nativa.
- pacote APK inclui bibliotecas nativas locais e de AARs.
- Build Result transporta applicationId para Install/Run.
- TaskGraph invalida outputs ausentes.
- Native Build Router continua sem Gradle fallback.

### Kotlin

- Core Kotlin Toolchain separado do APK-base.
- Compose compiler hosted pack separado.
- compiler plugin classpaths preparados no build nativo.

### Ainda precisa de validação

- GitHub Actions ainda não concluiu o build atual.
- build/install/launch físico em Android arm64;
- teste físico C;
- teste físico C++;
- teste físico Kotlin + C++ JNI;
- teste físico Java;
- Compose real end-to-end;
- AAR/JAR edge cases;
- desugaring;
- R8;
- AAB;
- múltiplas ABIs;
- integração clangd no editor;
- LLDB/debug nativo.

## Foundation

- Build API.
- TaskGraph.
- AAPT2.
- JavacTool/nb-javac.
- D8.
- APK package/zipalign/sign.
- Workspace adapter.
- Compose Material 3 surfaces.
- built-in language core.
- immutable language registry.
- plugin capability model without language/build capabilities.
