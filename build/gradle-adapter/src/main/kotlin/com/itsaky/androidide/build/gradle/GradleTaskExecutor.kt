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
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult

fun interface GradleTaskExecutor {
  fun execute(taskPath: String, context: TaskContext): TaskResult

  companion object {
    val UNAVAILABLE: GradleTaskExecutor = GradleTaskExecutor { taskPath, context ->
      TaskResult(
        taskId = taskPath,
        state = TaskResult.State.FAILED,
        diagnostics = listOf(
          BuildDiagnostic(
            severity = BuildDiagnostic.Severity.ERROR,
            kind = BuildDiagnostic.Kind.CONFIGURATION,
            message = "No Gradle execution bridge is configured for task '$taskPath'.",
            source = context.projectDir,
            task = taskPath,
            detail = "The compatibility adapter currently plans Gradle tasks only.",
          )
        ),
        message = "Gradle execution bridge unavailable",
      )
    }
  }
}
