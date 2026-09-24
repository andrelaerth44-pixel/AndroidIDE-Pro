/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

import java.util.LinkedHashMap

class BuildGraph(tasks: Collection<BuildTask>) {

  val tasks: Map<TaskId, BuildTask>

  init {
    val indexed = LinkedHashMap<TaskId, BuildTask>(tasks.size)
    for (task in tasks) {
      require(task.id.isNotBlank()) { "Task id cannot be blank" }
      require(indexed.put(task.id, task) == null) {
        "Duplicate task id '${task.id}'"
      }
    }

    for (task in indexed.values) {
      for (dependency in task.dependencies) {
        require(indexed.containsKey(dependency)) {
          "Task '${task.id}' depends on missing task '$dependency'"
        }
      }
    }

    this.tasks = indexed.toMap()
    validateAcyclic()
  }

  fun topologicalOrder(): List<BuildTask> {
    val remaining = tasks.toMutableMap()
    val result = ArrayList<BuildTask>(tasks.size)

    while (remaining.isNotEmpty()) {
      val ready = remaining.values
        .filter { candidate -> candidate.dependencies.none { dependency -> remaining.containsKey(dependency) } }
        .sortedBy(BuildTask::id)

      require(ready.isNotEmpty()) {
        "Build graph contains a dependency cycle"
      }

      for (task in ready) {
        result += task
        remaining.remove(task.id)
      }
    }

    return result
  }

  private fun validateAcyclic() {
    val state = HashMap<TaskId, VisitState>()

    fun visit(id: TaskId) {
      when (state[id]) {
        VisitState.VISITING -> error("Build graph contains a dependency cycle at '$id'")
        VisitState.VISITED -> return
        null -> Unit
      }

      state[id] = VisitState.VISITING
      for (dependency in tasks.getValue(id).dependencies) {
        visit(dependency)
      }
      state[id] = VisitState.VISITED
    }

    tasks.keys.forEach(::visit)
  }

  private enum class VisitState {
    VISITING,
    VISITED,
  }
}
