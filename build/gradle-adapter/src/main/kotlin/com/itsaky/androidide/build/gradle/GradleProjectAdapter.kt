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
