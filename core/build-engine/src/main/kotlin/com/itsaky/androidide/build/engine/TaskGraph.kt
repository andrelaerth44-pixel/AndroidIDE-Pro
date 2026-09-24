package com.itsaky.androidide.build.engine

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskResult

class DefaultBuildContext(
  override val cacheRoot: java.nio.file.Path? = null,
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
        if (!result.success) {
          state.remove(id)
          return result
        }
      }

      if (TaskFingerprints.isUpToDate(task, context.cacheRoot)) {
        context.log("UP-TO-DATE: " + task.id)
        state[id] = VisitState.DONE
        return TaskResult(true, "UP-TO-DATE")
      }

      context.log("RUN: " + task.id)
      return try {
        val result = task.execute(context)
        if (result.success) {
          TaskFingerprints.write(task, context.cacheRoot)
          state[id] = VisitState.DONE
        } else {
          state.remove(id)
        }
        result
      } catch (t: Throwable) {
        state.remove(id)
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

object TaskFingerprints {
  private const val VERSION = "v1"

  fun isUpToDate(
    task: BuildTask,
    cacheRoot: java.nio.file.Path?
  ): Boolean {
    if (task.outputs.isEmpty()) return false
    if (task.outputs.any { !java.nio.file.Files.exists(it) }) return false

    val root = cacheRoot ?: return false
    val fingerprintFile = root
      .resolve("fingerprints")
      .resolve(task.id.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".sha256")

    if (!java.nio.file.Files.exists(fingerprintFile)) return false

    val current = fingerprint(task)
    val stored = runCatching {
      java.nio.file.Files.readString(fingerprintFile).trim()
    }.getOrNull()

    return stored == current
  }

  fun write(
    task: BuildTask,
    cacheRoot: java.nio.file.Path?
  ) {
    val root = cacheRoot ?: return
    val dir = root.resolve("fingerprints")
    java.nio.file.Files.createDirectories(dir)

    val file = dir.resolve(
      task.id.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".sha256"
    )

    java.nio.file.Files.writeString(file, fingerprint(task))
  }

  private fun fingerprint(task: BuildTask): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")

    update(digest, VERSION)
    update(digest, task.id)

    task.inputs
      .map { it.toAbsolutePath().normalize() }
      .sortedBy { it.toString() }
      .forEach { path ->
        updatePath(digest, path)
      }

    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun updatePath(
    digest: java.security.MessageDigest,
    path: java.nio.file.Path
  ) {
    update(digest, path.toString())

    if (!java.nio.file.Files.exists(path)) {
      update(digest, "<missing>")
      return
    }

    if (java.nio.file.Files.isDirectory(path)) {
      java.nio.file.Files.walk(path).use { stream ->
        stream
          .filter { java.nio.file.Files.isRegularFile(it) }
          .map { it.toAbsolutePath().normalize() }
          .sorted { a, b -> a.toString().compareTo(b.toString()) }
          .forEach { file ->
            update(digest, file.toString())
            val bytes = java.nio.file.Files.readAllBytes(file)
            digest.update(bytes)
          }
      }
      return
    }

    digest.update(java.nio.file.Files.readAllBytes(path))
  }

  private fun update(
    digest: java.security.MessageDigest,
    value: String
  ) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    digest.update(bytes)
    digest.update(0)
  }
}

object TaskUpToDate {
  fun isUpToDate(task: BuildTask): Boolean =
    TaskFingerprints.isUpToDate(task, null)
}
