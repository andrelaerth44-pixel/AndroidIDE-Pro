package com.itsaky.androidide.native.build

import com.itsaky.androidide.utils.Environment
import java.io.File

data class Aapt2CommandPlan(
  val executable: File,
  val arguments: List<String>,
  val workingDirectory: File? = null,
) {
  fun asNativeSpec(): NativeCommandSpec =
    NativeCommandSpec(
      executable = executable,
      arguments = arguments,
      workingDirectory = workingDirectory,
    )
}

object Aapt2CommandPlanner {

  fun planCompile(
    resourceDirectory: File,
    compiledResources: File,
  ): Aapt2CommandPlan {
    require(resourceDirectory.isDirectory) {
      "Resource directory does not exist: " + resourceDirectory.absolutePath
    }
    compiledResources.parentFile?.mkdirs()

    return Aapt2CommandPlan(
      executable = requireExecutable(),
      arguments =
        listOf(
          "compile",
          "--dir",
          resourceDirectory.absolutePath,
          "-o",
          compiledResources.absolutePath,
        ),
      workingDirectory = resourceDirectory.parentFile,
    )
  }

  fun planLink(
    compiledResources: File,
    manifest: File,
    androidJar: File,
    outputApk: File,
    javaSourceOutput: File? = null,
    minSdk: Int = 28,
    targetSdk: Int = minSdk,
  ): Aapt2CommandPlan {
    require(compiledResources.isFile) {
      "Compiled resources archive does not exist: " + compiledResources.absolutePath
    }
    require(manifest.isFile) {
      "Android manifest does not exist: " + manifest.absolutePath
    }
    require(androidJar.isFile) {
      "Android platform jar does not exist: " + androidJar.absolutePath
    }
    require(minSdk > 0) { "minSdk must be positive" }
    require(targetSdk >= minSdk) { "targetSdk must be >= minSdk" }

    outputApk.parentFile?.mkdirs()
    javaSourceOutput?.mkdirs()

    val arguments = mutableListOf(
      "link",
      "-o",
      outputApk.absolutePath,
      "--manifest",
      manifest.absolutePath,
      "-I",
      androidJar.absolutePath,
      "--min-sdk-version",
      minSdk.toString(),
      "--target-sdk-version",
      targetSdk.toString(),
      "-R",
      compiledResources.absolutePath,
    )

    javaSourceOutput?.let {
      arguments.addAll(listOf("--java", it.absolutePath))
    }

    return Aapt2CommandPlan(
      executable = requireExecutable(),
      arguments = arguments,
      workingDirectory = outputApk.parentFile,
    )
  }

  private fun requireExecutable(): File =
    require(Environment.AAPT2.isFile) {
      "AAPT2 is not installed at " + Environment.AAPT2.absolutePath
    }.let { Environment.AAPT2 }
}
