package com.itsaky.androidide.native.build

import java.io.File
import java.util.regex.Pattern

data class AndroidProjectModel(
  val moduleRoot: File,
  val manifest: File,
  val resourceDirectory: File,
  val assetsDirectory: File?,
  val javaSources: List<File>,
  val kotlinSources: List<File>,
  val applicationId: String,
  val minSdk: Int = 28,
  val targetSdk: Int = 28,
) {
  val hasKotlin: Boolean get() = kotlinSources.isNotEmpty()
  val hasJava: Boolean get() = javaSources.isNotEmpty()
  val hasNativeSources: Boolean
    get() =
      moduleRoot.resolve("src/main").walkTopDown().any {
        it.isFile && it.extension.lowercase() in setOf("c", "cc", "cpp", "cxx")
      }
}

object AndroidProjectModelLoader {

  fun load(moduleRoot: File): AndroidProjectModel? {
    if (!moduleRoot.isDirectory) return null

    val main = File(moduleRoot, "src/main")
    val manifest = File(main, "AndroidManifest.xml")
    val res = File(main, "res")

    if (!manifest.isFile || !res.isDirectory) return null

    val manifestText = manifest.readText()
    val applicationId =
      PACKAGE_PATTERN.matcher(manifestText).run {
        if (!find()) return null
        group(1)
      }

    val nativeConfig = NativeProjectConfigStore.load(moduleRoot)
    val minSdk =
      nativeConfig?.minSdk
        ?: SDK_PATTERN.matcher(manifestText).run {
          if (find()) group(1).toIntOrNull() else null
        }
        ?: 28
    val targetSdk =
      nativeConfig?.targetSdk
        ?: TARGET_SDK_PATTERN.matcher(manifestText).run {
          if (find()) group(1).toIntOrNull() else null
        }
        ?: minSdk

    val javaRoot = File(main, "java")
    val kotlinRoot = File(main, "kotlin")
    val javaSources =
      if (javaRoot.isDirectory) {
        javaRoot.walkTopDown()
          .filter { it.isFile && it.extension.equals("java", true) }
          .sortedBy(File::getAbsolutePath)
          .toList()
      } else {
        emptyList()
      }

    val kotlinSources =
      if (kotlinRoot.isDirectory) {
        kotlinRoot.walkTopDown()
          .filter { it.isFile && it.extension.equals("kt", true) }
          .sortedBy(File::getAbsolutePath)
          .toList()
      } else {
        emptyList()
      }

    return AndroidProjectModel(
      moduleRoot = moduleRoot,
      manifest = manifest,
      resourceDirectory = res,
      assetsDirectory = File(main, "assets").takeIf(File::isDirectory),
      javaSources = javaSources,
      kotlinSources = kotlinSources,
      applicationId = applicationId,
    )
  }

  private val PACKAGE_PATTERN =
    Pattern.compile("\\bpackage\\s*=\\s*[\"']([^\"']+)[\"']")
}
