/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.java

import org.eclipse.jdt.core.compiler.CategorizedProblem
import org.eclipse.jdt.internal.compiler.CompilationResult
import org.eclipse.jdt.internal.compiler.ICompilerRequestor
import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path

/**
 * Java compiler backend for the native Android build engine.
 *
 * This deliberately exposes only Paths and plain result data to the build layer. JDT stays an implementation
 * detail so the task graph does not become coupled to compiler internals.
 */
object JdtJavaCompiler {

  data class Request(
    val sources: List<Path>,
    val classpath: List<Path>,
    val outputDir: Path,
    val sourceLevel: String = "17",
    val targetLevel: String = sourceLevel,
    val bootClasspath: List<Path> = emptyList(),
  )

  data class Diagnostic(
    val file: Path?,
    val line: Int?,
    val column: Int = -1,
    val message: String,
    val error: Boolean,
  )

  data class Result(
    val success: Boolean,
    val diagnostics: List<Diagnostic>,
    val messages: List<String>,
  )

  fun compile(request: Request): Result {
    Files.createDirectories(request.outputDir)
    val sources = request.sources.filter(Files::isRegularFile)
    if (sources.isEmpty()) return Result(true, emptyList(), emptyList())

    if (request.bootClasspath.isNotEmpty() && complianceAtLeast9(request.sourceLevel)) {
      return ImageFreeJavaCompiler.compile(
        sources = sources,
        libraries = request.bootClasspath + request.classpath,
        outputDir = request.outputDir,
        sourceLevel = request.sourceLevel,
      )
    }

    val out = StringWriter()
    val err = StringWriter()
    val main = RecordingMain(PrintWriter(out), PrintWriter(err))
    val args = ArrayList<String>()
    args += listOf("-source", request.sourceLevel, "-target", request.targetLevel)
    args += listOf("-proc:none", "-g", "-d", request.outputDir.toString())
    if (request.bootClasspath.isNotEmpty()) {
      args += "-bootclasspath"
      args += request.bootClasspath.joinToString(File.pathSeparator) { it.toString() }
    }
    if (request.classpath.isNotEmpty()) {
      args += "-classpath"
      args += request.classpath.joinToString(File.pathSeparator) { it.toString() }
    }
    args += sources.map(Path::toString)

    val ok = runCatching { main.compile(quoteArguments(args)) }.getOrDefault(false)
    val transcript = (err.toString() + "\n" + out.toString())
      .lineSequence()
      .filter(String::isNotBlank)
      .toList()
    val diagnostics = main.problems.ifEmpty { parseTextDiagnostics(transcript) }
    return Result(ok && diagnostics.none(Diagnostic::error), diagnostics, transcript)
  }

  private fun complianceAtLeast9(level: String): Boolean {
    val number = level.removePrefix("1.").takeWhile(Char::isDigit).toIntOrNull() ?: return false
    return number >= 9
  }

  private class RecordingMain(out: PrintWriter, err: PrintWriter) : Main(out, err, false) {
    val problems = ArrayList<Diagnostic>()

    override fun getBatchRequestor(): ICompilerRequestor {
      val delegate = super.getBatchRequestor()
      return ICompilerRequestor { result: CompilationResult? ->
        if (result != null) {
          result.allProblems?.forEach { problem ->
            if (problem != null) problems += toDiagnostic(problem, result)
          }
        }
        delegate.acceptResult(result)
      }
    }
  }

  private fun toDiagnostic(problem: CategorizedProblem, result: CompilationResult): Diagnostic {
    val path = runCatching { Path.of(String(problem.originatingFileName)) }.getOrNull()
    val line = problem.sourceLineNumber.takeIf { it > 0 }
    val column = if (problem.sourceStart >= 0) {
      val separators = result.lineSeparatorPositions
      if (line == null || line <= 1 || separators == null) problem.sourceStart + 1
      else {
        val previous = line - 2
        if (previous in separators.indices) (problem.sourceStart - separators[previous]).coerceAtLeast(1) else -1
      }
    } else -1
    return Diagnostic(path, line, column, problem.message, problem.isError)
  }

  private fun parseTextDiagnostics(lines: List<String>): List<Diagnostic> {
    val header = Regex("^\\d+\\.\\s+(ERROR|WARNING)\\s+in\\s+(.+?)\\s+\\(at line (\\d+)\\)$")
    val result = ArrayList<Diagnostic>()
    var current: Diagnostic? = null
    val message = StringBuilder()

    fun flush() {
      val base = current ?: return
      result += base.copy(message = message.toString().ifBlank { base.message })
      current = null
      message.setLength(0)
    }

    for (raw in lines) {
      val line = raw.trim()
      val match = header.matchEntire(line)
      if (match != null) {
        flush()
        current = Diagnostic(
          file = runCatching { Path.of(match.groupValues[2]) }.getOrNull(),
          line = match.groupValues[3].toIntOrNull(),
          message = if (match.groupValues[1] == "ERROR") "error" else "warning",
          error = match.groupValues[1] == "ERROR",
        )
        continue
      }
      if (current != null && line.isNotEmpty() && !line.startsWith("---")) {
        if (message.isNotEmpty()) message.append(' ')
        message.append(line)
      }
    }
    flush()
    return result
  }

  /** JDT batch compiler accepts one shell-like command string. */
  private fun quoteArguments(arguments: List<String>): String =
    arguments.joinToString(" ") { argument ->
      if (argument.none { it.isWhitespace() || it == '"' }) argument
      else "\"${argument.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}
