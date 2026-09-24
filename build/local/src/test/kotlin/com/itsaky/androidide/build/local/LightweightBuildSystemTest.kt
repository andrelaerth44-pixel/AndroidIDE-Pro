/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildModule
import com.itsaky.androidide.build.api.BuildModuleType
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.SequentialBuildExecutor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LightweightBuildSystemTest {

  @Test
  fun cleanDoesNotNeedGradle() {
    val root = Files.createTempDirectory("androidide-local-build")
    try {
      val appDir = Files.createDirectories(root.resolve("app"))
      Files.createDirectories(root.resolve("build").resolve("generated"))
      Files.createDirectories(appDir.resolve("build").resolve("intermediates"))
      Files.writeString(appDir.resolve("build/intermediates/index"), "generated")

      val project = BuildProject(
        rootDir = root,
        modules = listOf(BuildModule(":app", appDir, BuildModuleType.UNKNOWN)),
      )
      val context = BuildContext(
        projectDir = root,
        cacheDir = root.resolve(".cache"),
        toolchainsDir = root.resolve(".toolchains"),
      )

      val result = SequentialBuildExecutor().execute(
        LightweightBuildSystem().createBuildGraph(
          project,
          BuildRequest(requestedTasks = setOf("clean")),
          context,
        ),
        context,
      )

      assertEquals(com.itsaky.androidide.build.api.BuildResult.State.SUCCESS, result.state)
      assertFalse(Files.exists(root.resolve("build")))
      assertFalse(Files.exists(appDir.resolve("build")))
    } finally {
      deleteRecursively(root)
    }
  }

  @Test
  fun nonCleanTaskIsRejected() {
    val root = Paths.get("/project")
    val project = BuildProject(
      root,
      listOf(BuildModule(":app", root.resolve("app"), BuildModuleType.UNKNOWN)),
    )
    val context = BuildContext(root, root.resolve(".cache"), root.resolve(".toolchains"))

    try {
      LightweightBuildSystem().createBuildGraph(
        project,
        BuildRequest(requestedTasks = setOf("assemble")),
        context,
      )
      throw AssertionError("Expected unsupported task to be rejected")
    } catch (error: IllegalArgumentException) {
      assertTrue(error.message!!.contains("clean"))
    }
  }

  private fun deleteRecursively(root: Path) {
    if (!Files.exists(root)) return
    Files.walk(root).use { stream ->
      stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
  }
}
