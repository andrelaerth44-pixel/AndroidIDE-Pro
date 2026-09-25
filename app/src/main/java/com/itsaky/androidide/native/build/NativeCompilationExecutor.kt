package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.NativeSourceSet
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File

class NativeCompilationExecutor(
  private val toolchain: NativeToolchain,
  private val androidApiLevel: Int,
  private val commandExecutor: (
    command: NativeCommandSpec,
    onOutput: (String) -> Unit,
  ) -> NativeProcessResult = { command, onOutput ->
    NativeCommandExecutor.execute(command, onOutput = onOutput)
  },
) {

  fun compile(
    sourceRoot: File,
    sourceSet: NativeSourceSet,
    buildDirectory: File,
    abi: AbiTarget,
    onOutput: (String) -> Unit = {},
  ): NativeCompilationResult {
    val factory = NativeCommandFactory(toolchain, androidApiLevel)
    val plan =
      NativeCommandPlanner.planCompilation(
        sourceRoot = sourceRoot,
        sourceSet = sourceSet,
        buildDirectory = buildDirectory,
        abi = abi,
        factory = factory,
      )

    val commands = plan.cCommands + plan.cppCommands
    buildDirectory.mkdirs()

    commands.forEachIndexed { index, command ->
      command.outputFile().parentFile?.mkdirs()
      val result = commandExecutor(command, onOutput)
      if (!result.success) {
        return NativeCompilationResult(
          success = false,
          completedCommands = index,
          failedCommand = command,
          processResult = result,
        )
      }
    }

    return NativeCompilationResult(
      success = true,
      completedCommands = commands.size,
    )
  }

  private fun NativeCommandSpec.outputFile(): File {
    val outputIndex = arguments.indexOf("-o")
    require(outputIndex >= 0 && outputIndex + 1 < arguments.size) {
      "Native compiler command does not contain an output path"
    }
    return File(arguments[outputIndex + 1])
  }
}
