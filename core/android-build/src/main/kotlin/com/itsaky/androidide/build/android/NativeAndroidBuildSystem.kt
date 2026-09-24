package com.itsaky.androidide.build.android

import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.BuildResult
import com.itsaky.androidide.build.api.BuildSystem
import com.itsaky.androidide.build.engine.DefaultBuildContext
import com.itsaky.androidide.build.engine.TaskGraph
import com.itsaky.androidide.language.NativeLanguageScanner
import java.nio.file.Files
import java.nio.file.Path

class NativeAndroidBuildSystem(
  private val module: AndroidModule,
  private val debugKeystore: Path,
  private val debugStorePassword: String = "android",
  private val debugKeyAlias: String = "androiddebugkey",
  private val debugKeyPassword: String = "android",
  private val logger: (String) -> Unit = ::println
) : BuildSystem {

  override fun assemble(request: BuildRequest): BuildResult {
    require(request.moduleName == module.name) {
      "Unknown module '" + request.moduleName +
        "', expected '" + module.name + "'"
    }
    require(request.variant == "debug") {
      "NativeAndroidBuildSystem currently supports only debug"
    }

    val sourceReport = NativeLanguageScanner.scan(
      listOf(module.sourceDir, module.kotlinSourceDir, module.nativeSourceDir)
    )

    logger("BUILT-IN LANGUAGES: " +
      sourceReport.filesByLanguage.keys.joinToString { it.displayName })

    if (sourceReport.unknownFiles.isNotEmpty()) {
      return BuildResult(
        success = false,
        message = "Unsupported source files in native build: " +
          sourceReport.unknownFiles.joinToString { it.toString() }
      )
    }

    validateToolchain()

    val merge = MergeResourcesTask(module)
    val compileRes = Aapt2CompileTask(module)
    val link = Aapt2LinkTask(module)
    val buildConfig = GenerateBuildConfigTask(module)
    val javac = CompileJavaTask(module)
    val kotlinc = KotlinCompileTask(module)
    val native = CompileNativeTask(module)
    val dex = DexBuilderTask(module)
    val packageApk = PackageApkTask(module)
    val align = ZipalignTask(module)
    val sign = SignApkTask(
      module = module,
      keystore = debugKeystore,
      storePassword = debugStorePassword,
      keyAlias = debugKeyAlias,
      keyPassword = debugKeyPassword
    )

    val graph = TaskGraph()
      .add(merge)
      .add(compileRes)
      .add(link)
      .add(buildConfig)
      .add(javac)
      .add(kotlinc)
      .add(native)
      .add(dex)
      .add(packageApk)
      .add(align)
      .add(sign)
      .dependsOn(compileRes.id, merge.id)
      .dependsOn(link.id, compileRes.id)
      .dependsOn(javac.id, link.id)
      .dependsOn(javac.id, buildConfig.id)
      .dependsOn(kotlinc.id, javac.id)
      .dependsOn(native.id, link.id)
      .dependsOn(dex.id, javac.id)
      .dependsOn(dex.id, kotlinc.id)
      .dependsOn(packageApk.id, link.id)
      .dependsOn(packageApk.id, dex.id)
      .dependsOn(packageApk.id, native.id)
      .dependsOn(align.id, packageApk.id)
      .dependsOn(sign.id, align.id)

    val result = graph.execute(
      DefaultBuildContext(
        cacheRoot = module.buildDir.resolve(".cache"),
        logger = logger
      )
    )

    return if (result.success) {
      BuildResult(
        success = true,
        outputApk = module.signedApk,
        message = "assembleDebug completed with built-in language pipeline"
      )
    } else {
      BuildResult(
        success = false,
        message = result.message
      )
    }
  }

  private fun validateToolchain() {
    listOf(
      module.sdk.aapt2,
      module.sdk.d8,
      module.sdk.zipalign,
      module.sdk.apksigner,
      module.sdk.androidJar(),
      module.manifest,
      debugKeystore
    ).forEach {
      check(Files.exists(it)) {
        "Missing required build input/tool: " + it
      }
    }
  }
}
