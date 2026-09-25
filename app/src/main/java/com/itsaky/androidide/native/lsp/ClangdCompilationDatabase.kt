package com.itsaky.androidide.native.lsp

import com.google.gson.GsonBuilder
import com.itsaky.androidide.native.build.NativeCommandSpec
import java.io.File

data class ClangdCompilationEntry(
  val directory: String,
  val file: String,
  val arguments: List<String>,
)

object ClangdCompilationDatabase {

  private val gson = GsonBuilder().setPrettyPrinting().create()

  fun fromCommands(commands: List<NativeCommandSpec>): List<ClangdCompilationEntry> =
    commands.mapNotNull { command ->
      val source =
        command.arguments
          .windowed(2, 1)
          .firstOrNull { it.first() == "-c" }
          ?.last()
          ?.let(::File)
          ?: return@mapNotNull null

      ClangdCompilationEntry(
        directory =
          (command.workingDirectory ?: source.parentFile ?: File(".")).absolutePath,
        file = source.absolutePath,
        arguments = command.asCommandLine(),
      )
    }

  fun write(
    outputDirectory: File,
    commands: List<NativeCommandSpec>,
  ): File {
    outputDirectory.mkdirs()
    val output = File(outputDirectory, "compile_commands.json")
    output.writeText(gson.toJson(fromCommands(commands)))
    return output
  }
}
