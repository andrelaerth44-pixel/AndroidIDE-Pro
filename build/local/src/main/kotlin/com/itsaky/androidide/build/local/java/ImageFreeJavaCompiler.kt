/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.java

import org.eclipse.jdt.internal.compiler.Compiler
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies
import org.eclipse.jdt.internal.compiler.ICompilerRequestor
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader
import org.eclipse.jdt.internal.compiler.env.INameEnvironment
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.zip.ZipFile

/**
 * ART-safe ECJ backend for Java 9+ against Android platform jars.
 *
 * ART does not expose a JRT image that ECJ can read. A classic INameEnvironment lets ECJ resolve
 * java/android classes directly from android.jar and output directories without -bootclasspath/JRT.
 */
internal object ImageFreeJavaCompiler {

  fun compile(
    sources: List<Path>,
    libraries: List<Path>,
    outputDir: Path,
    sourceLevel: String,
  ): JdtJavaCompiler.Result {
    val jars = libraries.filter(Files::isRegularFile).mapNotNull {
      runCatching { ZipFile(it.toFile()) }.getOrNull()
    }
    val dirs = libraries.filter(Files::isDirectory)
    try {
      val packages = HashSet<String>()
      fun addPackages(binaryName: String) {
        val slash = binaryName.lastIndexOf('/')
        if (slash <= 0) return
        val pkg = binaryName.substring(0, slash)
        var start = 0
        while (true) {
          val next = pkg.indexOf('/', start)
          if (next < 0) {
            packages += pkg
            break
          }
          packages += pkg.substring(0, next)
          start = next + 1
        }
      }

      for (jar in jars) {
        val entries = jar.entries()
        while (entries.hasMoreElements()) {
          val name = entries.nextElement().name
          if (name.endsWith(".class")) addPackages(name)
        }
      }
      for (dir in dirs) {
        Files.walk(dir).use { stream ->
          stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".class") }.forEach { file ->
            addPackages(dir.relativize(file).toString().replace('\\', '/'))
          }
        }
      }

      val environment = object : INameEnvironment {
        override fun findType(compoundTypeName: Array<CharArray>): NameEnvironmentAnswer? =
          find(compoundTypeName.joinToString("/") { String(it) })

        override fun findType(typeName: CharArray, packageName: Array<CharArray>): NameEnvironmentAnswer? =
          find(if (packageName.isEmpty()) String(typeName) else packageName.joinToString("/") { String(it) } + "/" + String(typeName))

        override fun isPackage(parentPackageName: Array<CharArray>?, packageName: CharArray): Boolean {
          val parent = parentPackageName ?: emptyArray()
          val prefix = if (parent.isEmpty()) String(packageName) else parent.joinToString("/") { String(it) } + "/" + String(packageName)
          return packages.contains(prefix)
        }

        override fun cleanup() = Unit

        private fun find(binaryName: String): NameEnvironmentAnswer? {
          val entryName = "$binaryName.class"
          for (dir in dirs) {
            val file = dir.resolve(entryName)
            if (!Files.isRegularFile(file)) continue
            val bytes = runCatching { Files.readAllBytes(file) }.getOrNull() ?: continue
            val reader = runCatching { ClassFileReader.read(bytes, entryName) }.getOrNull() ?: continue
            return NameEnvironmentAnswer(reader, null)
          }
          for (jar in jars) {
            val entry = jar.getEntry(entryName) ?: continue
            val bytes = runCatching { jar.getInputStream(entry).use { it.readBytes() } }.getOrNull() ?: continue
            val reader = runCatching { ClassFileReader.read(bytes, entryName) }.getOrNull() ?: continue
            return NameEnvironmentAnswer(reader, null)
          }
          return null
        }
      }

      val level = classFileLevel(sourceLevel)
      val options = CompilerOptions().apply {
        complianceLevel = level
        sourceLevel = level
        targetJDK = level
        produceDebugAttributes = ClassFileConstants.ATTR_SOURCE or
          ClassFileConstants.ATTR_LINES or
          ClassFileConstants.ATTR_VARS
      }

      val diagnostics = ArrayList<JdtJavaCompiler.Diagnostic>()
      val messages = ArrayList<String>()
      var hadError = false
      val requestor = ICompilerRequestor { result ->
        result.allProblems?.forEach { problem ->
          if (problem != null && problem.isError) {
            hadError = true
            val file = runCatching { Path.of(String(problem.originatingFileName)) }.getOrNull()
            diagnostics += JdtJavaCompiler.Diagnostic(
              file = file,
              line = problem.sourceLineNumber.takeIf { it > 0 },
              column = problem.sourceStart + 1,
              message = problem.message,
              error = true,
            )
            messages += "${file ?: "<unknown>"}:${problem.sourceLineNumber}: error: ${problem.message}"
          }
        }

        if (!result.hasErrors()) {
          for (classFile in result.classFiles) {
            val destination = outputDir.resolve(String(classFile.fileName()) + ".class")
            runCatching {
              Files.createDirectories(destination.parent)
              Files.write(destination, classFile.bytes)
            }.onFailure {
              hadError = true
              messages += "${destination}: error: ${it.message}"
              diagnostics += JdtJavaCompiler.Diagnostic(
                file = destination,
                line = null,
                message = it.message ?: "Failed to write class file",
                error = true,
              )
            }
          }
        }
      }

      val units = sources.filter(Files::isRegularFile).map { source ->
        CompilationUnit(Files.readAllBytes(source).toString(Charsets.UTF_8).toCharArray(), source.toString(), "UTF-8")
      }

      return runCatching {
        Compiler(
          environment,
          DefaultErrorHandlingPolicies.proceedWithAllProblems(),
          options,
          requestor,
          DefaultProblemFactory(Locale.getDefault()),
        ).compile(units.toTypedArray())
        JdtJavaCompiler.Result(!hadError, diagnostics, messages)
      }.getOrElse { error ->
        val diagnostic = JdtJavaCompiler.Diagnostic(
          file = null,
          line = null,
          message = "internal compile failed: ${error.message}",
          error = true,
        )
        JdtJavaCompiler.Result(false, diagnostics + diagnostic, messages + diagnostic.message)
      }
    } finally {
      jars.forEach { runCatching { it.close() } }
    }
  }

  private fun classFileLevel(sourceLevel: String): Long = when (
    sourceLevel.removePrefix("1.").takeWhile(Char::isDigit).toIntOrNull() ?: 17
  ) {
    8 -> ClassFileConstants.JDK1_8
    11 -> ClassFileConstants.JDK11
    else -> ClassFileConstants.JDK17
  }
}
