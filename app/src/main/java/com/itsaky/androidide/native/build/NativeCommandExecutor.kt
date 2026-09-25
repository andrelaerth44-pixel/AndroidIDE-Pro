package com.itsaky.androidide.native.build

object NativeCommandExecutor {

  fun execute(
    command: NativeCommandSpec,
    environment: Map<String, String> = emptyMap(),
    onOutput: (String) -> Unit = {},
  ): NativeProcessResult {
    val startedAt = System.currentTimeMillis()
    val processBuilder =
      ProcessBuilder(command.asCommandLine())
        .redirectErrorStream(true)

    command.workingDirectory?.let {
      processBuilder.directory(it)
    }

    processBuilder.environment().putAll(environment)

    val process = processBuilder.start()
    val output = StringBuilder()

    process.inputStream.bufferedReader().useLines { lines ->
      lines.forEach { line ->
        output.append(line).append('\n')
        onOutput(line)
      }
    }

    val exitCode = process.waitFor()

    return NativeProcessResult(
      exitCode = exitCode,
      output = output.toString(),
      durationMs = System.currentTimeMillis() - startedAt,
    )
  }
}
