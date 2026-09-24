package com.itsaky.androidide.build

import android.content.Context
import com.itsaky.androidide.projects.IWorkspace
import com.itsaky.androidide.projects.android.AndroidModule
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.BuildResult
import com.itsaky.androidide.build.android.AndroidSdk
import com.itsaky.androidide.build.android.NativeAndroidBuildSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * AndroidIDE Pro bridge between the existing Workspace model and the Gradle-free
 * native Android build engine.
 *
 * This class deliberately lives in the app layer: the native build engine remains
 * independent from the IDE project model and Android Context.
 */
class NativeBuildCoordinator(
  private val appContext: Context
) {

  fun assembleDebug(
    workspace: IWorkspace,
    modulePath: String,
    logger: (String) -> Unit = ::println
  ): BuildResult {
    val module = workspace.androidProjects()
      .firstOrNull { it.path == modulePath }
      ?: return BuildResult(
        success = false,
        message = "Android module not found: " + modulePath
      )

    return assembleDebug(module, logger)
  }

  fun assembleDebug(
    module: AndroidModule,
    logger: (String) -> Unit = ::println
  ): BuildResult {
    val variant = module.getVariant("debug")
      ?: return BuildResult(
        success = false,
        message = "Debug variant not available for " + module.path
      )

    val artifact = variant.mainArtifact
    val namespace = module.namespace
      ?: return BuildResult(false, message = "Module has no namespace")

    val applicationId = artifact.applicationId ?: namespace
    val platformDir = module.getPlatformDir()
      ?: return BuildResult(false, message = "Compile SDK platform was not found")

    val compileSdk = platformDir.name
      .removePrefix("android-")
      .toIntOrNull()
      ?: return BuildResult(false, message = "Invalid platform directory: " + platformDir)

    val sdkRoot = platformDir.parentFile?.parentFile?.toPath()
      ?: return BuildResult(false, message = "Unable to determine Android SDK root")

    val buildToolsVersion = findBuildToolsVersion(
      sdkRoot.resolve("build-tools")
    ) ?: return BuildResult(
      false,
      message = "No Android Build Tools installation was found"
    )

    val sdk = AndroidSdk(
      root = sdkRoot,
      buildToolsVersion = buildToolsVersion,
      compileSdk = compileSdk
    )

    val nativeModule = com.itsaky.androidide.build.android.AndroidModule(
      name = module.path.removePrefix(":").ifBlank { module.name },
      rootDir = module.projectDir.toPath(),
      namespace = namespace,
      applicationId = applicationId,
      compileSdk = compileSdk,
      minSdk = artifact.minSdkVersion,
      targetSdk = if (artifact.targetSdkVersionOverride > 0) artifact.targetSdkVersionOverride else compileSdk,
      sdk = sdk
    )

    val keystore = ensureDebugKeystore()
      ?: return BuildResult(
        success = false,
        message = "Unable to create or locate the AndroidIDE Pro debug keystore"
      )

    return NativeAndroidBuildSystem(
      module = nativeModule,
      debugKeystore = keystore,
      logger = logger
    ).assemble(
      BuildRequest(
        moduleName = nativeModule.name,
        variant = "debug"
      )
    )
  }

  private fun findBuildToolsVersion(root: Path): String? {
    if (!Files.isDirectory(root)) return null

    return Files.list(root).use { stream ->
      stream
        .filter { Files.isDirectory(it) }
        .map { it.fileName.toString() }
        .maxWithOrNull(BuildToolsVersionComparator)
    }
  }

  private fun ensureDebugKeystore(): Path? {
    val file = appContext.filesDir.toPath()
      .resolve("keys")
      .resolve("debug.keystore")

    if (Files.exists(file)) return file

    return runCatching {
      file.parent.createDirectories()

      val keytool = Path.of(
        System.getProperty("java.home"),
        "bin",
        "keytool"
      )

      check(Files.exists(keytool)) {
        "keytool not found at " + keytool
      }

      ProcessBuilder(
        keytool.toString(),
        "-genkeypair",
        "-v",
        "-keystore", file.toString(),
        "-storepass", "android",
        "-alias", "androiddebugkey",
        "-keypass", "android",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=Android Debug,O=Android,C=US"
      )
        .redirectErrorStream(true)
        .start()
        .also { process ->
          process.inputStream.bufferedReader().use { it.readText() }
          check(process.waitFor() == 0) {
            "keytool failed to create debug keystore"
          }
        }

      file
    }.getOrNull()
  }

  private object BuildToolsVersionComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int {
      val ap = versionParts(a)
      val bp = versionParts(b)

      val size = max(ap.size, bp.size)
      for (i in 0 until size) {
        val av = ap.getOrElse(i) { 0 }
        val bv = bp.getOrElse(i) { 0 }
        if (av != bv) return av.compareTo(bv)
      }

      return a.compareTo(b)
    }

    private fun versionParts(version: String): List<Int> =
      version.split('.', '-', '_').map { token ->
        token.takeWhile(Char::isDigit).toIntOrNull() ?: 0
      }
  }
}
