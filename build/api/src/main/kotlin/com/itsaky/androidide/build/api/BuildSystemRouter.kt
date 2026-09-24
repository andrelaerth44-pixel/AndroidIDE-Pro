/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

/**
 * Selects the first backend that explicitly supports the concrete project/request.
 *
 * The caller supplies native first, Gradle compatibility second. No backend is selected implicitly from
 * task name alone, so adding native coverage cannot accidentally route an unsupported task into it.
 */
class BuildSystemRouter(
  private val backends: List<BuildSystem>,
) {

  init {
    require(backends.isNotEmpty()) { "At least one build backend is required" }
    require(backends.map(BuildSystem::id).distinct().size == backends.size) {
      "Build backend ids must be unique"
    }
  }

  fun select(
    project: BuildProject,
    request: BuildRequest,
    context: BuildContext,
  ): BuildSystem {
    return backends.firstOrNull { it.supports(project, request, context) }
      ?: error("No build backend supports request " + request.requestedTasks + " for project " + project.rootDir)
  }

  fun createBuildGraph(
    project: BuildProject,
    request: BuildRequest,
    context: BuildContext,
  ): Pair<BuildSystem, BuildGraph> {
    val backend = select(project, request, context)
    return backend to backend.createBuildGraph(project, request, context)
  }
}
