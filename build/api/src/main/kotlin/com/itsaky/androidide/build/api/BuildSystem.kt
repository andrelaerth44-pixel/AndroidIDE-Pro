/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

interface BuildSystem {

  val id: String

  fun supports(moduleType: BuildModuleType): Boolean

  /**
   * Capability check for a concrete project/request pair.
   *
   * Backends should override this when support depends on the task, variant, project layout or
   * available toolchain. The default keeps legacy module-type semantics.
   */
  fun supports(
    project: BuildProject,
    request: BuildRequest,
    context: BuildContext? = null,
  ): Boolean {
    return project.modules.all { supports(it.type) }
  }

  fun createBuildGraph(
    project: BuildProject,
    request: BuildRequest,
    context: BuildContext,
  ): BuildGraph

  fun tasks(project: BuildProject): Collection<TaskDescriptor>
}
