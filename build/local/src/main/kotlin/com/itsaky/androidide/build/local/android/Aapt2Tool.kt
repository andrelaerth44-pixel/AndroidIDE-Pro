/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import java.nio.file.Files
import java.nio.file.Path

data class Aapt2CompileResult(
  val archives: List<Path>,
  val result: AndroidToolResult,
)

interface Aapt2Tool {
  fun compile(resDirs: List<Path>, outDir: Path): Aapt2CompileResult

  fun link(
    compiled: List<Path>,
    manifest: Path,
    androidJar: Path,
    packageName: String,
    extraPackages: List<String>,
    minSdk: Int,
    targetSdk: Int,
    outputDir: Path,
    resourcesAp: Path,
    versionCode: Int? = null,
    versionName: String? = null,
    overlays: List<Path> = emptyList(),
    rTxt: Path? = null,
  ): AndroidToolResult
}

/** Native aapt2 wrapper used by the Gradle-free Android build pipeline. */
class Aapt2Subprocess(private val executable: Path) : Aapt2Tool {

  override fun compile(resDirs: List<Path>, outDir: Path): Aapt2CompileResult {
    Files.createDirectories(outDir)
    val archives = ArrayList<Path>()
    val logs = ArrayList<String>()

    resDirs.filter(Files::isDirectory).forEachIndexed { index, resDir ->
      val archive = outDir.resolve("res-$index.zip")
      val result = AndroidToolProcess.run(
        listOf(executable.toString(), "compile", "--dir", resDir.toString(), "-o", archive.toString()),
      )
      logs += result.log
      if (!result.success) {
        return Aapt2CompileResult(
          archives = archives,
          result = result.copy(log = logs),
        )
      }
      if (Files.isRegularFile(archive)) archives += archive
    }

    return Aapt2CompileResult(
      archives = archives,
      result = AndroidToolResult(success = true, log = logs),
    )
  }

  override fun link(
    compiled: List<Path>,
    manifest: Path,
    androidJar: Path,
    packageName: String,
    extraPackages: List<String>,
    minSdk: Int,
    targetSdk: Int,
    outputDir: Path,
    resourcesAp: Path,
    versionCode: Int?,
    versionName: String?,
    overlays: List<Path>,
    rTxt: Path?,
  ): AndroidToolResult {
    Files.createDirectories(outputDir)
    resourcesAp.parent?.let(Files::createDirectories)
    rTxt?.parent?.let(Files::createDirectories)

    val command = buildList {
      add(executable.toString())
      add("link")
      add("-o"); add(resourcesAp.toString())
      add("-I"); add(androidJar.toString())
      add("--manifest"); add(manifest.toString())
      add("--java"); add(outputDir.toString())
      add("--custom-package"); add(packageName)
      if (extraPackages.isNotEmpty()) {
        add("--extra-packages"); add(extraPackages.joinToString(":"))
      }
      add("--min-sdk-version"); add(minSdk.toString())
      add("--target-sdk-version"); add(targetSdk.toString())
      versionCode?.let { add("--version-code"); add(it.toString()) }
      versionName?.let { add("--version-name"); add(it) }
      rTxt?.let { add("--output-text-symbols"); add(it.toString()) }
      add("--auto-add-overlay")
      compiled.filter(Files::isRegularFile).forEach { add(it.toString()) }
      overlays.filter(Files::isRegularFile).forEach {
        add("-R")
        add(it.toString())
      }
    }

    return AndroidToolProcess.run(command)
  }
}
