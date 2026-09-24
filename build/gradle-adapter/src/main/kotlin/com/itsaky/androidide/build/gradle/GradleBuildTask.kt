/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.gradle

import com.itsaky.androidide.build.api.BuildDiagnostic
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskId
import com.itsaky.androidide.build.api.TaskResult

internal class GradleBuildTask(
  override val id: TaskId,
  override val dependencies: Set<TaskId>,
  private val executor: GradleTaskExecutor,
) : BuildTask {
  override val displayName: String get() = "Gradle $id"

  override fun execute(context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return TaskResult(id, TaskResult.State.CANCELLED, message = "Build cancellation requested")
    }
    return try {
      executor.execute(id, context)
    } catch (error: Throwable) {
      TaskResult(
        taskId = id,
        state = TaskResult.State.FAILED,
        diagnostics = listOf(
          BuildDiagnostic(
            severity = BuildDiagnostic.Severity.ERROR,
            kind = BuildDiagnostic.Kind.INTERNAL,
            message = error.message ?: error::class.java.simpleName,
            source = context.projectDir,
            task = id,
            detail = error.stackTraceToString(),
          )
        ),
        message = "Gradle task execution failed",
      )
    }
  }
}
