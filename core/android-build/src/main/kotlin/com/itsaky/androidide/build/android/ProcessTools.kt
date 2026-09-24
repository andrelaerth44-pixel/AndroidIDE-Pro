package com.itsaky.androidide.build.android

import java.nio.file.Files
import java.nio.file.Path

object ProcessTools {
  fun run(
    executable: Path,
    args: List<String>,
    workingDirectory: Path? = null,
    environment: Map<String, String> = emptyMap(),
    logger: (String) -> Unit = ::println
  ) {
    require(Files.exists(executable)) {
      "Tool not found: " + executable
    }

    val command = buildList {
      add(executable.toString())
      addAll(args)
    }

    logger(command.joinToString(" "))
    val processBuilder = ProcessBuilder(command).redirectErrorStream(true)

    if (workingDirectory != null) {
      processBuilder.directory(workingDirectory.toFile())
    }

    processBuilder.environment().putAll(environment)

    val process = processBuilder.start()
    process.inputStream.bufferedReader().useLines { lines ->
      lines.forEach(logger)
    }

    val exitCode = process.waitFor()
    check(exitCode == 0) {
      executable.fileName.toString() + " exited with code " + exitCode
    }
  }
}
