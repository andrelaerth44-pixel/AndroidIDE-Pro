/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import com.itsaky.androidide.build.api.BuildDiagnostic
import com.itsaky.androidide.build.api.BuildDiagnosticSink
import com.itsaky.androidide.build.api.CancellationToken
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DexAndApkTasksTest {

  @Test
  fun dexMergeCollectsArchivesThroughInjectedDexer() {
    val root = Files.createTempDirectory("androidide-dex-merge")
    try {
      val archive = root.resolve("archive")
      val dex = archive.resolve("classes.dex")
      val androidJar = root.resolve("android.jar")
      val output = root.resolve("merged")
      Files.createDirectories(archive)
      Files.writeString(dex, "fake-dex")
      Files.writeString(androidJar, "fake-android")

      val fake = object : Dexer {
        override fun dexArchive(inputs: List<Path>, classpath: List<Path>, androidJar: Path, minApi: Int, release: Boolean, outDir: Path, threads: Int) = AndroidToolResult(true)
        override fun dex(inputs: List<Path>, androidJar: Path, minApi: Int, release: Boolean, outDir: Path, threads: Int): AndroidToolResult {
          Files.createDirectories(outDir)
          Files.writeString(outDir.resolve("classes.dex"), "merged")
          return AndroidToolResult(true)
        }
      }

      val result = DexMergeTask(
        id = "mergeProjectDexDebug",
        dexInputs = listOf(archive),
        androidJar = androidJar,
        minSdk = 24,
        outputDir = output,
        dexer = fake,
      ).execute(context(root))

      assertEquals(TaskResult.State.SUCCESS, result.state)
      assertTrue(Files.exists(output.resolve("classes.dex")))
    } finally {
      deleteRecursively(root)
    }
  }

  @Test
  fun packageApkCopiesResourcesDexAndAssets() {
    val root = Files.createTempDirectory("androidide-package")
    try {
      val resources = root.resolve("resources.ap_")
      val dexDir = root.resolve("dex")
      val assets = root.resolve("assets")
      val output = root.resolve("app-unsigned.apk")
      Files.createDirectories(dexDir)
      Files.createDirectories(assets.resolve("data"))

      java.util.zip.ZipOutputStream(Files.newOutputStream(resources)).use { zip ->
        zip.putNextEntry(java.util.zip.ZipEntry("AndroidManifest.xml"))
        zip.write("manifest".toByteArray())
        zip.closeEntry()
        zip.putNextEntry(java.util.zip.ZipEntry("resources.arsc"))
        zip.write("resources".toByteArray())
        zip.closeEntry()
      }
      Files.writeString(dexDir.resolve("classes.dex"), "dex")
      Files.writeString(assets.resolve("data/hello.txt"), "hello")

      ApkPackager.packageApk(
        ApkPackageRequest(
          resourcesAp = resources,
          dexDirs = listOf(dexDir),
          assetDirs = listOf(assets),
          outputApk = output,
        )
      )

      ZipFile(output.toFile()).use { zip ->
        assertTrue(zip.getEntry("AndroidManifest.xml") != null)
        assertTrue(zip.getEntry("resources.arsc") != null)
        assertTrue(zip.getEntry("classes.dex") != null)
        assertTrue(zip.getEntry("assets/data/hello.txt") != null)
      }
    } finally {
      deleteRecursively(root)
    }
  }

  @Test
  fun signerTaskUsesInjectedSigner() {
    val root = Files.createTempDirectory("androidide-sign")
    try {
      val unsigned = root.resolve("unsigned.apk")
      val signed = root.resolve("signed.apk")
      val keystore = root.resolve("debug.keystore")
      Files.writeString(unsigned, "apk")
      Files.writeString(keystore, "key")

      val fake = ApkSigner { request ->
        Files.copy(request.inputApk, request.signedApk)
        AndroidToolResult(true)
      }
      val result = SignApkTask(
        id = "signDebug",
        request = ApkSigningRequest(unsigned, signed, keystore, "pass", "alias", "pass"),
        signer = fake,
      ).execute(context(root))

      assertEquals(TaskResult.State.SUCCESS, result.state)
      assertTrue(Files.exists(signed))
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
