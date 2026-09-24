/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import com.itsaky.androidide.build.api.BuildModule
import com.itsaky.androidide.build.api.BuildRequest
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Small controlled-layout adapter for the first native Android path.
 *
 * It intentionally handles the conventional `src/main`/`src/<variant>` layout only. The full Workspace/variant
 * model remains the next integration boundary and should replace this adapter rather than grow ad-hoc Gradle rules.
 */
object ConventionalAndroidApplicationAdapter {

  fun createInputs(
    module: BuildModule,
    request: BuildRequest,
    toolchain: NativeAndroidToolchain,
  ): AndroidApplicationBuildInputs {
    val projectDir = module.projectDir
    val variant = request.variant?.ifBlank { null } ?: inferVariant(request)
    val manifest = firstExisting(
      projectDir.resolve("src/$variant/AndroidManifest.xml"),
      projectDir.resolve("src/main/AndroidManifest.xml"),
      projectDir.resolve("AndroidManifest.xml"),
    ) ?: error("No AndroidManifest.xml found in ${projectDir}")

    val packageName = readPackageName(manifest)
      ?: request.parameters["android.packageName"]
      ?: error("No Android package name found in $manifest")

    val resDirs = listOf(
      projectDir.resolve("src/main/res"),
      projectDir.resolve("src/$variant/res"),
    ).filter(Files::isDirectory).distinct()

    val javaRoots = listOf(
      projectDir.resolve("src/main/java"),
      projectDir.resolve("src/$variant/java"),
    ).filter(Files::isDirectory).distinct()

    return AndroidApplicationBuildInputs(
      variant = variant,
      resDirs = resDirs,
      javaSourceRoots = javaRoots,
      manifest = manifest,
      androidJar = toolchain.androidJar,
      packageName = packageName,
      minSdk = request.parameters["android.minSdk"]?.toIntOrNull() ?: 21,
      targetSdk = request.parameters["android.targetSdk"]?.toIntOrNull() ?: 35,
      buildDir = projectDir.resolve("build"),
      javacBootClasspath = listOf(toolchain.androidJar),
      javaSourceLevel = request.parameters["java.source"] ?: "17",
    )
  }

  private fun inferVariant(request: BuildRequest): String {
    val requested = request.requestedTasks.firstOrNull().orEmpty()
    val suffix = requested.removePrefix("assemble")
    return if (suffix.isBlank()) "debug" else suffix.replaceFirstChar(Char::lowercase)
  }

  private fun firstExisting(vararg paths: Path): Path? = paths.firstOrNull(Files::isRegularFile)

  private fun readPackageName(manifest: Path): String? = runCatching {
    val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
      .newDocumentBuilder().parse(manifest.toFile())
    document.documentElement?.getAttribute("package")?.takeIf(String::isNotBlank)
  }.getOrNull()
}
