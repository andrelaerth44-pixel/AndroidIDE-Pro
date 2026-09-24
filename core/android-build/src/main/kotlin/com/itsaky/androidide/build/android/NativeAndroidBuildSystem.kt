package com.itsaky.androidide.build.android

import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.BuildResult
import com.itsaky.androidide.build.api.BuildSystem
import com.itsaky.androidide.build.engine.DefaultBuildContext
import com.itsaky.androidide.build.engine.TaskGraph
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

    validateToolchain()

    val merge = MergeResourcesTask(module)
    val compileRes = Aapt2CompileTask(module)
    val link = Aapt2LinkTask(module)
    val buildConfig = GenerateBuildConfigTask(module)
    val javac = CompileJavaTask(module)
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
      .add(dex)
      .add(packageApk)
      .add(align)
      .add(sign)
      .dependsOn(compileRes.id, merge.id)
      .dependsOn(link.id, compileRes.id)
      .dependsOn(javac.id, link.id)
      .dependsOn(javac.id, buildConfig.id)
      .dependsOn(dex.id, javac.id)
      .dependsOn(packageApk.id, link.id)
      .dependsOn(packageApk.id, dex.id)
      .dependsOn(align.id, packageApk.id)
      .dependsOn(sign.id, align.id)

    val result = graph.execute(DefaultBuildContext(logger))

    return if (result.success) {
      BuildResult(
        success = true,
        outputApk = module.signedApk,
        message = "assembleDebug completed"
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
