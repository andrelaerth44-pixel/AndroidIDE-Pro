/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import com.itsaky.androidide.build.api.Artifact
import com.itsaky.androidide.build.api.BuildDiagnostic
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path

/** Executes `aapt2 link`, producing resources.ap_ and generated R.java sources. */
class Aapt2LinkTask(
  override val id: String,
  private val compiledResources: Path,
  private val manifest: Path,
  private val androidJar: Path,
  private val packageName: String,
  private val minSdk: Int,
  private val targetSdk: Int,
  private val generatedRDir: Path,
  private val resourcesAp: Path,
  private val aapt2: Aapt2Tool,
  private val extraPackages: List<String> = emptyList(),
  private val versionCode: Int? = null,
  private val versionName: String? = null,
  override val dependencies: Set<String> = emptySet(),
) : BuildTask {

  override val inputs: Set<Artifact> = setOf(
    Artifact(compiledResources),
    Artifact(manifest),
    Artifact(androidJar),
  )

  override val outputs: Set<Artifact> = setOf(
    Artifact(generatedRDir),
    Artifact(resourcesAp),
  )

  override fun execute(context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return TaskResult(id, TaskResult.State.CANCELLED, message = "AAPT2 link cancelled")
    }
    return try {
      val archives = if (Files.isDirectory(compiledResources)) {
        Files.list(compiledResources).use { stream ->
          stream.filter(Files::isRegularFile).filter { it.fileName.toString().endsWith(".zip") }.sorted().toList()
        }
      } else emptyList()

      val result = aapt2.link(
        compiled = archives,
        manifest = manifest,
        androidJar = androidJar,
        packageName = packageName,
        extraPackages = extraPackages,
        minSdk = minSdk,
        targetSdk = targetSdk,
        outputDir = generatedRDir,
        resourcesAp = resourcesAp,
        versionCode = versionCode,
        versionName = versionName,
      )
      if (result.success) {
        TaskResult(id, TaskResult.State.SUCCESS, outputs = outputs, message = "AAPT2 resources linked")
      } else {
        val message = result.message ?: result.log.lastOrNull() ?: "AAPT2 link failed"
        val diagnostic = BuildDiagnostic(
          severity = BuildDiagnostic.Severity.ERROR,
          kind = BuildDiagnostic.Kind.LINKER,
          message = message,
          task = id,
          source = manifest,
          detail = result.log.joinToString("\n"),
        )
        context.diagnostics.report(diagnostic)
        TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "AAPT2 link failed")
      }
    } catch (error: Throwable) {
      val diagnostic = BuildDiagnostic(
        severity = BuildDiagnostic.Severity.ERROR,
        kind = BuildDiagnostic.Kind.INTERNAL,
        message = error.message ?: error::class.java.simpleName,
        task = id,
        source = manifest,
        detail = error.stackTraceToString(),
      )
      context.diagnostics.report(diagnostic)
      TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "AAPT2 link failed internally")
    }
  }
}
