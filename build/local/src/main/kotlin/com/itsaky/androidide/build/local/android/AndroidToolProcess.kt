/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import java.nio.file.Path
import java.util.concurrent.TimeUnit

data class AndroidToolResult(
  val success: Boolean,
  val log: List<String> = emptyList(),
  val exitCode: Int? = null,
  val message: String? = null,
)

/**
 * Small subprocess primitive shared by native Android build tools.
 *
 * It deliberately does not know Gradle and does not create a persistent daemon. Each invocation has a
 * bounded lifetime and returns a plain transcript that the task layer turns into BuildDiagnostic entries.
 */
object AndroidToolProcess {

  fun run(
    command: List<String>,
    environment: Map<String, String> = emptyMap(),
    workingDirectory: Path? = null,
    timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
  ): AndroidToolResult {
    require(command.isNotEmpty()) { "Tool command cannot be empty" }

    return try {
      val builder = ProcessBuilder(command).redirectErrorStream(true)
      workingDirectory?.let { builder.directory(it.toFile()) }
      if (environment.isNotEmpty()) builder.environment().putAll(environment)

      val process = builder.start()
      val lines = java.util.Collections.synchronizedList(ArrayList<String>())
      val readerThread = Thread {
        runCatching {
          process.inputStream.bufferedReader().useLines { sequence ->
            sequence.forEach(lines::add)
          }
        }
      }.apply {
        isDaemon = true
        start()
      }

      val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
      if (!finished) {
        process.destroyForcibly()
        process.waitFor(2, TimeUnit.SECONDS)
        readerThread.join(2_000)
        return AndroidToolResult(
          success = false,
          log = lines.toList(),
          message = "Android tool timed out after ${timeoutSeconds}s",
        )
      }

      readerThread.join(2_000)
      val exitCode = process.exitValue()
      AndroidToolResult(
        success = exitCode == 0,
        log = lines.toList(),
        exitCode = exitCode,
        message = if (exitCode == 0) null else "Android tool exited with code $exitCode",
      )
    } catch (error: Throwable) {
      AndroidToolResult(
        success = false,
        message = error.message ?: error::class.java.simpleName,
      )
    }
  }

  private const val DEFAULT_TIMEOUT_SECONDS = 300L
}
