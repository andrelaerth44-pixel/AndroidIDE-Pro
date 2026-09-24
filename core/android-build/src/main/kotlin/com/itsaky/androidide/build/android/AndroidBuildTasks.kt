package com.itsaky.androidide.build.android

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.lang.reflect.Proxy
import java.nio.charset.StandardCharsets
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.zip.ZipOutputStream
import jdkx.tools.DiagnosticListener
import jdkx.tools.JavaFileObject
import jdkx.tools.StandardLocation
import openjdk.tools.javac.api.JavacTool
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.walk

private fun Path.ensureParent() {
  parent?.createDirectories()
}

private fun copyTree(from: Path, to: Path) {
  if (!from.exists()) return
  Files.walk(from).use { stream ->
    stream.forEach { source ->
      val relative = from.relativize(source)
      val target = to.resolve(relative.toString())
      if (Files.isDirectory(source)) {
        target.createDirectories()
      } else {
        target.ensureParent()
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
      }
    }
  }
}

private fun hasFiles(root: Path): Boolean =
  root.exists() && Files.walk(root).use { stream -> stream.anyMatch(Files::isRegularFile) }

private fun hasJvmSources(module: AndroidModule): Boolean =
  (module.sourceDir.exists() &&
    module.sourceDir.walk().any { it.isRegularFile() && it.extension == "java" }) ||
    (module.kotlinSourceDir.exists() &&
      module.kotlinSourceDir.walk().any { it.isRegularFile() && it.extension == "kt" })

private fun hasJavaBytecode(root: Path): Boolean =
  root.exists() &&
    Files.walk(root).use { stream ->
      stream.anyMatch { Files.isRegularFile(it) && it.fileName.toString().endsWith(".class") }
    }

private fun hasKotlinBytecode(jar: Path): Boolean =
  jar.exists() &&
    runCatching {
      java.util.zip.ZipFile(jar.toFile()).use { zip ->
        zip.entries().asSequence().any { !it.isDirectory && it.name.endsWith(".class") }
      }
    }.getOrDefault(false)

class MergeResourcesTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "mergeResourcesDebug"
  override val inputs = listOf(module.resourceDir) + module.dependencyResourceDirs
  override val outputs = listOf(module.mergedResourcesDir)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.mergedResourcesDir.deleteRecursively()
    module.mergedResourcesDir.createDirectories()

    copyTree(module.resourceDir, module.mergedResourcesDir)
    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "mergeResources failed") }
}

class Aapt2CompileTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "aapt2CompileDebug"
  override val inputs = listOf(module.mergedResourcesDir, module.sdk.aapt2) + module.dependencyResourceDirs
  override val outputs = listOf(module.compiledResourcesDir)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.compiledResourcesDir.deleteRecursively()
    module.compiledResourcesDir.createDirectories()

    val resourceRoots = buildList {
      add(module.mergedResourcesDir)
      addAll(module.dependencyResourceDirs)
    }

    val hasResources = resourceRoots.any(::hasFiles)
    if (!hasResources) {
      context.log("AAPT2: no resources to compile")
      return@runCatching TaskResult(true, "No Android resources")
    }

    resourceRoots.forEachIndexed { index, root ->
      if (!root.exists()) return@forEachIndexed

      val outputDir = if (index == 0) {
        module.compiledResourcesDir
      } else {
        module.compiledResourcesDir.resolve("dependency-$index")
      }

      outputDir.createDirectories()

      ProcessTools.run(
        module.sdk.aapt2,
        listOf(
          "compile",
          "--dir", root.toString(),
          "-o", outputDir.toString()
        ),
        logger = context::log
      )
    }

    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "aapt2 compile failed") }
}

