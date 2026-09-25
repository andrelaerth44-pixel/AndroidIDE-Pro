package com.itsaky.androidide.native.build

/**
 * Immutable native build graph with deterministic topological ordering.
 */
class NativeBuildGraph(
  tasks: List<NativeBuildTask>,
) {

  private val tasksById: Map<String, NativeBuildTask> = tasks.associateBy { it.id }

  init {
    require(tasksById.size == tasks.size) {
      "Native build task ids must be unique"
    }

    tasks.forEach { task ->
      task.dependencies.forEach { dependency ->
        require(dependency in tasksById) {
          "Unknown dependency '$dependency' for task 'undefined'"
        }
      }
    }

    topologicalOrder()
  }

  val tasks: List<NativeBuildTask>
    get() = tasksById.values.toList()

  fun task(id: String): NativeBuildTask? = tasksById[id]

  /**
   * Returns tasks in deterministic dependency-first order.
   */
  fun topologicalOrder(): List<NativeBuildTask> {
    val result = mutableListOf<NativeBuildTask>()
    val temporary = mutableSetOf<String>()
    val permanent = mutableSetOf<String>()

    fun visit(id: String) {
      if (id in permanent) return

      check(id !in temporary) {
        "Cycle detected in native build graph at task '$id'"
      }

      temporary += id
      val task = requireNotNull(tasksById[id])
      task.dependencies.sorted().forEach(::visit)
      temporary -= id
      permanent += id
      result += task
    }

    tasksById.keys.sorted().forEach(::visit)
    return result
  }

  fun readyTasks(completedTaskIds: Set<String>): List<NativeBuildTask> =
    tasksById.values
      .asSequence()
      .filter { it.id !in completedTaskIds }
      .filter { task -> task.dependencies.all(completedTaskIds::contains) }
      .sortedBy { it.id }
      .toList()
}
