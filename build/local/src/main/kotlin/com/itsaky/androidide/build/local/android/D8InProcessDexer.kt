/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.Diagnostic
import com.android.tools.r8.OutputMode
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

/**
 * Runs D8 in the AndroidIDE process without Gradle.
 *
 * The in-process path is deliberately bounded by [threads]. The final merge can later be routed to a separate
 * VM backend when the device has enough memory; the task API remains unchanged.
 */
class D8InProcessDexer : Dexer {

  override fun dexArchive(
    inputs: List<Path>,
    classpath: List<Path>,
    androidJar: Path,
    minApi: Int,
    release: Boolean,
    outDir: Path,
    threads: Int,
  ): AndroidToolResult = run(
    inputs = inputs,
    classpath = classpath,
    androidJar = androidJar,
    minApi = minApi,
    release = release,
    outDir = outDir,
    outputMode = OutputMode.DexFilePerClassFile,
    intermediate = true,
    threads = threads,
  )

  override fun dex(
    inputs: List<Path>,
    androidJar: Path,
    minApi: Int,
    release: Boolean,
    outDir: Path,
    threads: Int,
  ): AndroidToolResult = run(
    inputs = inputs,
    classpath = emptyList(),
    androidJar = androidJar,
    minApi = minApi,
    release = release,
    outDir = outDir,
    outputMode = OutputMode.DexIndexed,
    intermediate = false,
    threads = threads,
  )

  private fun run(
    inputs: List<Path>,
    classpath: List<Path>,
    androidJar: Path,
    minApi: Int,
    release: Boolean,
    outDir: Path,
    outputMode: OutputMode,
    intermediate: Boolean,
    threads: Int,
  ): AndroidToolResult {
    Files.createDirectories(outDir)
    val programs = inputs.filter(Files::exists)
    if (programs.isEmpty()) return AndroidToolResult(false, message = "No D8 program inputs")

    val collector = RecordingDiagnostics()
    return try {
      val builder = D8Command.builder(collector)
        .addProgramFiles(programs)
        .setMinApiLevel(minApi)
        .setMode(if (release) CompilationMode.RELEASE else CompilationMode.DEBUG)
        .setOutput(outDir, outputMode)

      if (intermediate) builder.setIntermediate(true)
      classpath.filter(Files::exists).forEach(builder::addClasspathFiles)
      if (Files.exists(androidJar)) builder.addLibraryFiles(androidJar)

      val poolSize = threads.coerceIn(1, Runtime.getRuntime().availableProcessors().coerceAtLeast(1))
      if (poolSize == 1) {
        D8.run(builder.build())
      } else {
        Executors.newFixedThreadPool(poolSize).use { executor ->
          D8.run(builder.build(), executor)
          executor.shutdown()
        }
      }

      AndroidToolResult(
        success = collector.errorCount == 0,
        log = collector.lines,
      )
    } catch (error: Throwable) {
      AndroidToolResult(
        success = false,
        log = collector.lines + "D8 failed: ${error.message}",
        message = error.message ?: error::class.java.simpleName,
      )
    }
  }

  private class RecordingDiagnostics : DiagnosticsHandler {
    val lines = ArrayList<String>()
    var errorCount = 0
      private set

    override fun info(diagnostic: Diagnostic) {
      lines += "info: ${diagnostic.diagnosticMessage}"
    }

    override fun warning(diagnostic: Diagnostic) {
      lines += "warning: ${diagnostic.diagnosticMessage}"
    }

    override fun error(diagnostic: Diagnostic) {
      errorCount++
      lines += "error: ${diagnostic.diagnosticMessage}"
    }
  }
}
