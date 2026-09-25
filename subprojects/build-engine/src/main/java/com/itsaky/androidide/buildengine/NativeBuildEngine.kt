package com.itsaky.androidide.buildengine

import java.io.File
import java.security.MessageDigest
import java.util.Properties
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

@JvmInline
value class BuildTaskId(val value: String)

enum class BuildTaskState {
  PENDING,
  RUNNING,
  SUCCESS,
  UP_TO_DATE,
  FAILED,
  BLOCKED,
}

data class BuildTaskResult(
  val state: BuildTaskState,
  val inputFingerprint: String,
  val message: String? = null,
  val outputFingerprint: String? = null,
)

data class BuildContext(
  val projectDir: File,
  val buildDir: File,
  val environment: Map<String, String> = emptyMap(),
)

interface BuildTask {
  val id: BuildTaskId
  val dependsOn: Set<BuildTaskId>
    get() = emptySet()

  fun inputs(context: BuildContext): Map<String, String> = emptyMap()

  fun execute(context: BuildContext): BuildTaskExecution
}

data class BuildTaskExecution(
  val message: String? = null,
  val outputFingerprint: String? = null,
)

data class BuildProgress(
  val task: BuildTaskId,
  val state: BuildTaskState,
  val message: String? = null,
)

fun interface BuildProgressListener {
  fun onProgress(progress: BuildProgress)
}

interface BuildStateStore {
  fun get(task: BuildTaskId): String?
  fun put(task: BuildTaskId, fingerprint: String)
}

class FileBuildStateStore(
  private val stateFile: File,
) : BuildStateStore {

  private val properties = Properties()

  init {
    if (stateFile.isFile) {
      stateFile.inputStream().use(properties::load)
    }
  }

  @Synchronized
  override fun get(task: BuildTaskId): String? = properties.getProperty(task.value)

  @Synchronized
  override fun put(task: BuildTaskId, fingerprint: String) {
    stateFile.parentFile?.mkdirs()
    properties.setProperty(task.value, fingerprint)
    stateFile.outputStream().use { properties.store(it, "AndroidIDE Native Build Engine") }
  }
}

class TaskGraph(
  tasks: Collection<BuildTask>,
) {
  private val byId = tasks.associateBy { it.id }

  init {
    require(byId.size == tasks.size) { "Duplicate build task id" }
    tasks.forEach { task ->
      task.dependsOn.forEach { dependency ->
        require(byId.containsKey(dependency)) {
          "Unknown dependency $dependency for ${task.id}"
        }
      }
    }
  }

  fun tasks(): Collection<BuildTask> = byId.values

  fun topologicalLevels(): List<List<BuildTask>> {
    val remaining = byId.toMutableMap()
    val completed = mutableSetOf<BuildTaskId>()
    val levels = mutableListOf<List<BuildTask>>()

    while (remaining.isNotEmpty()) {
      val ready =
        remaining.values.filter { task ->
          task.dependsOn.all(completed::contains)
        }

      require(ready.isNotEmpty()) {
        "Build task graph contains a dependency cycle"
      }

      levels += ready
      ready.forEach { task ->
        remaining.remove(task.id)
        completed += task.id
      }
    }

    return levels
  }
}

class NativeBuildEngine(
  private val maxParallelTasks: Int =
    (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1),
) {

  fun build(
    tasks: Collection<BuildTask>,
    context: BuildContext,
    stateStore: BuildStateStore = FileBuildStateStore(
      File(context.buildDir, ".androidide/build-state.properties"),
    ),
    listener: BuildProgressListener? = null,
  ): Map<BuildTaskId, BuildTaskResult> {
    val graph = TaskGraph(tasks)
    val results = linkedMapOf<BuildTaskId, BuildTaskResult>()
    val executor = Executors.newFixedThreadPool(maxParallelTasks)

    try {
      for (level in graph.topologicalLevels()) {
        val futures = mutableListOf<Pair<BuildTask, Future<BuildTaskResult>>>()

        for (task in level) {
          val blocked = task.dependsOn.any {
            results[it]?.state in setOf(BuildTaskState.FAILED, BuildTaskState.BLOCKED)
          }

          if (blocked) {
            val result =
              BuildTaskResult(
                state = BuildTaskState.BLOCKED,
                inputFingerprint = "",
                message = "Blocked by a failed dependency",
              )
            results[task.id] = result
            listener?.onProgress(BuildProgress(task.id, result.state, result.message))
            continue
          }

          val fingerprint = fingerprint(task, context)
          if (stateStore.get(task.id) == fingerprint) {
            val result =
              BuildTaskResult(
                state = BuildTaskState.UP_TO_DATE,
                inputFingerprint = fingerprint,
                message = "Inputs unchanged",
              )
            results[task.id] = result
            listener?.onProgress(BuildProgress(task.id, result.state, result.message))
            continue
          }

          listener?.onProgress(BuildProgress(task.id, BuildTaskState.RUNNING))
          futures += task to executor.submit(
            Callable {
              runTask(task, context, fingerprint, stateStore, listener)
            },
          )
        }

        for ((task, future) in futures) {
          val result =
            try {
              future.get()
            } catch (error: Throwable) {
              BuildTaskResult(
                state = BuildTaskState.FAILED,
                inputFingerprint = fingerprint(task, context),
                message = error.cause?.message ?: error.message,
              )
            }

          results[task.id] = result
        }
      }
    } finally {
      executor.shutdown()
    }

    return results
  }

  private fun runTask(
    task: BuildTask,
    context: BuildContext,
    fingerprint: String,
    stateStore: BuildStateStore,
    listener: BuildProgressListener?,
  ): BuildTaskResult {
    return try {
      val execution = task.execute(context)
      val result =
        BuildTaskResult(
          state = BuildTaskState.SUCCESS,
          inputFingerprint = fingerprint,
          message = execution.message,
          outputFingerprint = execution.outputFingerprint,
        )
      stateStore.put(task.id, fingerprint)
      listener?.onProgress(BuildProgress(task.id, result.state, result.message))
      result
    } catch (error: Throwable) {
      val result =
        BuildTaskResult(
          state = BuildTaskState.FAILED,
          inputFingerprint = fingerprint,
          message = error.message ?: error.javaClass.simpleName,
        )
      listener?.onProgress(BuildProgress(task.id, result.state, result.message))
      result
    }
  }

  private fun fingerprint(
    task: BuildTask,
    context: BuildContext,
  ): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(task.id.value.toByteArray())
    task.inputs(context)
      .toSortedMap()
      .forEach { (key, value) ->
        digest.update(key.toByteArray())
        digest.update(0)
        digest.update(value.toByteArray())
        digest.update(0)
      }
    return digest.digest().toHex()
  }

  private fun ByteArray.toHex(): String =
    joinToString("") { byte -> "%02x".format(byte) }
}

object AndroidBuildTaskIds {
  val PREPARE = BuildTaskId("android.prepare")
  val GENERATE_SOURCES = BuildTaskId("android.generateSources")
  val PROCESS_MANIFEST = BuildTaskId("android.processManifest")
  val COMPILE_RESOURCES = BuildTaskId("android.compileResources")
  val LINK_RESOURCES = BuildTaskId("android.linkResources")
  val COMPILE_KOTLIN = BuildTaskId("android.compileKotlin")
  val COMPILE_JAVA = BuildTaskId("android.compileJava")
  val DEX = BuildTaskId("android.dex")
  val PACKAGE_APK = BuildTaskId("android.packageApk")
  val SIGN_APK = BuildTaskId("android.signApk")
}
