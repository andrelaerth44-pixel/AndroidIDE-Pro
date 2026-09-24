/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.services.builder

import com.itsaky.androidide.build.api.BuildDiagnostic
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult
import com.itsaky.androidide.projects.builder.BuildService
import com.itsaky.androidide.tooling.api.messages.result.TaskExecutionResult
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Bridges the existing AndroidIDE BuildService to the new GradleTaskExecutor SPI.
 *
 * This class deliberately depends on the BuildService contract rather than the concrete service,
 * which keeps the bridge testable and allows the existing GradleBuildService to remain the runtime
 * owner of the Tooling API process.
 */
class GradleBuildServiceTaskExecutor(
  private val buildService: BuildService,
) : com.itsaky.androidide.build.gradle.GradleTaskExecutor {

  override fun execute(taskPath: String, context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return cancelled(taskPath, "Build cancellation requested before Gradle execution")
    }

    val startNanos = System.nanoTime()
    val future = try {
      buildService.executeTasks(taskPath)
    } catch (error: Throwable) {
      return failed(taskPath, error, startNanos)
    }

    var cancellationRequested = false

    while (true) {
      try {
        val result = future.get(100, TimeUnit.MILLISECONDS)
        return mapResult(taskPath, result, startNanos)
      } catch (_: TimeoutException) {
        if (!cancellationRequested && context.cancellation.isCancellationRequested()) {
          cancellationRequested = true
          try {
            buildService.cancelCurrentBuild()
          } catch (error: Throwable) {
            return failed(taskPath, error, startNanos)
          }
        }
      } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        return cancelled(taskPath, "Gradle task executor interrupted")
      } catch (error: ExecutionException) {
        return failed(taskPath, error.cause ?: error, startNanos)
      }
    }
  }

  private fun mapResult(
    taskPath: String,
    result: TaskExecutionResult,
    startNanos: Long,
  ): TaskResult {
    if (result.isSuccessful) {
      return TaskResult(
        taskId = taskPath,
        state = TaskResult.State.SUCCESS,
        message = "Gradle task completed",
        durationMillis = elapsedMillis(startNanos),
      )
    }

    val failure = result.failure
    val cancelled = failure == TaskExecutionResult.Failure.BUILD_CANCELLED
    val message = failure?.name ?: "UNKNOWN"

    return if (cancelled) {
      cancelled(taskPath, message, startNanos)
    } else {
      TaskResult(
        taskId = taskPath,
        state = TaskResult.State.FAILED,
        diagnostics = listOf(
          BuildDiagnostic(
            severity = BuildDiagnostic.Severity.ERROR,
            kind = diagnosticKind(failure),
            message = "Gradle task failed: $message",
            source = null,
            task = taskPath,
            detail = failure?.name,
          )
        ),
        message = message,
        durationMillis = elapsedMillis(startNanos),
      )
    }
  }

  private fun failed(
    taskPath: String,
    error: Throwable,
    startNanos: Long,
  ): TaskResult {
    return TaskResult(
      taskId = taskPath,
      state = TaskResult.State.FAILED,
      diagnostics = listOf(
        BuildDiagnostic(
          severity = BuildDiagnostic.Severity.ERROR,
          kind = BuildDiagnostic.Kind.INTERNAL,
          message = error.message ?: error::class.java.simpleName,
          task = taskPath,
          detail = error.stackTraceToString(),
        )
      ),
      message = "Gradle task bridge failed",
      durationMillis = elapsedMillis(startNanos),
    )
  }

  private fun cancelled(
    taskPath: String,
    message: String,
    startNanos: Long = System.nanoTime(),
  ): TaskResult {
    return TaskResult(
      taskId = taskPath,
      state = TaskResult.State.CANCELLED,
      message = message,
      durationMillis = elapsedMillis(startNanos),
    )
  }

  private fun diagnosticKind(
    failure: TaskExecutionResult.Failure?,
  ): BuildDiagnostic.Kind {
    return when (failure) {
      TaskExecutionResult.Failure.PROJECT_NOT_FOUND,
      TaskExecutionResult.Failure.PROJECT_NOT_INITIALIZED,
      TaskExecutionResult.Failure.PROJECT_NOT_DIRECTORY,
      TaskExecutionResult.Failure.PROJECT_DIRECTORY_INACCESSIBLE,
      TaskExecutionResult.Failure.UNKNOWN,
      TaskExecutionResult.Failure.UNSUPPORTED_CONFIGURATION,
      TaskExecutionResult.Failure.UNSUPPORTED_BUILD_ARGUMENT,
      TaskExecutionResult.Failure.UNSUPPORTED_GRADLE_VERSION ->
        BuildDiagnostic.Kind.CONFIGURATION

      TaskExecutionResult.Failure.CONNECTION_ERROR,
      TaskExecutionResult.Failure.CONNECTION_CLOSED ->
        BuildDiagnostic.Kind.INTERNAL

      TaskExecutionResult.Failure.BUILD_FAILED ->
        BuildDiagnostic.Kind.GENERAL

      TaskExecutionResult.Failure.BUILD_CANCELLED,
      null ->
        BuildDiagnostic.Kind.GENERAL
    }
  }

  private fun elapsedMillis(startNanos: Long): Long {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
  }
}
