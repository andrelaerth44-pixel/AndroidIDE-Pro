package com.itsaky.androidide.build.android

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
import jdkx.tools.DiagnosticListener
import jdkx.tools.JavaFileObject
import jdkx.tools.StandardLocation
import openjdk.tools.javac.api.JavacTool
import java.util.zip.ZipOutputStream
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.Services
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import kotlin.io.path.createDirectories
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

    val hasResources = resourceRoots.any { root ->
      if (!root.exists()) {
        false
      } else {
        Files.walk(root).use { stream ->
          stream.anyMatch(Files::isRegularFile)
        }
      }
    }

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

    val compiledFiles = module.compiledResourcesDir
      .walk()
      .filter { it.isRegularFile() && it.extension == "flat" }
      .toList()

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
  override val inputs = emptyList<Path>()

  private val file = module.generatedBuildConfigDir
    .resolve(module.namespace.replace('.', '/'))
    .resolve("BuildConfig.java")

  override val outputs = listOf(file)

  override fun execute(context: BuildContext): TaskResult = runCatching {
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
      add(module.generatedRDir)
      add(module.generatedBuildConfigDir)
      add(module.sdk.androidJar())
      addAll(module.compileClasspath)
    }

  override val outputs = listOf(module.classesDir)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.classesDir.deleteRecursively()
    module.classesDir.createDirectories()

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

    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "javac failed") }
}

class KotlinCompileTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "compileKotlinDebug"

  override val inputs: List<Path>
    get() = listOf(
      module.sourceDir,
      module.kotlinSourceDir,
      module.classesDir,
      module.sdk.androidJar()
    ) + module.compileClasspath

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

    module.classesDir.createDirectories()

    val arguments = K2JVMCompilerArguments().apply {
      freeArgs = sources.map(Path::toString).toMutableList()
      destination = module.kotlinOutputJar.toString()
      classpath = buildList {
        add(module.sdk.androidJar())
        add(module.classesDir)
        addAll(module.compileClasspath)
      }.joinToString(File.pathSeparator)
      includeRuntime = false
      noReflect = true
      jvmTarget = module.javaBytecodeLevel
      moduleName = module.name.replace(Regex("[^A-Za-z0-9_]"), "_")
      skipRuntimeVersionCheck = true
    }

    val collector = PrintingMessageCollector(
      System.out,
      MessageRenderer.PLAIN_RELATIVE_PATHS,
      false
    )

    val exitCode = K2JVMCompiler().exec(
      collector,
      Services.EMPTY,
      arguments
    )

    require(exitCode == ExitCode.OK) {
      "Kotlin compiler failed: " + exitCode
    }

    TaskResult(true)
  }.getOrElse {
    TaskResult(false, it.message ?: "Kotlin compilation failed")
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
  override val outputs = listOf(module.dexDir.resolve("classes.dex"))

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.dexDir.deleteRecursively()
    module.dexDir.createDirectories()

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

    TaskResult(true)
  }.getOrElse { TaskResult(false, it.message ?: "D8 failed") }
}

class PackageApkTask(
  private val module: AndroidModule
) : BuildTask {
  override val id = "packageApkDebug"
  override val inputs = listOf(
    module.resourcesApk,
    module.dexDir.resolve("classes.dex"),
    module.nativeLibDir
  )
  override val outputs = listOf(module.unsignedApk)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.unsignedApk.ensureParent()

    val nativeLib = module.nativeLibDir.resolve("arm64-v8a/libappnative.so")
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

        if (seen.add("classes.dex")) {
          output.putNextEntry(java.util.zip.ZipEntry("classes.dex"))
          module.dexDir.resolve("classes.dex").inputStream().use { it.copyTo(output) }
          output.closeEntry()
        }

        if (Files.exists(nativeLib)) {
          val name = "lib/arm64-v8a/libappnative.so"
          if (seen.add(name)) {
            output.putNextEntry(java.util.zip.ZipEntry(name))
            nativeLib.inputStream().use { it.copyTo(output) }
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
