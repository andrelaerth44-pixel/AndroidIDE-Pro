package com.itsaky.androidide.build.android

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

data class NativeProjectDescriptor(
  val rootDir: Path,
  val namespace: String,
  val applicationId: String,
  val compileSdk: Int,
  val minSdk: Int,
  val targetSdk: Int,
  val sdkRoot: Path,
  val versionCode: Int = 1,
  val versionName: String = "1.0",
  val nativeActivity: Boolean = false,
  val nativeLibraryName: String? = null
) {

  companion object {
    const val FILE_NAME = "androidide-project.properties"

    fun load(rootDir: Path): NativeProjectDescriptor {
      val descriptor = rootDir.resolve(FILE_NAME)
      check(Files.isRegularFile(descriptor)) {
        "Native Android project descriptor not found: " + descriptor
      }

      val properties = Properties()
      Files.newInputStream(descriptor).use(properties::load)

      fun required(name: String): String =
        properties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
          ?: error("Missing native project property: " + name)

      fun int(name: String): Int =
        required(name).toIntOrNull()
          ?: error("Invalid integer native project property: " + name)

      return NativeProjectDescriptor(
        rootDir = rootDir,
        namespace = required("namespace"),
        applicationId = properties.getProperty(
          "applicationId",
          required("namespace")
        ).trim(),
        compileSdk = int("compileSdk"),
        minSdk = int("minSdk"),
        targetSdk = int("targetSdk"),
        sdkRoot = Path.of(required("sdkRoot")),
        versionCode = properties.getProperty(
          "versionCode",
          "1"
        ).trim().toIntOrNull() ?: 1,
        versionName = properties.getProperty(
          "versionName",
          "1.0"
        ).trim(),
        nativeActivity = properties.getProperty(
          "nativeActivity",
          "false"
        ).trim().toBoolean(),
        nativeLibraryName = properties.getProperty(
          "nativeLibraryName"
        )?.trim()?.takeIf { it.isNotEmpty() }
      )
    }
  }

  fun writeExample(destination: Path) {
    val text = buildString {
      appendLine("namespace=" + namespace)
      appendLine("applicationId=" + applicationId)
      appendLine("compileSdk=" + compileSdk)
      appendLine("minSdk=" + minSdk)
      appendLine("targetSdk=" + targetSdk)
      appendLine("sdkRoot=" + sdkRoot)
      appendLine("versionCode=" + versionCode)
      appendLine("versionName=" + versionName)
      appendLine("nativeActivity=" + nativeActivity)
      nativeLibraryName?.let {
        appendLine("nativeLibraryName=" + it)
      }
    }

    destination.parent?.let { Files.createDirectories(it) }
    Files.writeString(destination, text)
  }
}
