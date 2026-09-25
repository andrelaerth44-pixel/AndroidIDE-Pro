package com.itsaky.androidide.native.build

/**
 * Immutable native build graph with deterministic dependency-first ordering.
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
          "Unknown dependency '" + dependency + "' for task '" + task.id + "'"
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
   *
   * When multiple tasks are ready, their semantic pipeline stage is used as
   * the tie-breaker instead of the task id.
   */
  fun topologicalOrder(): List<NativeBuildTask> {
    val result = mutableListOf<NativeBuildTask>()
    val temporary = mutableSetOf<String>()
    val permanent = mutableSetOf<String>()

    fun visit(id: String) {
      if (id in permanent) return

      check(id !in temporary) {
        "Cycle detected in native build graph at task '" + id + "'"
      }

      temporary += id
      val current = requireNotNull(tasksById[id])
      current.dependencies
        .sortedWith(taskComparator)
        .forEach(::visit)
      temporary -= id
      permanent += id
      result += current
    }

    tasksById.values
      .sortedWith(taskComparator)
      .map { it.id }
      .forEach(::visit)

    return result
  }

  fun readyTasks(completedTaskIds: Set<String>): List<NativeBuildTask> =
    tasksById.values
      .asSequence()
      .filter { it.id !in completedTaskIds }
      .filter { task -> task.dependencies.all(completedTaskIds::contains) }
      .sortedWith(taskComparator)
      .toList()

  private companion object {
    val taskComparator =
      compareBy<NativeBuildTask>({ it.kind.ordinal }, { it.id })
  }
}
