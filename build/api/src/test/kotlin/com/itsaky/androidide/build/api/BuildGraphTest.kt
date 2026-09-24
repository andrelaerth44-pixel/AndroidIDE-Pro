/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BuildGraphTest {

  @Test
  fun ordersDependenciesBeforeDependents() {
    val compile = testTask("compile")
    val packageTask = testTask("package", "compile")
    val graph = BuildGraph(listOf(packageTask, compile))

    assertEquals(listOf("compile", "package"), graph.topologicalOrder().map(BuildTask::id))
  }

  @Test
  fun rejectsMissingDependency() {
    assertThrows(IllegalArgumentException::class.java) {
      BuildGraph(listOf(testTask("compile", "missing")))
    }
  }

  @Test
  fun rejectsDependencyCycles() {
    assertThrows(IllegalStateException::class.java) {
      BuildGraph(listOf(testTask("a", "b"), testTask("b", "a")))
    }
  }

  private fun testTask(id: String, vararg dependencies: TaskId): BuildTask =
    object : BuildTask {
      override val id = id
      override val dependencies = dependencies.toSet()

      override fun execute(context: TaskContext): TaskResult =
        TaskResult(id, TaskResult.State.SUCCESS)
    }
}
