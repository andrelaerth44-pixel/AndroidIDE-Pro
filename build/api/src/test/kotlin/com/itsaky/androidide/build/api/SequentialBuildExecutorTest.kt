/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SequentialBuildExecutorTest {

  private val context = BuildContext(
    projectDir = Paths.get("/project"),
    cacheDir = Paths.get("/cache"),
    toolchainsDir = Paths.get("/toolchains"),
  )

  @Test
  fun executesDependenciesFirst() {
    val executed = mutableListOf<String>()
    val graph = BuildGraph(
      listOf(
        task("package", setOf("compile")) { executed += "package" },
        task("compile") { executed += "compile" },
      )
    )

    val result = SequentialBuildExecutor().execute(graph, context)

    assertEquals(BuildResult.State.SUCCESS, result.state)
    assertEquals(listOf("compile", "package"), executed)
  }

  @Test
  fun stopsAfterFailure() {
    val executed = mutableListOf<String>()
    val graph = BuildGraph(
      listOf(
        task("first") {
          executed += "first"
          error("boom")
        },
        task("second") { executed += "second" },
      )
    )

    val result = SequentialBuildExecutor().execute(graph, context)

    assertEquals(BuildResult.State.FAILED, result.state)
    assertEquals(listOf("first"), executed)
    assertTrue(result.diagnostics.isNotEmpty())
  }

  @Test
  fun respectsCancellationBeforeNextTask() {
    val executed = mutableListOf<String>()
    val cancellation = CancellationToken { true }
    val result = SequentialBuildExecutor().execute(
      BuildGraph(listOf(task("compile") { executed += "compile" })),
      context.copy(cancellation = cancellation),
    )

    assertEquals(BuildResult.State.CANCELLED, result.state)
    assertTrue(executed.isEmpty())
  }

  private fun task(
    id: String,
    dependencies: Set<TaskId> = emptySet(),
    action: () -> Unit,
  ): BuildTask = object : BuildTask {
    override val id: TaskId = id
    override val dependencies: Set<TaskId> = dependencies

    override fun execute(context: TaskContext): TaskResult {
      action()
      return TaskResult(taskId = id, state = TaskResult.State.SUCCESS)
    }
  }
}
