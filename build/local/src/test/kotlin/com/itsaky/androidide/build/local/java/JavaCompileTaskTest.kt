/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.java

import com.itsaky.androidide.build.api.BuildDiagnostic
import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.TaskContext
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JavaCompileTaskTest {

  @Test
  fun compilesHelloWorldWithoutGradle() {
    val root = Files.createTempDirectory("androidide-java-build")
    try {
      val source = root.resolve("Hello.java")
      val output = root.resolve("classes")
      Files.writeString(
        source,
        "public class Hello { public static void main(String[] args) { System.out.println(\"hello\"); } }",
      )

      val diagnostics = mutableListOf<BuildDiagnostic>()
      val context = TaskContext(
        projectDir = root,
        cacheDir = root.resolve("cache"),
        toolchainsDir = root.resolve("toolchains"),
        environment = emptyMap(),
        cancellation = com.itsaky.androidide.build.api.CancellationToken.NONE,
        diagnostics = com.itsaky.androidide.build.api.BuildDiagnosticSink { diagnostics += it },
      )
      val result = JavaCompileTask(
        id = ":app:compileJavaDebug",
        sources = listOf(source),
        classpath = emptyList(),
        outputDir = output,
      ).execute(context)

      assertEquals(com.itsaky.androidide.build.api.TaskResult.State.SUCCESS, result.state)
      assertTrue(Files.exists(output.resolve("Hello.class")))
      assertTrue(diagnostics.none { it.severity == BuildDiagnostic.Severity.ERROR })
    } finally {
      deleteRecursively(root)
    }
  }

  @Test
  fun returnsStructuredCompilerDiagnostic() {
    val root = Files.createTempDirectory("androidide-java-build-error")
    try {
      val source = root.resolve("Broken.java")
      val output = root.resolve("classes")
      Files.writeString(source, "public class Broken { public void broken( { }")

      val diagnostics = mutableListOf<BuildDiagnostic>()
      val context = TaskContext(
        projectDir = root,
        cacheDir = root.resolve("cache"),
        toolchainsDir = root.resolve("toolchains"),
        environment = emptyMap(),
        cancellation = com.itsaky.androidide.build.api.CancellationToken.NONE,
        diagnostics = com.itsaky.androidide.build.api.BuildDiagnosticSink { diagnostics += it },
      )
      val result = JavaCompileTask(
        id = ":app:compileJavaDebug",
        sources = listOf(source),
        classpath = emptyList(),
        outputDir = output,
      ).execute(context)

      assertEquals(com.itsaky.androidide.build.api.TaskResult.State.FAILED, result.state)
      assertTrue(diagnostics.any {
        it.kind == BuildDiagnostic.Kind.COMPILER &&
          it.severity == BuildDiagnostic.Severity.ERROR &&
          it.source == source
      })
    } finally {
      deleteRecursively(root)
    }
  }

  private fun deleteRecursively(root: Path) {
    if (!Files.exists(root)) return
    Files.walk(root).use { stream ->
      stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
  }
}
