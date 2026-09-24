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
