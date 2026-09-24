/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.java

import com.itsaky.androidide.build.api.Artifact
import com.itsaky.androidide.build.api.BuildDiagnostic
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskId
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.measureTimeMillis

/** First executable Java compilation task for the native Android build engine. */
class JavaCompileTask(
  override val id: TaskId,
  private val sources: List<Path>,
  private val classpath: List<Path>,
  private val outputDir: Path,
  private val bootClasspath: List<Path> = emptyList(),
  private val sourceLevel: String = "17",
  private val targetLevel: String = sourceLevel,
  override val dependencies: Set<TaskId> = emptySet(),
) : BuildTask {

  override val inputs: Set<Artifact> =
    (sources + classpath + bootClasspath).map(::Artifact).toSet()

  override val outputs: Set<Artifact> = setOf(Artifact(outputDir))

  override fun execute(context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return TaskResult(id, TaskResult.State.CANCELLED, message = "Java compilation cancelled")
    }

    if (sources.none(Files::isRegularFile)) {
      return TaskResult(id, TaskResult.State.SKIPPED, message = "No Java sources")
    }

    return try {
      Files.createDirectories(outputDir)
      var result: JdtJavaCompiler.Result? = null
      val duration = measureTimeMillis {
        result = JdtJavaCompiler.compile(
          JdtJavaCompiler.Request(
            sources = sources,
            classpath = classpath,
            outputDir = outputDir,
            sourceLevel = sourceLevel,
            targetLevel = targetLevel,
            bootClasspath = bootClasspath,
          )
        )
      }

      val compileResult = requireNotNull(result)
      val diagnostics = compileResult.diagnostics.map { diagnostic ->
        BuildDiagnostic(
          severity = if (diagnostic.error) BuildDiagnostic.Severity.ERROR else BuildDiagnostic.Severity.WARNING,
          kind = BuildDiagnostic.Kind.COMPILER,
          message = diagnostic.message,
          source = diagnostic.file,
          line = diagnostic.line,
          column = diagnostic.column.takeIf { it > 0 },
          task = id,
        )
      }
      diagnostics.forEach(context.diagnostics::report)

      TaskResult(
        taskId = id,
        state = if (compileResult.success) TaskResult.State.SUCCESS else TaskResult.State.FAILED,
        outputs = outputs,
        diagnostics = diagnostics,
        message = if (compileResult.success) "Java compilation completed" else "Java compilation failed",
        durationMillis = duration,
      )
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
      TaskResult(
        taskId = id,
        state = TaskResult.State.FAILED,
        diagnostics = listOf(diagnostic),
        message = "Java compilation failed internally",
      )
    }
  }
}