class Aapt2LinkTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "aapt2LinkDebug"
  override val inputs = listOf(
    module.compiledResourcesDir,
    module.manifest,
    module.sdk.androidJar(),
    module.sdk.aapt2
  )
  override val outputs = listOf(module.resourcesApk, module.generatedRDir)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.resourcesApk.ensureParent()
    module.generatedRDir.deleteRecursively()
    module.generatedRDir.createDirectories()

    val compiledFiles = buildList {
      val dependencyDirs = Files.list(module.compiledResourcesDir).use { stream ->
        stream
          .filter { Files.isDirectory(it) }
          .filter { it.fileName.toString().startsWith("dependency-") }
          .sorted { a, b -> a.fileName.toString().compareTo(b.fileName.toString()) }
          .toList()
      }

      dependencyDirs.forEach { directory ->
        directory.walk()
          .filter { it.isRegularFile() && it.extension == "flat" }
          .sortedBy { it.toString() }
          .forEach(::add)
      }

      module.compiledResourcesDir
        .walk()
        .filter { it.parent == module.compiledResourcesDir }
        .filter { it.isRegularFile() && it.extension == "flat" }
        .sortedBy { it.toString() }
        .forEach(::add)
    }

    ProcessTools.run(
      module.sdk.aapt2,
      listOf(
        "link",
        "-o", module.resourcesApk.toString(),
        "--manifest", module.manifest.toString(),
        "--java", module.generatedRDir.toString(),
        "--custom-package", module.namespace,
        "--min-sdk-version", module.minSdk.toString(),
        "--target-sdk-version", module.targetSdk.toString(),
        "-I", module.sdk.androidJar().toString(),
        "--auto-add-overlay"
      ) + compiledFiles.map(Path::toString),
      logger = context::log
    )

    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "aapt2 link failed") }
}

class GenerateBuildConfigTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "generateBuildConfigDebug"

  private val file = module.generatedBuildConfigDir
    .resolve(module.namespace.replace('.', '/'))
    .resolve("BuildConfig.java")

  override val inputs: List<Path>
    get() = emptyList()

  override val outputs: List<Path>
    get() = if (hasJvmSources(module)) listOf(file) else emptyList()

  override fun execute(context: BuildContext): TaskResult = runCatching {
    if (!hasJvmSources(module)) {
      file.deleteIfExists()
      return@runCatching TaskResult(true, "No Java/Kotlin sources; BuildConfig not generated")
    }

    file.ensureParent()

    val content = buildString {
      appendLine("package " + module.namespace + ";")
      appendLine()
      appendLine("public final class BuildConfig {")
      appendLine("  public static final boolean DEBUG = true;")
      appendLine("  public static final String APPLICATION_ID = \"" + module.applicationId + "\";")
      appendLine("  public static final String BUILD_TYPE = \"debug\";")
      appendLine("  public static final int VERSION_CODE = 1;")
      appendLine("  public static final String VERSION_NAME = \"1.0\";")
      appendLine("  private BuildConfig() {}")
      appendLine("}")
    }

    Files.writeString(file, content)
    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "BuildConfig generation failed") }
}

class CompileJavaTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "compileJavaDebug"

  override val inputs: List<Path>
    get() = buildList {
      add(module.sourceDir)
      add(module.kotlinSourceDir)
      add(module.generatedRDir)
      add(module.generatedBuildConfigDir)
      add(module.sdk.androidJar())
      addAll(module.compileClasspath)
    }

  override val outputs = listOf(module.classesDir)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.classesDir.deleteRecursively()
    module.classesDir.createDirectories()

    if (!hasJvmSources(module)) {
      Files.writeString(module.classesDir.resolve(".jvm-stamp"), "no-jvm-sources\n")
      return@runCatching TaskResult(true, "No Java/Kotlin sources")
    }

    val sources = buildList {
      if (module.sourceDir.exists()) {
        module.sourceDir.walk().filter { it.extension == "java" }.forEach(::add)
      }
      if (module.generatedRDir.exists()) {
        module.generatedRDir.walk().filter { it.extension == "java" }.forEach(::add)
      }
      if (module.generatedBuildConfigDir.exists()) {
        module.generatedBuildConfigDir.walk().filter { it.extension == "java" }.forEach(::add)
      }
    }

    if (sources.isEmpty()) return@runCatching TaskResult(true, "No Java sources")

    val compiler = JavacTool.create()
    val diagnostics = DiagnosticListener<JavaFileObject> { diagnostic ->
      val source = diagnostic.source?.name ?: "<unknown>"
      context.log(
        "javac " + diagnostic.kind + ": " + source + ":" +
          (diagnostic.lineNumber.takeIf { it > 0 } ?: 0) +
          ": " + diagnostic.getMessage(Locale.ROOT)
      )
    }

    val fileManager = compiler.getStandardFileManager(
      diagnostics,
      Locale.ROOT,
      StandardCharsets.UTF_8
    )

    try {
      fileManager.setLocation(
        StandardLocation.CLASS_PATH,
        buildList {
          add(module.sdk.androidJar().toFile())
          add(module.classesDir.toFile())
          addAll(module.compileClasspath.map(Path::toFile))
        }
      )
      fileManager.setLocation(
        StandardLocation.SOURCE_PATH,
        listOf(
          module.sourceDir.toFile(),
          module.generatedRDir.toFile(),
          module.generatedBuildConfigDir.toFile()
        ).filter { it.exists() }
      )
      fileManager.setLocation(
        StandardLocation.CLASS_OUTPUT,
        listOf(module.classesDir.toFile())
      )

      val units = fileManager.getJavaFileObjectsFromFiles(
        sources.map(Path::toFile)
      )

      val task = compiler.getTask(
        null,
        fileManager,
        diagnostics,
        listOf(
          "-source", module.javaSourceLevel,
          "-target", module.javaBytecodeLevel,
          "-proc:none",
          "-g"
        ),
        null,
        units
      )

      require(task.call() == true) {
        "Embedded Java compiler failed"
      }
    } finally {
      fileManager.close()
    }

    Files.writeString(module.classesDir.resolve(".jvm-stamp"), "compiled-java\n")
    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "javac failed") }
}

class KotlinCompileTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "compileKotlinDebug"

  override val inputs: List<Path>
    get() = buildList {
      add(module.sourceDir)
      add(module.kotlinSourceDir)
      add(module.classesDir)
      add(module.sdk.androidJar())
      module.compileClasspath.forEach(::add)
      module.kotlinCompilerClassLoader?.let {
        // The compiler lives in a first-party APK and is represented by its
        // package classloader rather than a writable code directory.
      }
    }

  override val outputs = listOf(module.kotlinOutputJar)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.kotlinOutputJar.ensureParent()
    Files.deleteIfExists(module.kotlinOutputJar)

    val sources = buildList {
      if (module.kotlinSourceDir.exists()) {
        module.kotlinSourceDir.walk().filter { it.extension == "kt" }.forEach(::add)
      }
      if (module.sourceDir.exists()) {
        module.sourceDir.walk().filter { it.extension == "kt" }.forEach(::add)
      }
    }.distinct()

    if (sources.isEmpty()) {
      ZipOutputStream(module.kotlinOutputJar.outputStream()).use { }
      return@runCatching TaskResult(true, "No Kotlin sources")
    }

    val loader = module.kotlinCompilerClassLoader
      ?: return@runCatching TaskResult(
        false,
        "Kotlin sources require the first-party Core Kotlin Toolchain Pack."
      )

    module.classesDir.createDirectories()

    val argumentsClass = Class.forName(
      "org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments",
      true,
      loader
    )
    val arguments = argumentsClass.getDeclaredConstructor().newInstance()

    setCompilerArgument(arguments, "setFreeArgs", sources.map(Path::toString).toMutableList())
    setCompilerArgument(arguments, "setDestination", module.kotlinOutputJar.toString())
    setCompilerArgument(
      arguments,
      "setClasspath",
      buildList {
        add(module.sdk.androidJar())
        add(module.classesDir)
        addAll(module.compileClasspath)
      }.joinToString(File.pathSeparator)
    )
    setCompilerArgument(arguments, "setIncludeRuntime", false)
    setCompilerArgument(arguments, "setNoReflect", true)
    setCompilerArgument(arguments, "setJvmTarget", module.javaBytecodeLevel)
    setCompilerArgument(
      arguments,
      "setModuleName",
      module.name.replace(Regex("[^A-Za-z0-9_]"), "_")
    )
    setCompilerArgument(arguments, "setSkipRuntimeVersionCheck", true)

    val messageCollectorClass = Class.forName(
      "org.jetbrains.kotlin.cli.common.messages.MessageCollector",
      true,
      loader
    )
    var compilerErrors = false

    val collector = Proxy.newProxyInstance(
      loader,
      arrayOf(messageCollectorClass)
    ) { _, method, args ->
      when (method.name) {
        "report" -> {
          val severity = args?.getOrNull(0)?.toString() ?: "MESSAGE"
          val message = args?.getOrNull(1)?.toString() ?: ""
          if (severity.contains("ERROR", ignoreCase = true)) {
            compilerErrors = true
          }
          context.log("kotlinc " + severity + ": " + message)
          null
        }
        "hasErrors" -> compilerErrors
        "clear" -> null
        else -> defaultReflectionValue(method.returnType)
      }
    }

    val servicesClass = Class.forName(
      "org.jetbrains.kotlin.cli.common.Services",
      true,
      loader
    )
    val services = servicesClass.getField("EMPTY").get(null)

    val compilerClass = Class.forName(
      "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
      true,
      loader
    )
    val compiler = compilerClass.getDeclaredConstructor().newInstance()

    val exec = compilerClass.methods.firstOrNull { method ->
      method.name == "exec" &&
        method.parameterTypes.size == 3 &&
        method.parameterTypes[0].isAssignableFrom(messageCollectorClass) &&
        method.parameterTypes[1].isAssignableFrom(servicesClass) &&
        method.parameterTypes[2].isAssignableFrom(argumentsClass)
    } ?: error("Kotlin compiler exec method not found")

    val exitCode = exec.invoke(
      compiler,
      collector,
      services,
      arguments
    )

    val exitName = (exitCode as? Enum<*>)?.name ?: exitCode.toString()
    check(!compilerErrors && exitName == "OK") {
      "Kotlin compiler failed with " + exitName
    }

    TaskResult(true)
  }.getOrElse { throwable ->
    val cause = generateSequence(throwable) { it.cause }.lastOrNull() ?: throwable
    TaskResult(false, cause.message ?: "Kotlin compilation failed")
  }

  private fun setCompilerArgument(
    arguments: Any,
    setterName: String,
    value: Any
  ) {
    val setter = arguments.javaClass.methods.firstOrNull {
      it.name == setterName && it.parameterTypes.size == 1
    } ?: error("Kotlin compiler argument setter not found: " + setterName)

    setter.invoke(arguments, value)
  }

  private fun defaultReflectionValue(returnType: Class<*>): Any? =
    when (returnType) {
      Boolean::class.javaPrimitiveType -> false
      Int::class.javaPrimitiveType -> 0
      Long::class.javaPrimitiveType -> 0L
      Float::class.javaPrimitiveType -> 0f
      Double::class.javaPrimitiveType -> 0.0
      else -> null
    }
}

class DexBuilderTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "dexBuilderDebug"
  override val inputs = listOf(
    module.classesDir,
    module.kotlinOutputJar,
    module.sdk.d8,
    module.sdk.androidJar()
  ) + module.compileClasspath
  override val outputs = listOf(module.dexDir)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.dexDir.deleteRecursively()
    module.dexDir.createDirectories()

    val hasProgramBytecode =
      hasJavaBytecode(module.classesDir) ||
        hasKotlinBytecode(module.kotlinOutputJar)

    if (!hasProgramBytecode) {
      context.log("D8: no Java/Kotlin bytecode; skipping dex")
      Files.writeString(module.dexDir.resolve(".dex-stamp"), "no-jvm-bytecode\n")
      return@runCatching TaskResult(true, "No JVM bytecode")
    }

    Files.deleteIfExists(module.dexDir.resolve(".dex-stamp"))

    ProcessTools.run(
      module.sdk.d8,
      listOf(
        "--lib", module.sdk.androidJar().toString(),
        "--min-api", module.minSdk.toString(),
        "--output", module.dexDir.toString(),
        module.classesDir.toString(),
        module.kotlinOutputJar.toString()
      ) + module.compileClasspath.map(Path::toString),
      logger = context::log
    )

    Files.writeString(module.dexDir.resolve(".dex-stamp"), "d8-complete\n")
    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "D8 failed") }
}

class PackageApkTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "packageApkDebug"

  override val inputs: List<Path>
    get() = buildList {
      add(module.resourcesApk)
      add(module.dexDir)
      add(module.nativeLibDir)
      module.sdk.nativeToolchain?.runtimeSharedLibrary?.let(::add)
    }

  override val outputs = listOf(module.unsignedApk)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.unsignedApk.ensureParent()

    val seen = HashSet<String>()
    java.util.zip.ZipFile(module.resourcesApk.toFile()).use { input ->
      java.util.zip.ZipOutputStream(module.unsignedApk.outputStream()).use { output ->
        input.entries().asSequence().forEach { entry ->
          if (!seen.add(entry.name)) return@forEach

          val target = java.util.zip.ZipEntry(entry.name)
          output.putNextEntry(target)
          input.getInputStream(entry).use { it.copyTo(output) }
          output.closeEntry()
        }

        if (Files.exists(module.dexDir)) {
          Files.list(module.dexDir).use { stream ->
            stream
              .filter { Files.isRegularFile(it) }
              .filter { it.fileName.toString().matches(Regex("classes(\\\\d+)?\\\\.dex")) }
              .sorted { a, b -> a.fileName.toString().compareTo(b.fileName.toString()) }
              .forEach { dex ->
                val name = dex.fileName.toString()
                if (!seen.add(name)) return@forEach
                output.putNextEntry(java.util.zip.ZipEntry(name))
                dex.inputStream().use { it.copyTo(output) }
                output.closeEntry()
              }
          }
        }

        val nativeDir = module.nativeLibDir.resolve("arm64-v8a")
        if (Files.isDirectory(nativeDir)) {
          Files.list(nativeDir).use { stream ->
            stream
              .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".so") }
              .sorted { a, b -> a.fileName.toString().compareTo(b.fileName.toString()) }
              .forEach { library ->
                val name = "lib/arm64-v8a/" + library.fileName
                if (!seen.add(name)) return@forEach
                output.putNextEntry(java.util.zip.ZipEntry(name))
                library.inputStream().use { it.copyTo(output) }
                output.closeEntry()
              }
          }
        }

        val runtime = module.sdk.nativeToolchain?.runtimeSharedLibrary
        if (runtime != null && Files.isRegularFile(runtime)) {
          val name = "lib/arm64-v8a/libc++_shared.so"
          if (seen.add(name)) {
            output.putNextEntry(java.util.zip.ZipEntry(name))
            runtime.inputStream().use { it.copyTo(output) }
            output.closeEntry()
          }
        }

        if (module.assetDir.exists()) {
          module.assetDir.walk()
            .filter { it.isRegularFile() }
            .forEach { asset ->
              val name = "assets/" + module.assetDir
                .relativize(asset)
                .toString()
                .replace('\\', '/')

              if (!seen.add(name)) return@forEach

              output.putNextEntry(java.util.zip.ZipEntry(name))
              asset.inputStream().use { it.copyTo(output) }
              output.closeEntry()
            }
        }
      }
    }

    TaskResult(true)
  }.getOrElse {
    TaskResult(false, it.message ?: "APK packaging failed")
  }
}

class ZipalignTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "zipalignDebug"
  override val inputs = listOf(module.unsignedApk, module.sdk.zipalign)
  override val outputs = listOf(module.alignedApk)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.alignedApk.ensureParent()
    ProcessTools.run(
      module.sdk.zipalign,
      listOf(
        "-f", "4",
        module.unsignedApk.toString(),
        module.alignedApk.toString()
      ),
      logger = context::log
    )
    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "zipalign failed") }
}

class SignApkTask(
  private val module: AndroidModule,
  private val keystore: Path,
  private val storePassword: String,
  private val keyAlias: String,
  private val keyPassword: String
) : BuildTask {
  override val id = "signDebug"
  override val inputs = listOf(module.alignedApk, module.sdk.apksigner, keystore)
  override val outputs = listOf(module.signedApk)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.signedApk.ensureParent()

    ProcessTools.run(
      module.sdk.apksigner,
      listOf(
        "sign",
        "--ks", keystore.toString(),
        "--ks-pass", "pass:" + storePassword,
        "--ks-key-alias", keyAlias,
        "--key-pass", "pass:" + keyPassword,
        "--out", module.signedApk.toString(),
        module.alignedApk.toString()
      ),
      logger = context::log
    )

    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "apksigner failed") }
}
