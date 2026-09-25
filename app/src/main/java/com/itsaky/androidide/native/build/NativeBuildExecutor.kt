package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.NativeBuildRequest
import com.itsaky.androidide.native.model.NativeLibraryType
import com.itsaky.androidide.toolchain.NativeToolchain
import com.itsaky.androidide.utils.Environment
import java.io.File

enum class NativeBuildTaskState {
  RUNNING,
  SUCCESS,
  FAILED,
}

class NativeBuildExecutor(
  private val toolchain: NativeToolchain,
  private val androidApiLevel: Int,
  private val commandExecutor: (
    command: NativeCommandSpec,
    controller: NativeProcessController?,
    onOutput: (String) -> Unit,
  ) -> NativeProcessResult = { command, controller, onOutput ->
    NativeCommandExecutor.execute(
      command = command,
      environment = Environment.getEnvironment(),
      controller = controller,
      onOutput = onOutput,
    )
  },
) {

  fun execute(
    request: NativeBuildRequest,
    moduleRoot: File,
    buildDirectory: File = File(
      moduleRoot,
      ".androidide/native/" + request.variant.name.lowercase() + "/" + request.abi.name.lowercase(),
    ),
    onTaskState: (NativeBuildTask, NativeBuildTaskState) -> Unit = { _, _ -> },
    onOutput: (String) -> Unit = {},
    processController: NativeProcessController? = null,
  ): NativeBuildResult {
    val target =
      requireNotNull(
        request.module.targets.firstOrNull {
          it.abi == request.abi && it.variant == request.variant
        }
      ) {
        "Requested native target is not present in the module"
      }

    val sourceRoot = File(moduleRoot, "src/main")
    require(sourceRoot.isDirectory) {
      "Native source root does not exist: " + sourceRoot.absolutePath
    }

    val graph = NativePipeline.createGraph(request)
    val factory = NativeCommandFactory(toolchain, androidApiLevel)
    val objectFiles =
      NativeCommandPlanner.objectFiles(
        sourceRoot = sourceRoot,
        sourceSet = target.sourceSet,
        buildDirectory = buildDirectory,
        abi = request.abi,
      )

    val executed = mutableListOf<String>()

    fun fail(task: NativeBuildTask, message: String, result: NativeProcessResult? = null): NativeBuildResult {
      onTaskState(task, NativeBuildTaskState.FAILED)
      return NativeBuildResult(
        success = false,
        executedTasks = executed,
        failedTaskId = task.id,
        message = message,
        outputFile = result?.let { null },
      )
    }

    for (task in graph.topologicalOrder()) {
      onTaskState(task, NativeBuildTaskState.RUNNING)

      when (task.kind) {
        NativeBuildTask.Kind.SCAN_SOURCES -> {
          if (target.sourceSet.cSources.isEmpty() && target.sourceSet.cppSources.isEmpty()) {
            return fail(task, "No native C/C++ sources were found")
          }
        }

        NativeBuildTask.Kind.GENERATE_JNI_HEADERS -> {
          // Header generation will be supplied by the JNI project model.
          // Keeping the stage explicit lets the graph remain stable.
        }

        NativeBuildTask.Kind.COMPILE_C -> {
          val commands =
            NativeCommandPlanner.planC(
              sourceRoot = sourceRoot,
              sourceSet = target.sourceSet,
              buildDirectory = buildDirectory,
              abi = request.abi,
              factory = factory,
            )

          val result = executeCommands(task.id, commands, executed, onOutput, processController)
          if (result != null) {
            onTaskState(task, NativeBuildTaskState.FAILED)
            return result
          }
        }

        NativeBuildTask.Kind.COMPILE_CPP -> {
          val commands =
            NativeCommandPlanner.planCpp(
              sourceRoot = sourceRoot,
              sourceSet = target.sourceSet,
              buildDirectory = buildDirectory,
              abi = request.abi,
              factory = factory,
            )

          val result = executeCommands(task.id, commands, executed, onOutput)
          if (result != null) {
            onTaskState(task, NativeBuildTaskState.FAILED)
            return result
          }
        }

        NativeBuildTask.Kind.ARCHIVE_OBJECTS -> {
          val output = nativeOutputFile(buildDirectory, request, NativeLibraryType.STATIC)
          output.parentFile?.mkdirs()
          val command = factory.archiveObjects(objectFiles, output)
          val result = commandExecutor(command, processController, onOutput)
          if (!result.success) {
            return fail(task, "Static archive failed", result)
          }
          executed += task.id
        }

        NativeBuildTask.Kind.LINK_NATIVE -> {
          val output = nativeOutputFile(buildDirectory, request, NativeLibraryType.SHARED)
          output.parentFile?.mkdirs()
          val command = factory.linkShared(request.abi, objectFiles, output)
          val result = commandExecutor(command, processController, onOutput)
          if (!result.success) {
            return fail(task, "Shared library link failed", result)
          }
          executed += task.id
        }

        NativeBuildTask.Kind.PACKAGE_NATIVE_LIBS -> {
          val type =
            if (request.module.targets.any { it.libraryType == NativeLibraryType.SHARED }) {
              NativeLibraryType.SHARED
            } else {
              NativeLibraryType.STATIC
            }

          val output = nativeOutputFile(buildDirectory, request, type)
          if (!output.isFile) {
            return fail(task, "Native output was not produced: " + output.absolutePath)
          }
          executed += task.id
          onTaskState(task, NativeBuildTaskState.SUCCESS)

          return NativeBuildResult(
            success = true,
            executedTasks = executed,
            message = "Native build completed",
            outputFile = output,
          )
        }
      }

      onTaskState(task, NativeBuildTaskState.SUCCESS)
      executed += task.id
    }

    return NativeBuildResult(
      success = false,
      executedTasks = executed,
      message = "Native build graph finished without a packaged output",
    )
  }

  private fun executeCommands(
    taskId: String,
    commands: List<NativeCommandSpec>,
    executed: MutableList<String>,
    onOutput: (String) -> Unit,
    processController: NativeProcessController?,
  ): NativeBuildResult? {
    for (command in commands) {
      command.arguments
        .windowed(2, 1)
        .firstOrNull { it.first() == "-o" }
        ?.last()
        ?.let { File(it).parentFile?.mkdirs() }

      val result = commandExecutor(command, processController, onOutput)
      if (!result.success) {
        return NativeBuildResult(
          success = false,
          executedTasks = executed,
          failedTaskId = taskId,
          message = "Native compile command failed with exit code " + result.exitCode,
        )
      }
    }

    return null
  }

  private fun nativeOutputFile(
    buildDirectory: File,
    request: NativeBuildRequest,
    type: NativeLibraryType,
  ): File {
    val extension =
      if (type == NativeLibraryType.SHARED) {
        ".so"
      } else {
        ".a"
      }

    val prefix =
      if (type == NativeLibraryType.SHARED) {
        "lib"
      } else {
        "lib"
      }

    return File(
      buildDirectory,
      "libs/" + request.abi.name.lowercase() + "/" +
        prefix + request.module.moduleName + extension,
    )
  }
}
