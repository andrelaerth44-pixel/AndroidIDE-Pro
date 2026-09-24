/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.gradle

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildModule
import com.itsaky.androidide.build.api.BuildModuleType
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.build.api.BuildRequest
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleBuildSystemTest {
  private val project = BuildProject(
    Paths.get("/project"),
    listOf(BuildModule(":app", Paths.get("/project/app"), BuildModuleType.UNKNOWN)),
  )
  private val context = BuildContext(
    projectDir = Paths.get("/project"),
    cacheDir = Paths.get("/cache"),
    toolchainsDir = Paths.get("/toolchains"),
  )

  @Test
  fun variantCreatesAssembleTask() {
    val graph = GradleBuildSystem().createBuildGraph(project, BuildRequest(variant = "debug"), context)
    assertEquals(listOf("assembleDebug"), graph.topologicalOrder().map { it.id })
  }

  @Test
  fun cleanIsDependencyOfRequestedTasks() {
    val graph = GradleBuildSystem().createBuildGraph(
      project,
      BuildRequest(requestedTasks = setOf(":app:assembleDebug"), clean = true),
      context,
    )
    assertEquals(listOf("clean", ":app:assembleDebug"), graph.topologicalOrder().map { it.id })
  }

  @Test
  fun nativeModulesAreNotHandledByGradleAdapter() {
    assertFalse(GradleBuildSystem().supports(BuildModuleType.NATIVE))
    assertTrue(GradleBuildSystem().supports(BuildModuleType.UNKNOWN))
    assertTrue(GradleBuildSystem().supports(BuildModuleType.JAVA_LIBRARY))
  }
}
