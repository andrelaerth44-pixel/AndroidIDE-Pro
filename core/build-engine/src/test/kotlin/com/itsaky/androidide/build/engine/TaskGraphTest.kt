package com.itsaky.androidide.build.engine

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskGraphTest {
  @Test
  fun executesDependenciesBeforeRoot() {
    val temp = Files.createTempDirectory("androidide-build-test")
    val order = mutableListOf<String>()

    val a = RecordingTask("a", temp.resolve("a.out")) { order += "a" }
    val b = RecordingTask("b", temp.resolve("b.out")) { order += "b" }

    TaskGraph()
      .add(a)
      .add(b)
      .dependsOn("b", "a")
      .execute(DefaultBuildContext())

    assertEquals(listOf("a", "b"), order)
  }

  @Test
  fun failedTaskCanRunAgain() {
    val temp = Files.createTempDirectory("androidide-build-failure-test")
    var fail = true
    var executions = 0

    val task = object : BuildTask {
      override val id = "flaky"
      override val inputs: List<Path> = emptyList()
      override val outputs: List<Path> = listOf(temp.resolve("flaky.out"))

      override fun execute(context: BuildContext): TaskResult {
        executions++
        if (fail) {
          return TaskResult(false, "expected failure")
        }
        Files.writeString(outputs.single(), "ok")
        return TaskResult(true)
      }
    }

    val graph = TaskGraph().add(task)

    assertEquals(false, graph.execute(DefaultBuildContext()).success)
    fail = false
    assertEquals(true, graph.execute(DefaultBuildContext()).success)
    assertEquals(2, executions)
  }

  private class RecordingTask(
    override val id: String,
    private val output: Path,
    private val action: () -> Unit
  ) : BuildTask {
    override val inputs: List<Path> = emptyList()
    override val outputs: List<Path> = listOf(output)

    override fun execute(context: BuildContext): TaskResult {
      action()
      Files.writeString(output, id)
      return TaskResult(true)
    }
  }
}
