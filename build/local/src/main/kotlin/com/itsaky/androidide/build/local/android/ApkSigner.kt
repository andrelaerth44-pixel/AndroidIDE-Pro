/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import java.nio.file.Files
import java.nio.file.Path

data class ApkSigningRequest(
  val inputApk: Path,
  val signedApk: Path,
  val keystore: Path,
  val storePassword: String,
  val keyAlias: String,
  val keyPassword: String,
)

fun interface ApkSigner {
  fun sign(request: ApkSigningRequest): AndroidToolResult
}

/**
 * SDK Build Tools signer. Signing is intentionally outside the APK packager so tests can use a fake signer
 * and the Android backend can later replace this subprocess with apksig in-process.
 */
class ApkSignerSubprocess(
  private val zipalign: Path,
  private val apksigner: Path,
) : ApkSigner {

  override fun sign(request: ApkSigningRequest): AndroidToolResult {
    request.signedApk.parent?.let(Files::createDirectories)
    val aligned = request.signedApk.resolveSibling(request.signedApk.fileName.toString() + ".aligned")

    val align = AndroidToolProcess.run(
      listOf(zipalign.toString(), "-f", "4", request.inputApk.toString(), aligned.toString()),
    )
    if (!align.success) return align

    return try {
      AndroidToolProcess.run(
        listOf(
          apksigner.toString(),
          "sign",
          "--ks", request.keystore.toString(),
          "--ks-pass", "pass:${request.storePassword}",
          "--ks-key-alias", request.keyAlias,
          "--key-pass", "pass:${request.keyPassword}",
          "--out", request.signedApk.toString(),
          aligned.toString(),
        ),
      )
    } finally {
      runCatching { Files.deleteIfExists(aligned) }
    }
  }
}

class SignApkTask(
  override val id: String,
  private val request: ApkSigningRequest,
  private val signer: ApkSigner,
  override val dependencies: Set<String> = emptySet(),
) : com.itsaky.androidide.build.api.BuildTask {

  override val inputs = setOf(
    com.itsaky.androidide.build.api.Artifact(request.inputApk),
    com.itsaky.androidide.build.api.Artifact(request.keystore),
  )
  override val outputs = setOf(com.itsaky.androidide.build.api.Artifact(request.signedApk))

  override fun execute(context: com.itsaky.androidide.build.api.TaskContext): com.itsaky.androidide.build.api.TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return com.itsaky.androidide.build.api.TaskResult(id, com.itsaky.androidide.build.api.TaskResult.State.CANCELLED)
    }
    val result = signer.sign(request)
    if (result.success) {
      return com.itsaky.androidide.build.api.TaskResult(
        taskId = id,
        state = com.itsaky.androidide.build.api.TaskResult.State.SUCCESS,
        outputs = outputs,
        message = "APK signed",
      )
    }
    val diagnostic = com.itsaky.androidide.build.api.BuildDiagnostic(
      severity = com.itsaky.androidide.build.api.BuildDiagnostic.Severity.ERROR,
      kind = com.itsaky.androidide.build.api.BuildDiagnostic.Kind.PACKAGING,
      message = result.message ?: result.log.lastOrNull() ?: "APK signing failed",
      source = request.inputApk,
      task = id,
      detail = result.log.joinToString("\n"),
    )
    context.diagnostics.report(diagnostic)
    return com.itsaky.androidide.build.api.TaskResult(
      taskId = id,
      state = com.itsaky.androidide.build.api.TaskResult.State.FAILED,
      diagnostics = listOf(diagnostic),
      message = "APK signing failed",
    )
  }
}
