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
import com.itsaky.androidide.build.api.BuildGraph
import com.itsaky.androidide.build.api.BuildModuleType
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.BuildSystem
import com.itsaky.androidide.build.api.TaskDescriptor

/**
 * Lightweight runtime engine for operations that do not require Gradle.
 *
 * This is intentionally small. Its first real operation is clean, which removes generated build
 * directories without starting Gradle, a JVM daemon or a Tooling API process.
 */
class LightweightBuildSystem : BuildSystem {

  override val id: String = "local"

  override fun supports(moduleType: BuildModuleType): Boolean = true

  override fun createBuildGraph(
    project: BuildProject,
    request: BuildRequest,
    context: BuildContext,
  ): BuildGraph {
    require(request.requestedTasks.isEmpty() || request.requestedTasks.all { it == "clean" }) {
      "Lightweight engine currently supports only the clean task"
    }

    return if (request.clean || request.requestedTasks.contains("clean")) {
      BuildGraph(listOf(CleanProjectTask(project)))
    } else {
      BuildGraph(emptyList())
    }
  }

  override fun tasks(project: BuildProject): Collection<TaskDescriptor> {
    return listOf(
      TaskDescriptor("clean", "Remove generated build outputs without starting Gradle"),
    )
  }
}
