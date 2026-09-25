package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.BuildVariant
import com.itsaky.androidide.native.model.NativeBuildRequest
import com.itsaky.androidide.native.model.NativeLibraryType
import com.itsaky.androidide.native.model.NativeProjectModelLoader
import com.itsaky.androidide.native.lsp.ClangdCompilationDatabase
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchainLocator
import com.itsaky.androidide.utils.Environment
import java.io.File
import java.util.concurrent.TimeUnit

enum class NativeAndroidBuildStage {
  PREPARE,
  COMPILE_RESOURCES,
  LINK_RESOURCES,
  GENERATE_JNI_HEADERS,
  COMPILE_KOTLIN,
  COMPILE_JAVA,
  COMPILE_NATIVE,
  DEX,
  MERGE_DEX,
  MERGE_NATIVE,
  MERGE_ASSETS,
  ZIPALIGN,
  SIGN,
  SUCCESS,
  FAILED,
}

data class NativeAndroidBuildResult(
  val success: Boolean,
  val outputApk: File? = null,
  val failedStage: NativeAndroidBuildStage? = null,
  val message: String? = null,
)

class NativeAndroidBuildExecutor(
  private val commandExecutor: (
    NativeCommandSpec,
    Map<String, String>,
    NativeProcessController?,
    (String) -> Unit,
  ) -> NativeProcessResult = { command, environment, controller, onOutput ->
    NativeCommandExecutor.execute(command, environment, controller, onOutput)
  },
) {

  fun execute(
    moduleRoot: File,
    variant: BuildVariant = BuildVariant.DEBUG,
    abi: AbiTarget = AbiTarget.ARM64_V8A,
    controller: NativeProcessController? = null,
    onStage: (NativeAndroidBuildStage, String?) -> Unit = { _, _ -> },
    onOutput: (String) -> Unit = {},
  ): NativeAndroidBuildResult {
    fun fail(stage: NativeAndroidBuildStage, message: String): NativeAndroidBuildResult {
      onStage(NativeAndroidBuildStage.FAILED, message)
      return NativeAndroidBuildResult(
        success = false,
        failedStage = stage,
        message = message,
      )
    }

    onStage(NativeAndroidBuildStage.PREPARE, "Scanning Android module")

    val model =
      AndroidProjectModelLoader.load(moduleRoot)
        ?: return fail(
          NativeAndroidBuildStage.PREPARE,
          "Android module is missing src/main/AndroidManifest.xml or src/main/res",
        )

    val toolchain = AndroidBuildToolchainLocator.locate()
    if (!toolchain.javaReady) {
      return fail(
        NativeAndroidBuildStage.COMPILE_JAVA,
        "JDK javac is unavailable",
      )
    }
    if (!toolchain.dexReady) {
      return fail(
        NativeAndroidBuildStage.DEX,
        "Android D8 is unavailable in the SDK build-tools",
      )
    }
    if (!toolchain.packageReady) {
      return fail(
        NativeAndroidBuildStage.ZIPALIGN,
        "zipalign/apksigner are unavailable in the SDK build-tools",
      )
    }

    if (model.hasKotlin && !toolchain.kotlinReady) {
      return fail(
        NativeAndroidBuildStage.COMPILE_KOTLIN,
        "Kotlin sources were found but kotlinc is not provisioned",
      )
    }

    val buildDirectory =
      File(
        moduleRoot,
        ".androidide/native-android/" +
          variant.name.lowercase() + "/" +
          abi.androidAbiName,
      )
    buildDirectory.mkdirs()

    val compiledResources = File(buildDirectory, "resources.zip")
    val unsignedApk = File(buildDirectory, "unsigned.apk")
    val alignedApk = File(buildDirectory, "aligned.apk")
    val signedApk = File(buildDirectory, "outputs/" + moduleRoot.name + "-debug.apk")
    val generatedJava = File(buildDirectory, "generated")
    val classesDirectory = File(buildDirectory, "classes")
    val dexDirectory = File(buildDirectory, "dex")
    val kotlinClasspath = findKotlinStdlib(toolchain)
    val classpath = buildClasspath(model, kotlinClasspath)

    val compileResources =
      Aapt2CommandPlanner.planCompile(
        resourceDirectory = model.resourceDirectory,
        compiledResources = compiledResources,
      )
    onStage(NativeAndroidBuildStage.COMPILE_RESOURCES, "AAPT2 compile")
    val compileResult =
      runCommand(
        compileResources.asNativeSpec(),
        controller,
        onOutput,
      )
    if (!compileResult.success) {
      return fail(
        NativeAndroidBuildStage.COMPILE_RESOURCES,
        compileResult.output.ifBlank { "AAPT2 resource compilation failed" },
      )
    }

    val androidJar =
      runCatching { Environment.ANDROID_JAR }
        .getOrNull()
        ?.takeIf(File::isFile)
        ?: return fail(
          NativeAndroidBuildStage.LINK_RESOURCES,
          "Android API jar is unavailable",
        )

    val linkResources =
      Aapt2CommandPlanner.planLink(
        compiledResources = compiledResources,
        manifest = model.manifest,
        androidJar = androidJar,
        outputApk = unsignedApk,
        javaSourceOutput = generatedJava,
        minSdk = model.minSdk,
        targetSdk = model.targetSdk,
      )
    onStage(NativeAndroidBuildStage.LINK_RESOURCES, "AAPT2 link")
    val linkResult = runCommand(linkResources.asNativeSpec(), controller, onOutput)
    if (!linkResult.success) {
      return fail(
        NativeAndroidBuildStage.LINK_RESOURCES,
        linkResult.output.ifBlank { "AAPT2 resource linking failed" },
      )
    }

    val nativeModule =
      NativeProjectModelLoader.load(
        moduleRoot = moduleRoot,
        abi = abi,
        variant = variant,
      )

    val buildDirForNative = File(buildDirectory, "native")
    if (nativeModule != null) {
      onStage(NativeAndroidBuildStage.GENERATE_JNI_HEADERS, "Generate JNI headers")
      val headerCommand =
        JniHeaderGenerator.plan(
          moduleRoot = moduleRoot,
          buildDirectory = buildDirForNative,
        )
      if (headerCommand != null) {
        val headerResult = runCommand(headerCommand, controller, onOutput)
        if (!headerResult.success) {
          return fail(
            NativeAndroidBuildStage.GENERATE_JNI_HEADERS,
            headerResult.output.ifBlank { "JNI header generation failed" },
          )
        }
      }
    }

    generatedJava.walkTopDown()
      .filter { it.isFile && it.extension.equals("java", true) }
      .forEach { source ->
        val result = compileJavaFile(
          source = source,
          outputDirectory = classesDirectory,
          classpath = classpath,
          controller = controller,
          onOutput = onOutput,
        )
        if (!result.success) {
          return fail(
            NativeAndroidBuildStage.COMPILE_JAVA,
            result.output.ifBlank { "Generated Java compilation failed" },
          )
        }
      }

    if (model.hasKotlin) {
      onStage(NativeAndroidBuildStage.COMPILE_KOTLIN, "Compile Kotlin")
      val kotlinCommand =
        buildKotlinCommand(
          model = model,
          generatedJava = generatedJava,
          outputDirectory = classesDirectory,
          classpath = classpath,
          kotlin = requireNotNull(toolchain.kotlinc),
        )
      val result = runCommand(kotlinCommand, controller, onOutput)
      if (!result.success) {
        return fail(
          NativeAndroidBuildStage.COMPILE_KOTLIN,
          result.output.ifBlank { "Kotlin compilation failed" },
        )
      }
    }

    if (model.hasJava) {
      onStage(NativeAndroidBuildStage.COMPILE_JAVA, "Compile Java")
      val result =
        compileJavaSources(
          sources = model.javaSources,
          outputDirectory = classesDirectory,
          classpath = classpath + listOf(classesDirectory),
          controller = controller,
          onOutput = onOutput,
          javac = requireNotNull(toolchain.javac),
        )
      if (!result.success) {
        return fail(
          NativeAndroidBuildStage.COMPILE_JAVA,
          result.output.ifBlank { "Java compilation failed" },
        )
      }
    }

    if (nativeModule != null) {
      val nativeToolchain = NativeToolchainLocator.locate()
      val required =
        listOf(
          NativeToolId.CLANG,
          NativeToolId.CLANGXX,
        )
      val missing =
        required.firstOrNull { nativeModule.targets.any { target ->
          when {
            target.sourceSet.cSources.isNotEmpty() ->
              it == NativeToolId.CLANG && nativeToolchain.tool(it)?.path == null
            target.sourceSet.cppSources.isNotEmpty() ->
              it == NativeToolId.CLANGXX && nativeToolchain.tool(it)?.path == null
            else -> false
          }
        } }
      if (missing != null) {
        return fail(
          NativeAndroidBuildStage.COMPILE_NATIVE,
          "Native toolchain is missing " + missing.name,
        )
      }

      val request =
        NativeBuildRequest(
          module = nativeModule,
          abi = abi,
          variant = variant,
        )
      onStage(NativeAndroidBuildStage.COMPILE_NATIVE, "Compile native library")
      val nativeResult =
        NativeBuildExecutor(
          toolchain = nativeToolchain,
          androidApiLevel = nativeModule.androidApiLevel,
        ).execute(
          request = request,
          moduleRoot = moduleRoot,
          buildDirectory = buildDirForNative,
          processController = controller,
          onOutput = onOutput,
        )
      if (!nativeResult.success) {
        return fail(
          NativeAndroidBuildStage.COMPILE_NATIVE,
          nativeResult.message ?: "Native build failed",
        )
      }

      ClangdCompilationDatabase.write(
        outputDirectory = moduleRoot,
        commands =
          NativeCommandPlanner.planCompilation(
            sourceRoot = File(moduleRoot, "src/main"),
            sourceSet = nativeModule.targets.single().sourceSet,
            buildDirectory = buildDirForNative,
            abi = abi,
            factory = NativeCommandFactory(
              toolchain = nativeToolchain,
              androidApiLevel = nativeModule.androidApiLevel,
            ),
          ).let { it.cCommands + it.cppCommands },
      )
    }

    val allClassInputs =
      buildList {
        add(classesDirectory)
        kotlinClasspath?.let(::add)
        model.moduleRoot.resolve("libs").takeIf(File::isDirectory)?.let(::add)
      }

    onStage(NativeAndroidBuildStage.DEX, "D8")
    dexDirectory.mkdirs()
    val dexCommand =
      buildD8Command(
        d8 = requireNotNull(toolchain.d8),
        androidJar = androidJar,
        outputDirectory = dexDirectory,
        inputs = allClassInputs,
      )
    val dexResult = runCommand(dexCommand, controller, onOutput)
    if (!dexResult.success) {
      return fail(
        NativeAndroidBuildStage.DEX,
        dexResult.output.ifBlank { "D8 failed" },
      )
    }

    val dexEntries =
      dexDirectory.listFiles()
        ?.filter { it.isFile && it.name.matches(Regex("classes(\d*)\.dex")) }
        ?.sortedBy(File::getName)
        .orEmpty()
    if (dexEntries.isEmpty()) {
      return fail(NativeAndroidBuildStage.DEX, "D8 did not produce classes.dex")
    }

    onStage(NativeAndroidBuildStage.MERGE_DEX, "Merge DEX")
    val dexMap = dexEntries.associate { it.name to it }
    val dexMergedApk = File(buildDirectory, "dex-merged.apk")
    ApkEntryMerger.merge(
      inputApk = unsignedApk,
      outputApk = dexMergedApk,
      replacementEntries = dexMap,
    )

    var currentApk = dexMergedApk
    nativeModule?.let {
      val nativeTarget = it.targets.single()
      val nativeOutput =
        File(
          buildDirForNative,
          "libs/" + abi.androidAbiName + "/lib" +
            it.moduleName + if (nativeTarget.libraryType == NativeLibraryType.SHARED) ".so" else ".a",
        )
      if (nativeTarget.libraryType == NativeLibraryType.SHARED && nativeOutput.isFile) {
        onStage(NativeAndroidBuildStage.MERGE_NATIVE, "Merge native library")
        val next = File(buildDirectory, "native-merged.apk")
        NativeApkLibraryPackager.merge(
          inputApk = currentApk,
          nativeLibrary = nativeOutput,
          abi = abi,
          moduleName = it.moduleName,
          outputApk = next,
        )
        currentApk = next
      }
    }

    model.assetsDirectory?.let { assets ->
      onStage(NativeAndroidBuildStage.MERGE_ASSETS, "Merge assets")
      val next = File(buildDirectory, "assets-merged.apk")
      ApkEntryMerger.merge(
        inputApk = currentApk,
        outputApk = next,
        replacementEntries = emptyMap(),
        directory = assets,
        extraPrefix = "assets",
      )
      currentApk = next
    }

    onStage(NativeAndroidBuildStage.ZIPALIGN, "Zipalign")
    val zipalignCommand =
      NativeCommandSpec(
        executable = requireNotNull(toolchain.zipalign),
        arguments =
          listOf(
            "-P",
            "16",
            "-f",
            "4",
            currentApk.absolutePath,
            alignedApk.absolutePath,
          ),
        workingDirectory = alignedApk.parentFile,
      )
    val alignResult = runCommand(zipalignCommand, controller, onOutput)
    if (!alignResult.success) {
      return fail(
        NativeAndroidBuildStage.ZIPALIGN,
        alignResult.output.ifBlank { "zipalign failed" },
      )
    }

    onStage(NativeAndroidBuildStage.SIGN, "Sign APK")
    val signed = signApk(
      unsigned = alignedApk,
      signed = signedApk,
      keytool = requireNotNull(toolchain.keytool),
      apksigner = requireNotNull(toolchain.apksigner),
      moduleRoot = moduleRoot,
      controller = controller,
      onOutput = onOutput,
    )
    if (!signed.success) {
      return fail(
        NativeAndroidBuildStage.SIGN,
        signed.output.ifBlank { "APK signing failed" },
      )
    }

    onStage(NativeAndroidBuildStage.SUCCESS, "Native APK build completed")
    return NativeAndroidBuildResult(
      success = true,
      outputApk = signedApk,
      message = "Native APK build completed",
    )
  }

  private fun compileJavaSources(
    sources: List<File>,
    outputDirectory: File,
    classpath: List<File>,
    controller: NativeProcessController?,
    onOutput: (String) -> Unit,
    javac: File,
  ): NativeProcessResult =
    runCommand(
      NativeCommandSpec(
        executable = javac,
        arguments =
          buildList {
            add("-d")
            add(outputDirectory.absolutePath)
            add("-proc:none")
            add("-source")
            add("11")
            add("-target")
            add("11")
            add("-classpath")
            add(classpath.filter(File::isFile).joinToString(File.pathSeparator, File::getAbsolutePath))
            sources.forEach { add(it.absolutePath) }
          },
        workingDirectory = sources.firstOrNull()?.parentFile,
      ),
      controller,
      onOutput,
    )

  private fun compileJavaFile(
    source: File,
    outputDirectory: File,
    classpath: List<File>,
    controller: NativeProcessController?,
    onOutput: (String) -> Unit,
  ): NativeProcessResult =
    compileJavaSources(
      sources = listOf(source),
      outputDirectory = outputDirectory,
      classpath = classpath,
      controller = controller,
      onOutput = onOutput,
      javac = requireNotNull(AndroidBuildToolchainLocator.locate().javac),
    )

  private fun buildKotlinCommand(
    model: AndroidProjectModel,
    generatedJava: File,
    outputDirectory: File,
    classpath: List<File>,
    kotlin: File,
  ): NativeCommandSpec {
    val sources =
      (model.kotlinSources + model.javaSources +
        generatedJava.walkTopDown().filter { it.isFile && it.extension.equals("java", true) }.toList())
        .distinct()
        .sortedBy(File::getAbsolutePath)

    return NativeCommandSpec(
      executable = kotlin,
      arguments =
        buildList {
          add("-classpath")
          add(classpath.filter(File::isFile).joinToString(File.pathSeparator, File::getAbsolutePath))
          add("-d")
          add(outputDirectory.absolutePath)
          add("-jvm-target")
          add("11")
          addAll(sources.map(File::getAbsolutePath))
        },
      workingDirectory = model.moduleRoot,
    )
  }

  private fun buildD8Command(
    d8: File,
    androidJar: File,
    outputDirectory: File,
    inputs: List<File>,
  ): NativeCommandSpec =
    NativeCommandSpec(
      executable = d8,
      arguments =
        buildList {
          add("--lib")
          add(androidJar.absolutePath)
          add("--output")
          add(outputDirectory.absolutePath)
          inputs.filter(File::exists).forEach { add(it.absolutePath) }
        },
      workingDirectory = outputDirectory,
    )

  private fun buildClasspath(
    model: AndroidProjectModel,
    kotlinStdlib: File?,
  ): List<File> =
    buildList {
      add(Environment.ANDROID_JAR)
      kotlinStdlib?.let(::add)
      File(model.moduleRoot, "libs")
        .takeIf(File::isDirectory)
        ?.walkTopDown()
        ?.filter { it.isFile && it.extension.equals("jar", true) }
        ?.forEach(::add)
      File(model.moduleRoot, ".androidide/classpath")
        .takeIf(File::isDirectory)
        ?.walkTopDown()
        ?.filter { it.isFile && it.extension.equals("jar", true) }
        ?.forEach(::add)
    }

  private fun findKotlinStdlib(toolchain: AndroidBuildToolchain): File? {
    val kotlinc = toolchain.kotlinc ?: return null
    val home = kotlinc.parentFile?.parentFile ?: return null
    return listOf(
      File(home, "lib/kotlin-stdlib.jar"),
      File(home, "lib/kotlin-stdlib-jdk8.jar"),
    ).firstOrNull(File::isFile)
  }

  private fun runCommand(
    command: NativeCommandSpec,
    controller: NativeProcessController?,
    onOutput: (String) -> Unit,
  ): NativeProcessResult =
    commandExecutor(
      command,
      Environment.getEnvironment(),
      controller,
      onOutput,
    )

  private fun signApk(
    unsigned: File,
    signed: File,
    keytool: File,
    apksigner: File,
    moduleRoot: File,
    controller: NativeProcessController?,
    onOutput: (String) -> Unit,
  ): NativeProcessResult {
    val keystore = File(moduleRoot, ".androidide/debug.keystore")
    if (!keystore.isFile) {
      val generated =
        runCommand(
          NativeCommandSpec(
            executable = keytool,
            arguments =
              listOf(
                "-genkeypair",
                "-keystore", keystore.absolutePath,
                "-storepass", "android",
                "-keypass", "android",
                "-alias", "androiddebugkey",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-dname", "CN=Android Debug,O=Android,C=US",
              ),
            workingDirectory = keystore.parentFile,
          ),
          controller,
          onOutput,
        )
      if (!generated.success) return generated
    }

    signed.parentFile?.mkdirs()
    return runCommand(
      NativeCommandSpec(
        executable = apksigner,
        arguments =
          listOf(
            "sign",
            "--ks", keystore.absolutePath,
            "--ks-pass", "pass:android",
            "--key-pass", "pass:android",
            "--ks-key-alias", "androiddebugkey",
            "--out", signed.absolutePath,
            unsigned.absolutePath,
          ),
        workingDirectory = signed.parentFile,
      ),
      controller,
      onOutput,
    )
  }
}
