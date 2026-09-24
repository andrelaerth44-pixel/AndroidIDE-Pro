/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local

import com.itsaky.androidide.build.api.Artifact
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files

internal class CleanProjectTask(
  private val project: com.itsaky.androidide.build.api.BuildProject,
) : BuildTask {

  override val id: String = "clean"
  override val displayName: String = "Clean generated build outputs"

  override val outputs: Set<Artifact>
    get() = project.modules
      .map { Artifact(it.projectDir.resolve("build")) }
      .toSet()

  override fun execute(context: TaskContext): TaskResult {
    val targets = buildSet {
      add(project.rootDir.resolve("build"))
      project.modules.forEach { module ->
        add(module.projectDir.resolve("build"))
      }
    }

    var removed = 0
    val diagnostics = mutableListOf<com.itsaky.androidide.build.api.BuildDiagnostic>()

    for (target in targets) {
      if (context.cancellation.isCancellationRequested()) {
        return TaskResult(
          taskId = id,
          state = TaskResult.State.CANCELLED,
          outputs = emptySet(),
          diagnostics = diagnostics,
          message = "Clean cancelled",
        )
      }

      if (!Files.exists(target)) continue

      try {
        Files.walk(target).use { stream ->
          stream.sorted(Comparator.reverseOrder()).forEach { path ->
            Files.deleteIfExists(path)
            removed++
          }
        }
      } catch (error: Throwable) {
        diagnostics += com.itsaky.androidide.build.api.BuildDiagnostic(
          severity = com.itsaky.androidide.build.api.BuildDiagnostic.Severity.ERROR,
          kind = com.itsaky.androidide.build.api.BuildDiagnostic.Kind.GENERAL,
          message = error.message ?: error::class.java.simpleName,
          source = target,
          task = id,
          detail = error.stackTraceToString(),
        )
        return TaskResult(
          taskId = id,
          state = TaskResult.State.FAILED,
          diagnostics = diagnostics,
          message = "Unable to clean '$target'",
        )
      }
    }

    return TaskResult(
      taskId = id,
      state = TaskResult.State.SUCCESS,
      outputs = targets.map(::Artifact).toSet(),
      diagnostics = diagnostics,
      message = "Removed $removed generated filesystem entries",
    )
  }
}
