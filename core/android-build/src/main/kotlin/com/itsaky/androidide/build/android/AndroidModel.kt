package com.itsaky.androidide.build.android

import java.nio.file.Files
import java.nio.file.Path

data class AndroidNativeToolchain(
  val version: String,
  val compiler: Path,
  val cppCompiler: Path = compiler,
  val linker: Path,
  val sysroot: Path,
  val resourceDir: Path,
  val runtimeLibraryDir: Path,
  val runtimeSharedLibrary: Path? = null,
  val includeDirs: List<Path> = emptyList(),
  val nativeAppGlueDir: Path? = null
)

data class AndroidSdk(
  val root: Path,
  val buildToolsVersion: String,
  val compileSdk: Int,
  val nativeToolchain: AndroidNativeToolchain? = null
) {
  val buildTools: Path
    get() = root.resolve("build-tools").resolve(buildToolsVersion)

  val platform: Path
    get() = root.resolve("platforms").resolve("android-" + compileSdk)

  val aapt2: Path get() = buildTools.resolve("aapt2")
  val d8: Path get() = buildTools.resolve("d8")
  val zipalign: Path get() = buildTools.resolve("zipalign")
  val apksigner: Path get() = buildTools.resolve("apksigner")

  fun androidJar(): Path = platform.resolve("android.jar")
}

data class AndroidModule(
  val name: String,
  val rootDir: Path,
  val namespace: String,
  val applicationId: String,
  val compileSdk: Int,
  val minSdk: Int,
  val targetSdk: Int,
  val sdk: AndroidSdk,
  val compileClasspath: List<Path> = emptyList(),
  val dependencyResourceDirs: List<Path> = emptyList(),
  val dependencyNativeLibDirs: List<Path> = emptyList(),
  val dependencyAssetDirs: List<Path> = emptyList(),
  val localNativeLibDir: Path? = null,
  val localNativeIncludeDir: Path? = null,
  val javaSourceLevel: String = "11",
  val javaBytecodeLevel: String = "11",
  val cLanguageStandard: String = "c17",
  val cppLanguageStandard: String = "c++20",
  val kotlinCompilerClassLoader: ClassLoader? = null,
  val kotlinCompilerPluginClasspaths: List<Path> = emptyList(),
  val nativeBuildConfiguration: NativeBuildConfiguration =
    NativeBuildConfiguration.load(rootDir)
) {
  val manifest: Path get() = rootDir.resolve("src/main/AndroidManifest.xml")
  val sourceDir: Path get() = rootDir.resolve("src/main/java")
  val kotlinSourceDir: Path get() = rootDir.resolve("src/main/kotlin")
  val nativeSourceDir: Path get() = rootDir.resolve("src/main/cpp")
  val resourceDir: Path get() = rootDir.resolve("src/main/res")
  val assetDir: Path get() = rootDir.resolve("src/main/assets")

  val buildDir: Path get() = rootDir.resolve("build/androidide")
  val mergedResourcesDir: Path get() = buildDir.resolve("intermediates/merged-res/debug")
  val compiledResourcesDir: Path get() = buildDir.resolve("intermediates/compiled-res/debug")
  val generatedDir: Path get() = buildDir.resolve("generated/debug")
  val generatedRDir: Path get() = generatedDir.resolve("r")
  val generatedBuildConfigDir: Path = generatedDir.resolve("buildConfig")
  val classesDir: Path get() = buildDir.resolve("intermediates/classes/debug")
  val kotlinOutputJar: Path get() = buildDir.resolve("intermediates/kotlin/debug/classes.jar")
  val nativeLibDir: Path get() = buildDir.resolve("intermediates/native-libs/debug")
  val dexDir: Path get() = buildDir.resolve("intermediates/dex/debug")
  val resourcesApk: Path get() = buildDir.resolve("outputs/resources.ap_")
  val unsignedApk: Path get() = buildDir.resolve("outputs/app-debug-unsigned.apk")
  val alignedApk: Path get() = buildDir.resolve("outputs/app-debug-aligned.apk")
  val signedApk: Path get() =
    rootDir.resolve("build/outputs/apk/debug/" + name + "-debug.apk")

  val nativeLibraryName: String
    get() = nativeBuildConfiguration.libraryName
      ?: if (manifestContainsNativeActivity()) "main" else "appnative"

  val nativeActivityFunctionName: String
    get() = manifestMetadata("android.app.func_name") ?: "ANativeActivity_onCreate"

  fun manifestContainsNativeActivity(): Boolean =
    runCatching {
      Files.readString(manifest).contains("android.app.NativeActivity")
    }.getOrDefault(false)

  private fun manifestMetadata(name: String): String? =
    runCatching {
      val text = Files.readString(manifest)
      Regex(
        """android:name\s*=\s*["']$name["'][^>]*android:value\s*=\s*["']([^"']+)["']"""
      ).find(text)?.groupValues?.getOrNull(1)
    }.getOrNull()
}
