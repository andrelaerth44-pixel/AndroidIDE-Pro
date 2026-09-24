/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildDiagnostic
import com.itsaky.androidide.build.api.BuildDiagnosticSink
import com.itsaky.androidide.build.api.CancellationToken
import com.itsaky.androidide.build.api.SequentialBuildExecutor
import com.itsaky.androidide.build.api.BuildResult
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidApplicationBuildPlanTest {

  @Test
  fun helloWorldGraphRunsResourcesThenJavaWithoutGradle() {
    val root = Files.createTempDirectory("androidide-native-graph")
    try {
      val res = root.resolve("src/main/res")
      val java = root.resolve("src/main/java")
      val build = root.resolve("build")
      val manifest = root.resolve("AndroidManifest.xml")
      val androidJar = root.resolve("android.jar")
      Files.createDirectories(res.resolve("values"))
      Files.createDirectories(java.resolve("com/example/app"))
      Files.writeString(res.resolve("values/strings.xml"), "<resources><string name='app_name'>Hello</string></resources>")
      Files.writeString(manifest, "<manifest package='com.example.app'/>")
      Files.writeString(androidJar, "placeholder")
      Files.writeString(
        java.resolve("com/example/app/MainActivity.java"),
        "package com.example.app; public class MainActivity { int label = R.string.app_name; }",
      )

      val fakeAapt2 = object : Aapt2Tool {
        override fun compile(resDirs: List<Path>, outDir: Path): Aapt2CompileResult {
          Files.createDirectories(outDir)
          val archive = outDir.resolve("res-0.zip")
          Files.writeString(archive, "compiled")
          return Aapt2CompileResult(listOf(archive), AndroidToolResult(true, listOf("fake compile")))
        }

        override fun link(
          compiled: List<Path>, manifest: Path, androidJar: Path, packageName: String, extraPackages: List<String>,
          minSdk: Int, targetSdk: Int, outputDir: Path, resourcesAp: Path, versionCode: Int?, versionName: String?,
          overlays: List<Path>, rTxt: Path?,
        ): AndroidToolResult {
          val packageDir = outputDir.resolve("com/example/app")
          Files.createDirectories(packageDir)
          Files.writeString(
            packageDir.resolve("R.java"),
            "package com.example.app; public final class R { public static final class string { public static final int app_name = 1; } }",
          )
          resourcesAp.parent?.let(Files::createDirectories)
          Files.writeString(resourcesAp, "resources")
          return AndroidToolResult(true, listOf("fake link"))
        }
      }

      val graph = AndroidApplicationBuildPlan(
        AndroidApplicationBuildInputs(
          variant = "debug",
          resDirs = listOf(res),
          javaSourceRoots = listOf(java),
          manifest = manifest,
          androidJar = androidJar,
          packageName = "com.example.app",
          minSdk = 24,
          targetSdk = 35,
          buildDir = build,
        ),
        fakeAapt2,
      ).graph()

      assertEquals(
        listOf("mergeResourcesDebug", "aapt2CompileDebug", "aapt2LinkDebug", "compileJavaDebug"),
        graph.topologicalOrder().map { it.id },
      )

      val diagnostics = mutableListOf<BuildDiagnostic>()
      val result = SequentialBuildExecutor().execute(
        graph,
        BuildContext(
          projectDir = root,
          cacheDir = root.resolve("cache"),
          toolchainsDir = root.resolve("toolchains"),
          cancellation = CancellationToken.NONE,
          diagnostics = BuildDiagnosticSink { diagnostics += it },
        ),
      )

      assertEquals(BuildResult.State.SUCCESS, result.state)
      assertTrue(Files.exists(build.resolve("intermediates/javac/debug/com/example/app/MainActivity.class")))
      assertTrue(Files.exists(build.resolve("intermediates/resources/debug/resources.ap_")))
      assertTrue(diagnostics.none { it.severity == BuildDiagnostic.Severity.ERROR })
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
