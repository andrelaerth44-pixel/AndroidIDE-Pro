package com.itsaky.androidide.native.build

import com.itsaky.androidide.utils.Environment
import java.io.File

data class AndroidBuildToolchain(
  val javac: File?,
  val kotlinc: File?,
  val d8: File?,
  val zipalign: File?,
  val apksigner: File?,
  val keytool: File?,
  val adb: File?,
) {
  val javaReady: Boolean get() = javac?.isFile == true
  val kotlinReady: Boolean get() = kotlinc?.isFile == true
  val dexReady: Boolean get() = d8?.isFile == true
  val packageReady: Boolean get() = zipalign?.isFile == true && apksigner?.isFile == true
}

object AndroidBuildToolchainLocator {

  fun locate(): AndroidBuildToolchain {
    val sdk = runCatching { Environment.ANDROID_HOME }.getOrNull()
    val jdk = runCatching { Environment.JAVA_HOME }.getOrNull()
    val prefix = runCatching { Environment.PREFIX }.getOrNull()

    val javac =
      listOfNotNull(
        jdk?.let { File(it, "bin/javac") },
        prefix?.let { File(it, "opt/openjdk/bin/javac") },
      ).firstOrNull(File::isFile)

    val keytool =
      listOfNotNull(
        jdk?.let { File(it, "bin/keytool") },
        prefix?.let { File(it, "opt/openjdk/bin/keytool") },
      ).firstOrNull(File::isFile)

    val kotlinRoots =
      listOfNotNull(
        System.getenv("ANDROIDIDE_KOTLIN_HOME")?.takeIf(String::isNotBlank)?.let(::File),
        System.getenv("KOTLIN_HOME")?.takeIf(String::isNotBlank)?.let(::File),
        prefix?.let { File(it, "opt/kotlinc") },
        prefix?.let { File(it, "opt/kotlin") },
      )

    val kotlinc =
      kotlinRoots
        .asSequence()
        .map { File(it, "bin/kotlinc") }
        .firstOrNull(File::isFile)

    val buildTools =
      sdk?.let { File(it, "build-tools") }
        ?.takeIf(File::isDirectory)
        ?.listFiles()
        ?.filter(File::isDirectory)
        ?.sortedByDescending(File::getName)
        .orEmpty()

    fun buildTool(name: String): File? =
      buildTools
        .asSequence()
        .map { File(it, name) }
        .firstOrNull(File::isFile)

    val d8 =
      listOfNotNull(
        buildTool("d8"),
        sdk?.let { File(it, "build-tools/d8") },
      ).firstOrNull(File::isFile)

    val zipalign = buildTool("zipalign")
    val apksigner = buildTool("apksigner")
    val adb =
      listOfNotNull(
        sdk?.let { File(it, "platform-tools/adb") },
        prefix?.let { File(it, "bin/adb") },
      ).firstOrNull(File::isFile)

    return AndroidBuildToolchain(
      javac = javac,
      kotlinc = kotlinc,
      d8 = d8,
      zipalign = zipalign,
      apksigner = apksigner,
      keytool = keytool,
      adb = adb,
    )
  }
}
