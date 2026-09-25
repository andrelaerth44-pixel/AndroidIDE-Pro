package com.itsaky.androidide.native.build

import com.itsaky.androidide.utils.Environment
import java.io.File
import java.util.concurrent.TimeUnit

data class NativeApkPackageResult(
  val success: Boolean,
  val outputApk: File?,
  val failedStage: String? = null,
  val message: String? = null,
)

class NativeApkPackageExecutor(
  private val commandExecutor:
    (NativeCommandSpec, Map<String, String>, NativeProcessController?, (String) -> Unit) -> NativeProcessResult
    = { command, environment, controller, onOutput ->
      NativeCommandExecutor.execute(command, environment, controller, onOutput)
    },
) {

  fun execute(
    request: NativeApkPackageRequest,
    controller: NativeProcessController? = null,
    onOutput: (String) -> Unit = {},
  ): NativeApkPackageResult {
    val plan =
      runCatching { NativeApkPackagePlanner.plan(request) }
        .getOrElse { error ->
          return NativeApkPackageResult(
            success = false,
            outputApk = null,
            failedStage = "PLAN",
            message = error.message,
          )
        }

    val compile =
      commandExecutor(
        plan.compileResources.asNativeSpec(),
        Environment.getEnvironment(),
        controller,
        onOutput,
      )
    if (!compile.success) {
      return NativeApkPackageResult(
        success = false,
        outputApk = null,
        failedStage = "COMPILE_RESOURCES",
        message = compile.output.ifBlank { "AAPT2 resource compilation failed" },
      )
    }

    val link =
      commandExecutor(
        plan.linkResources.asNativeSpec(),
        Environment.getEnvironment(),
        controller,
        onOutput,
      )
    if (!link.success) {
      return NativeApkPackageResult(
        success = false,
        outputApk = null,
        failedStage = "LINK_RESOURCES",
        message = link.output.ifBlank { "AAPT2 resource linking failed" },
      )
    }

    val merged =
      runCatching {
        NativeApkLibraryPackager.merge(
          inputApk = request.unsignedApk,
          nativeLibrary = request.nativeLibrary,
          abi = request.abi,
          moduleName = request.moduleName,
          outputApk = request.mergedApk,
        )
      }.getOrElse { error ->
        return NativeApkPackageResult(
          success = false,
          outputApk = null,
          failedStage = "MERGE_NATIVE",
          message = error.message,
        )
      }

    return NativeApkPackageResult(
      success = merged.isFile,
      outputApk = merged.takeIf(File::isFile),
      failedStage = if (merged.isFile) null else "MERGE_NATIVE",
      message = if (merged.isFile) null else "Merged APK was not created",
    )
  }
}
