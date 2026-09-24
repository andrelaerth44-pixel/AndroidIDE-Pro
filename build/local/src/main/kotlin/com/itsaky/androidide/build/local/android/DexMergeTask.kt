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

/** Merges dex archives into indexed classes*.dex using D8. */
class DexMergeTask(
  override val id: String,
  private val dexInputs: List<Path>,
  private val androidJar: Path,
  private val minSdk: Int,
  private val outputDir: Path,
  private val dexer: Dexer,
  private val threads: Int = 1,
  private val release: Boolean = false,
  override val dependencies: Set<String> = emptySet(),
) : BuildTask {

  override val inputs: Set<Artifact> = (dexInputs + androidJar).map(::Artifact).toSet()
  override val outputs: Set<Artifact> = setOf(Artifact(outputDir))

  override fun execute(context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return TaskResult(id, TaskResult.State.CANCELLED, message = "Dex merge cancelled")
    }

    return try {
      val inputs = collectDexInputs(dexInputs)
      if (inputs.isEmpty()) {
        return TaskResult(id, TaskResult.State.SKIPPED, message = "No dex inputs")
      }
      cleanOutput(outputDir)
      Files.createDirectories(outputDir)

      val result = dexer.dex(
        inputs = inputs,
        androidJar = androidJar,
        minApi = minSdk,
        release = release,
        outDir = outputDir,
        threads = threads.coerceAtLeast(1),
      )
      if (result.success) {
        TaskResult(id, TaskResult.State.SUCCESS, outputs = outputs, message = "Dex merged")
      } else {
        val diagnostic = BuildDiagnostic(
          severity = BuildDiagnostic.Severity.ERROR,
          kind = BuildDiagnostic.Kind.PACKAGING,
          message = result.message ?: result.log.lastOrNull() ?: "D8 dex merge failed",
          source = context.projectDir,
          task = id,
          detail = result.log.joinToString("\n"),
        )
        context.diagnostics.report(diagnostic)
        TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "Dex merge failed")
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
      TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "Dex merge failed internally")
    }
  }

  private fun collectDexInputs(inputs: List<Path>): List<Path> =
    inputs.flatMap { input ->
      when {
        Files.isRegularFile(input) && input.toString().endsWith(".dex") -> listOf(input)
        Files.isDirectory(input) -> Files.walk(input).use { stream ->
          stream.filter(Files::isRegularFile)
            .filter { it.fileName.toString().endsWith(".dex") }
            .sorted()
            .toList()
        }
        else -> emptyList()
      }
    }

  private fun cleanOutput(path: Path) {
    if (!Files.exists(path)) return
    Files.walk(path).use { stream ->
      stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
  }
}
