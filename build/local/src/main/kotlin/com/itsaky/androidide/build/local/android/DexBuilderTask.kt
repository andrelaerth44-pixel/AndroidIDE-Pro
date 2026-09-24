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

/** Executes the `dexBuilder` archive stage using the injected D8 backend. */
class DexBuilderTask(
  override val id: String,
  private val classInputs: List<Path>,
  private val classpath: List<Path>,
  private val androidJar: Path,
  private val minSdk: Int,
  private val outputDir: Path,
  private val dexer: Dexer,
  private val threads: Int = 1,
  private val release: Boolean = false,
  override val dependencies: Set<String> = emptySet(),
) : BuildTask {

  override val inputs: Set<Artifact> = (classInputs + classpath + listOf(androidJar)).map(::Artifact).toSet()
  override val outputs: Set<Artifact> = setOf(Artifact(outputDir))

  override fun execute(context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return TaskResult(id, TaskResult.State.CANCELLED, message = "Dex archive cancelled")
    }
    return try {
      Files.createDirectories(outputDir)
      val result = dexer.dexArchive(
        inputs = classInputs,
        classpath = classpath,
        androidJar = androidJar,
        minApi = minSdk,
        release = release,
        outDir = outputDir,
        threads = threads.coerceAtLeast(1),
      )
      if (result.success) {
        TaskResult(id, TaskResult.State.SUCCESS, outputs = outputs, message = "Dex archive created")
      } else {
        val diagnostic = BuildDiagnostic(
          severity = BuildDiagnostic.Severity.ERROR,
          kind = BuildDiagnostic.Kind.PACKAGING,
          message = result.message ?: result.log.lastOrNull() ?: "D8 dexBuilder failed",
          source = context.projectDir,
          task = id,
          detail = result.log.joinToString("\n"),
        )
        context.diagnostics.report(diagnostic)
        TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "Dex archive failed")
      }
    } catch (error: Throwable) {
      val diagnostic = BuildDiagnostic(
        severity = BuildDiagnostic.Severity.ERROR,
        kind = BuildDiagnostic.Kind.INTERNAL,
        message = error.message ?: error::class.java.simpleName,
        source = context.projectDir,
        task = id,
        detail = error.stackTraceToString(),
      )
      context.diagnostics.report(diagnostic)
      TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "D8 dexBuilder failed internally")
    }
  }
}
