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
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeResourceTasksTest {

  @Test
  fun mergeResourcesOverlaysFilesAndValues() {
    val root = Files.createTempDirectory("androidide-res-merge")
    try {
      val low = root.resolve("low/res")
      val high = root.resolve("high/res")
      val output = root.resolve("merged")
      Files.createDirectories(low.resolve("drawable"))
      Files.createDirectories(high.resolve("drawable"))
      Files.createDirectories(low.resolve("values"))
      Files.createDirectories(high.resolve("values"))
      Files.writeString(low.resolve("drawable/icon.xml"), "<vector/>" )
      Files.writeString(high.resolve("drawable/icon.xml"), "<vector id='high'/>")
      Files.writeString(low.resolve("values/strings.xml"), "<resources><string name='app_name'>Low</string></resources>")
      Files.writeString(high.resolve("values/strings.xml"), "<resources><string name='app_name'>High</string><color name='accent'>#fff</color></resources>")

      val result = MergeResourcesTask(
        id = "mergeResourcesDebug",
        resDirs = listOf(low, high),
        outputDir = output,
      ).execute(context(root))

      assertEquals(TaskResult.State.SUCCESS, result.state)
      assertEquals("<vector id='high'/>", Files.readString(output.resolve("drawable/icon.xml")))
      val values = Files.readString(output.resolve("values/values.xml"))
      assertTrue(values.contains("High"))
      assertTrue(values.contains("accent"))
      assertTrue(!values.contains(">Low<"))
    } finally {
      deleteRecursively(root)
    }
  }

  @Test
  fun aapt2CompileTaskUsesInjectedTool() {
    val root = Files.createTempDirectory("androidide-aapt2-compile")
    try {
      val resources = root.resolve("res")
      val out = root.resolve("compiled")
      Files.createDirectories(resources)
      val archive = out.resolve("res-0.zip")
      val tool = object : Aapt2Tool {
        override fun compile(resDirs: List<Path>, outDir: Path): Aapt2CompileResult {
          Files.createDirectories(outDir)
          Files.writeString(archive, "fake")
          return Aapt2CompileResult(listOf(archive), AndroidToolResult(true))
        }

        override fun link(
          compiled: List<Path>, manifest: Path, androidJar: Path, packageName: String, extraPackages: List<String>,
          minSdk: Int, targetSdk: Int, outputDir: Path, resourcesAp: Path, versionCode: Int?, versionName: String?,
          overlays: List<Path>, rTxt: Path?,
        ): AndroidToolResult = AndroidToolResult(true)
      }

      val result = Aapt2CompileTask(
        id = "aapt2CompileDebug",
        mergedResources = resources,
        outputDir = out,
        aapt2 = tool,
      ).execute(context(root))

      assertEquals(TaskResult.State.SUCCESS, result.state)
      assertTrue(Files.exists(archive))
    } finally {
      deleteRecursively(root)
    }
  }

  @Test
  fun aapt2LinkTaskMapsToolFailureToLinkerDiagnostic() {
    val root = Files.createTempDirectory("androidide-aapt2-link")
    try {
      val compiled = root.resolve("compiled")
      val manifest = root.resolve("AndroidManifest.xml")
      val androidJar = root.resolve("android.jar")
      val generated = root.resolve("generated")
      val ap = root.resolve("resources.ap_")
      Files.createDirectories(compiled)
      Files.writeString(manifest, "<manifest/>")
      Files.writeString(androidJar, "fake")

      val tool = object : Aapt2Tool {
        override fun compile(resDirs: List<Path>, outDir: Path) = Aapt2CompileResult(emptyList(), AndroidToolResult(true))
        override fun link(
          compiled: List<Path>, manifest: Path, androidJar: Path, packageName: String, extraPackages: List<String>,
          minSdk: Int, targetSdk: Int, outputDir: Path, resourcesAp: Path, versionCode: Int?, versionName: String?,
          overlays: List<Path>, rTxt: Path?,
        ) = AndroidToolResult(success = false, log = listOf("aapt2: bad manifest"), message = "bad manifest")
      }

      val diagnostics = mutableListOf<BuildDiagnostic>()
      val taskContext = context(root, diagnostics)
      val result = Aapt2LinkTask(
        id = "aapt2LinkDebug",
        compiledResources = compiled,
        manifest = manifest,
        androidJar = androidJar,
        packageName = "com.example.app",
        minSdk = 24,
        targetSdk = 35,
        generatedRDir = generated,
        resourcesAp = ap,
        aapt2 = tool,
      ).execute(taskContext)

      assertEquals(TaskResult.State.FAILED, result.state)
      assertTrue(diagnostics.any {
        it.kind == BuildDiagnostic.Kind.LINKER && it.severity == BuildDiagnostic.Severity.ERROR &&
          it.message == "bad manifest"
      })
    } finally {
      deleteRecursively(root)
    }
  }

  private fun context(root: Path, diagnostics: MutableList<BuildDiagnostic> = mutableListOf()) = TaskContext(
    projectDir = root,
    cacheDir = root.resolve("cache"),
    toolchainsDir = root.resolve("toolchains"),
    environment = emptyMap(),
    cancellation = CancellationToken.NONE,
    diagnostics = BuildDiagnosticSink { diagnostics += it },
  )

  private fun deleteRecursively(root: Path) {
    if (!Files.exists(root)) return
    Files.walk(root).use { stream ->
      stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
  }
}
