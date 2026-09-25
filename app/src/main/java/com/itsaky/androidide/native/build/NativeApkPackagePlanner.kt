package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import java.io.File

data class NativeApkPackageRequest(
  val moduleRoot: File,
  val resourceDirectory: File,
  val manifest: File,
  val compiledResources: File,
  val unsignedApk: File,
  val mergedApk: File,
  val nativeLibrary: File,
  val moduleName: String,
  val abi: AbiTarget = AbiTarget.ARM64_V8A,
  val androidJar: File,
  val minSdk: Int = 28,
  val targetSdk: Int = minSdk,
  val generatedJavaDirectory: File? = null,
)

data class NativeApkPackagePlan(
  val compileResources: Aapt2CommandPlan,
  val linkResources: Aapt2CommandPlan,
  val mergeNativeLibrary: File,
)

object NativeApkPackagePlanner {

  fun plan(request: NativeApkPackageRequest): NativeApkPackagePlan {
    require(request.moduleRoot.isDirectory) {
      "Native Android module root does not exist: " + request.moduleRoot.absolutePath
    }
    require(request.resourceDirectory.isDirectory) {
      "Android resource directory does not exist: " + request.resourceDirectory.absolutePath
    }
    require(request.manifest.isFile) {
      "Android manifest does not exist: " + request.manifest.absolutePath
    }
    require(request.nativeLibrary.isFile) {
      "Native library does not exist: " + request.nativeLibrary.absolutePath
    }
    require(request.androidJar.isFile) {
      "Android platform jar does not exist: " + request.androidJar.absolutePath
    }

    return NativeApkPackagePlan(
      compileResources =
        Aapt2CommandPlanner.planCompile(
          resourceDirectory = request.resourceDirectory,
          compiledResources = request.compiledResources,
        ),
      linkResources =
        Aapt2CommandPlanner.planLink(
          compiledResources = request.compiledResources,
          manifest = request.manifest,
          androidJar = request.androidJar,
          outputApk = request.unsignedApk,
          javaSourceOutput = request.generatedJavaDirectory,
          minSdk = request.minSdk,
          targetSdk = request.targetSdk,
        ),
      mergeNativeLibrary = request.nativeLibrary,
    )
  }
}
