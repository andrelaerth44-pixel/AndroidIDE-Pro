package com.itsaky.androidide.build.engine

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskResult

class DefaultBuildContext(
  private val logger: (String) -> Unit = ::println
) : BuildContext {
  override fun log(message: String) {
    logger(message)
  }
}

class TaskGraph {
  private val tasks = LinkedHashMap<String, BuildTask>()
  private val dependencies = LinkedHashMap<String, MutableList<String>>()

  fun add(task: BuildTask): TaskGraph {
    require(tasks.put(task.id, task) == null) {
      "Duplicate task: " + task.id
    }
    dependencies.getOrPut(task.id) { mutableListOf() }
    return this
  }

  fun dependsOn(taskId: String, dependencyId: String): TaskGraph {
    require(taskId in tasks) { "Unknown task: " + taskId }
    require(dependencyId in tasks) { "Unknown dependency: " + dependencyId }
    dependencies.getOrPut(taskId) { mutableListOf() }.add(dependencyId)
    return this
  }

  fun execute(context: BuildContext = DefaultBuildContext()): TaskResult {
    val state = HashMap<String, VisitState>()

    fun visit(id: String): TaskResult {
      when (state[id]) {
        VisitState.DONE -> return TaskResult(true)
        VisitState.VISITING -> {
          return TaskResult(false, "Cycle detected at task: " + id)
        }
        null -> Unit
      }

      val task = tasks[id]
        ?: return TaskResult(false, "Unknown task: " + id)

      state[id] = VisitState.VISITING

      for (dependency in dependencies[id].orEmpty()) {
        val result = visit(dependency)
        if (!result.success) return result
      }

      if (TaskUpToDate.isUpToDate(task)) {
        context.log("UP-TO-DATE: " + task.id)
        state[id] = VisitState.DONE
        return TaskResult(true, "UP-TO-DATE")
      }

      context.log("RUN: " + task.id)
      return try {
        val result = task.execute(context)
        if (result.success) state[id] = VisitState.DONE
        result
      } catch (t: Throwable) {
        TaskResult(
          false,
          task.id + ": " + (t.message ?: t.javaClass.simpleName)
        )
      }
    }

    if (tasks.isEmpty()) {
      return TaskResult(false, "Build graph is empty")
    }

    for (id in tasks.keys) {
      val result = visit(id)
      if (!result.success) return result
    }

    return TaskResult(true)
  }

  private enum class VisitState {
    VISITING,
    DONE
  }
}

object TaskUpToDate {
  fun isUpToDate(task: BuildTask): Boolean {
    if (task.outputs.isEmpty()) return false
    if (task.outputs.any { !it.toFile().exists() }) return false

    val newestInput = task.inputs
      .filter { it.toFile().exists() }
      .maxOfOrNull { it.toFile().lastModified() }
      ?: return true

    val oldestOutput = task.outputs
      .minOfOrNull { it.toFile().lastModified() }
      ?: return false

    return oldestOutput >= newestInput
  }
}
