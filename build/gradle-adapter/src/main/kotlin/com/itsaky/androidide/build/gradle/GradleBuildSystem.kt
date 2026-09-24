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
import com.itsaky.androidide.build.api.BuildGraph
import com.itsaky.androidide.build.api.BuildModuleType
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.BuildSystem
import com.itsaky.androidide.build.api.TaskDescriptor

class GradleBuildSystem(
  private val executor: GradleTaskExecutor = GradleTaskExecutor.UNAVAILABLE,
) : BuildSystem {

  override val id: String = "gradle"

  override fun supports(moduleType: BuildModuleType): Boolean {
    return moduleType != BuildModuleType.NATIVE
  }

  override fun createBuildGraph(
    project: BuildProject,
    request: BuildRequest,
    context: BuildContext,
  ): BuildGraph {
    require(project.rootDir.normalize() == context.projectDir.normalize()) {
      "Build project root '${project.rootDir}' does not match context '${context.projectDir}'"
    }

    val requested = if (request.requestedTasks.isNotEmpty()) {
      request.requestedTasks.toSortedSet()
    } else {
      sortedSetOf(defaultTask(request.variant))
    }

    val taskIds = linkedSetOf<String>()
    if (request.clean) taskIds += "clean"
    taskIds += requested

    return BuildGraph(taskIds.map { taskPath ->
      val dependencies = if (request.clean && taskPath != "clean") setOf("clean") else emptySet()
      GradleBuildTask(taskPath, dependencies, executor)
    })
  }

  override fun tasks(project: BuildProject): Collection<TaskDescriptor> = listOf(
    TaskDescriptor("assemble", "Assemble project outputs"),
    TaskDescriptor("build", "Assemble and verify the project"),
    TaskDescriptor("check", "Run project verification tasks"),
    TaskDescriptor("clean", "Remove Gradle build outputs"),
  )

  private fun defaultTask(variant: String?): String {
    if (variant.isNullOrBlank()) return "assemble"
    return "assemble" + variant.replaceFirstChar(Char::uppercaseChar)
  }
}
