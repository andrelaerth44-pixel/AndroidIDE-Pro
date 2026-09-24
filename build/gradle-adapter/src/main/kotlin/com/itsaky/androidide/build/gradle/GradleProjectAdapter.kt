/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.gradle

import com.itsaky.androidide.build.api.BuildModule
import com.itsaky.androidide.build.api.BuildModuleType
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.tooling.api.IProject
import com.itsaky.androidide.tooling.api.ProjectType
import com.itsaky.androidide.tooling.api.models.params.StringParameter
import java.nio.file.Path

object GradleProjectAdapter {
  fun from(project: IProject, rootDir: Path): BuildProject {
    val modules = project.getProjects().get().map { metadata ->
      val selection = project.selectProject(StringParameter(metadata.projectPath)).get()
      require(selection.isSuccessful) {
        "Unable to select Gradle project '${metadata.projectPath}'"
      }
      BuildModule(
        id = metadata.projectPath,
        projectDir = metadata.projectDir.toPath(),
        type = project.getType().get().toBuildModuleType(),
      )
    }
    return BuildProject(rootDir, modules)
  }

  private fun ProjectType.toBuildModuleType(): BuildModuleType = when (this) {
    ProjectType.Java -> BuildModuleType.JAVA_LIBRARY
    ProjectType.Android,
    ProjectType.Gradle,
    ProjectType.Unknown -> BuildModuleType.UNKNOWN
  }
}
