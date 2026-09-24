package com.itsaky.androidide.build

import android.content.Context
import com.android.builder.model.v2.ide.LibraryType.ANDROID_LIBRARY
import com.itsaky.androidide.build.android.AndroidSdk
import com.itsaky.androidide.build.android.NativeAndroidBuildSystem
import com.itsaky.androidide.build.android.AndroidModule as NativeAndroidModule
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.BuildResult
import com.itsaky.androidide.projects.IWorkspace
import com.itsaky.androidide.projects.android.AndroidModule
import com.itsaky.androidide.toolchain.CoreKotlinToolchainManager
import com.itsaky.androidide.toolchain.CoreToolchainManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.createDirectories
import kotlin.math.max

/**
 * AndroidIDE Pro bridge between the existing Workspace model and the Gradle-free
 * native Android build engine.
 *
 * Java/Kotlin/C/C++ implementations stay in core. Heavy compiler binaries are
 * resolved by CoreToolchainManager and are never treated as language plugins.
 */
class NativeBuildCoordinator(
  private val appContext: Context
) {

  private val coreToolchainManager by lazy {
    CoreToolchainManager(appContext)
  }

  private val coreKotlinToolchainManager by lazy {
    CoreKotlinToolchainManager(appContext)
  }

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
    if (!module.isApplication) {
      return BuildResult(
        success = false,
        message = "Native APK assembly currently requires an application module"
      )
    }

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

    val sourceDir = module.projectDir.toPath().resolve("src/main/cpp")
    val kotlinSourceDir = module.projectDir.toPath().resolve("src/main/kotlin")
    val hasNativeSources = hasNativeSources(sourceDir)
    val hasKotlinSources = hasKotlinSources(kotlinSourceDir)
    val llvmToolchain = coreToolchainManager.resolveLlvm()
    val kotlinCompilerClassLoader =
      if (hasKotlinSources) coreKotlinToolchainManager.resolveClassLoader() else null

    logger(coreToolchainManager.describe())

    if (hasKotlinSources && kotlinCompilerClassLoader == null) {
      return BuildResult(
        success = false,
        message = "Este projeto contém Kotlin, mas o Core Kotlin Toolchain APK não está instalado."
      )
    }

    if (hasNativeSources && llvmToolchain == null) {
      return BuildResult(
        success = false,
        message = "This project contains C/C++ sources, but the first-party Android LLVM " +
          "Core Toolchain Pack is not installed for this device ABI."
      )
    }

    val sdk = AndroidSdk(
      root = sdkRoot,
      buildToolsVersion = buildToolsVersion,
      compileSdk = compileSdk,
      nativeToolchain = llvmToolchain
    )

    val nativeModule = NativeAndroidModule(
      name = module.path.removePrefix(":").ifBlank { module.name },
      rootDir = module.projectDir.toPath(),
      namespace = namespace,
      applicationId = applicationId,
      compileSdk = compileSdk,
      minSdk = artifact.minSdkVersion,
      targetSdk = if (artifact.targetSdkVersionOverride > 0) {
        artifact.targetSdkVersionOverride
      } else {
        compileSdk
      },
      sdk = sdk,
      compileClasspath = module.getCompileClasspaths()
        .map { it.toPath() }
        .filter { Files.exists(it) }
        .filterNot {
          it.toAbsolutePath().normalize().startsWith(
            module.projectDir.toPath().toAbsolutePath().normalize()
          )
        },
      dependencyResourceDirs = module.libraryMap.values
        .filter { it.type == ANDROID_LIBRARY }
        .mapNotNull { it.androidLibraryData?.resFolder?.toPath() }
        .filter { Files.exists(it) },
      javaSourceLevel = module.compilerSettings.getJavaSourceVersion(),
      javaBytecodeLevel = module.compilerSettings.getJavaBytecodeVersion(),
      kotlinCompilerClassLoader = kotlinCompilerClassLoader
    )

    val keystore = ensureDebugKeystore()
      ?: return BuildResult(
        success = false,
        message = "Unable to provision the AndroidIDE Pro debug keystore"
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

  private fun hasKotlinSources(root: Path): Boolean {
    if (!Files.isDirectory(root)) return false
    return Files.walk(root).use { stream ->
      stream.anyMatch {
        Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt")
      }
    }
  }

  private fun hasNativeSources(root: Path): Boolean {
    if (!Files.isDirectory(root)) return false
    return Files.walk(root).use { stream ->
      stream.anyMatch {
        Files.isRegularFile(it) &&
          it.fileName.toString().substringAfterLast('.', "") in setOf(
            "c", "cc", "cpp", "cxx"
          )
      }
    }
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

      val encoded = NativeBuildCoordinator::class.java
        .getResourceAsStream("/debug.keystore.b64")
        ?.bufferedReader()
        ?.use { it.readText().trim() }
        ?: error("Embedded AndroidIDE Pro debug keystore resource is missing")

      val bytes = Base64.getDecoder().decode(encoded)
      Files.write(file, bytes)
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
