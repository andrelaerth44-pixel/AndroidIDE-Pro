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

/** Executes the native `aapt2 compile` stage after resource merging. */
class Aapt2CompileTask(
  override val id: String,
  private val mergedResources: Path,
  private val outputDir: Path,
  private val aapt2: Aapt2Tool,
  override val dependencies: Set<String> = emptySet(),
) : BuildTask {

  override val inputs: Set<Artifact> = setOf(Artifact(mergedResources))
  override val outputs: Set<Artifact> = setOf(Artifact(outputDir))

  override fun execute(context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return TaskResult(id, TaskResult.State.CANCELLED, message = "AAPT2 compile cancelled")
    }
    return try {
      Files.createDirectories(outputDir)
      val compiled = aapt2.compile(listOf(mergedResources), outputDir)
      if (compiled.result.success) {
        TaskResult(
          taskId = id,
          state = TaskResult.State.SUCCESS,
          outputs = compiled.archives.map(::Artifact).toSet(),
          message = "AAPT2 resources compiled",
        )
      } else {
        val message = compiled.result.message ?: compiled.result.log.lastOrNull() ?: "AAPT2 compile failed"
        val diagnostic = BuildDiagnostic(
          severity = BuildDiagnostic.Severity.ERROR,
          kind = BuildDiagnostic.Kind.RESOURCE,
          message = message,
          task = id,
          source = context.projectDir,
          detail = compiled.result.log.joinToString("\n"),
        )
        context.diagnostics.report(diagnostic)
        TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "AAPT2 compile failed")
      }
    } catch (error: Throwable) {
      val diagnostic = BuildDiagnostic(
        severity = BuildDiagnostic.Severity.ERROR,
        kind = BuildDiagnostic.Kind.INTERNAL,
        message = error.message ?: error::class.java.simpleName,
        task = id,
        source = context.projectDir,
        detail = error.stackTraceToString(),
      )
      context.diagnostics.report(diagnostic)
      TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "AAPT2 compile failed internally")
    }
  }
}
