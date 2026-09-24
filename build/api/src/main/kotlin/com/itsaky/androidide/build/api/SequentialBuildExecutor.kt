/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

/**
 * Deterministic task executor used as the first concrete implementation of the BuildExecutor SPI.
 *
 * It intentionally executes one task at a time. This gives the build engine a correct baseline for
 * cancellation, failure propagation and diagnostic aggregation before parallel scheduling,
 * fingerprints and cache reuse are introduced.
 */
class SequentialBuildExecutor : BuildExecutor {

  override fun execute(
    graph: BuildGraph,
    context: BuildContext,
  ): BuildResult {
    val taskResults = ArrayList<TaskResult>(graph.tasks.size)
    val diagnostics = ArrayList<BuildDiagnostic>()

    for (task in graph.topologicalOrder()) {
      if (context.cancellation.isCancellationRequested()) {
        return BuildResult(
          state = BuildResult.State.CANCELLED,
          taskResults = taskResults,
          diagnostics = diagnostics,
          message = "Build cancellation requested",
        )
      }

      val taskContext = TaskContext(
        projectDir = context.projectDir,
        cacheDir = context.cacheDir,
        toolchainsDir = context.toolchainsDir,
        environment = context.environment,
        cancellation = context.cancellation,
        diagnostics = context.diagnostics,
      )

      val result = try {
        task.execute(taskContext)
      } catch (error: Throwable) {
        TaskResult(
          taskId = task.id,
          state = TaskResult.State.FAILED,
          diagnostics = listOf(
            BuildDiagnostic(
              severity = BuildDiagnostic.Severity.ERROR,
              kind = BuildDiagnostic.Kind.INTERNAL,
              message = error.message ?: error::class.java.simpleName,
              task = task.id,
              source = context.projectDir,
              detail = error.stackTraceToString(),
            )
          ),
          message = "Task execution threw an exception",
        )
      }

      taskResults += result
      diagnostics += result.diagnostics
      result.diagnostics.forEach(context.diagnostics::report)

      when (result.state) {
        TaskResult.State.SUCCESS,
        TaskResult.State.SKIPPED -> Unit

        TaskResult.State.CANCELLED -> {
          return BuildResult(
            state = BuildResult.State.CANCELLED,
            taskResults = taskResults,
            diagnostics = diagnostics,
            message = result.message ?: "Task cancelled",
          )
        }

        TaskResult.State.FAILED -> {
          return BuildResult(
            state = BuildResult.State.FAILED,
            taskResults = taskResults,
            diagnostics = diagnostics,
            message = result.message ?: "Task failed",
          )
        }
      }
    }

    return BuildResult(
      state = BuildResult.State.SUCCESS,
      taskResults = taskResults,
      diagnostics = diagnostics,
    )
  }
}
