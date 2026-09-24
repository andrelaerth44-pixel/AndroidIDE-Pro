package com.itsaky.androidide.build

import android.content.Context
import com.itsaky.androidide.build.api.BuildResult
import com.itsaky.androidide.projects.IWorkspace
import com.itsaky.androidide.projects.android.AndroidModule
import com.itsaky.androidide.projects.builder.BuildService
import com.itsaky.androidide.lookup.Lookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class BuildBackend {
  NATIVE,
  GRADLE
}

data class BuildRoute(
  val backend: BuildBackend,
  val reason: String
)

class NativeBuildCompatibility {

  fun route(module: AndroidModule): BuildRoute {
    if (!module.isApplication) {
      return BuildRoute(BuildBackend.GRADLE, "module is not an Android application")
    }

    val variant = module.getVariant("debug")
      ?: return BuildRoute(BuildBackend.GRADLE, "debug variant is unavailable")

    if (module.libraries.isNotEmpty()) {
      return BuildRoute(BuildBackend.GRADLE, "external/project dependencies are not yet supported by native V1")
    }

    val hasKotlin = runCatching {
      module.getCompileSourceDirectories()
        .flatMap { root ->
          if (!root.exists()) emptyList()
          else root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .take(1)
            .toList()
        }
        .isNotEmpty()
    }.getOrDefault(false)

    if (hasKotlin) {
      return BuildRoute(BuildBackend.GRADLE, "Kotlin compilation is not yet supported by native V1")
    }

    if (variant.mainArtifact.applicationId.isNullOrBlank() &&
      module.namespace.isNullOrBlank()
    ) {
      return BuildRoute(BuildBackend.GRADLE, "application identity is unavailable")
    }

    return BuildRoute(
      BuildBackend.NATIVE,
      "debug application is compatible with the native V1 pipeline"
    )
  }
}

class BuildRouter(
  private val context: Context,
  private val nativeCoordinator: NativeBuildCoordinator = NativeBuildCoordinator(context)
) {

  suspend fun assembleDebug(
    workspace: IWorkspace,
    modulePath: String,
    logger: (String) -> Unit = ::println
  ): BuildResult {
    val module = workspace.androidProjects()
      .firstOrNull { it.path == modulePath }
      ?: return BuildResult(false, message = "Android module not found: " + modulePath)

    return when (val route = NativeBuildCompatibility().route(module).also {
      logger("BUILD ROUTE: " + it.backend + " — " + it.reason)
    }.backend) {
      BuildBackend.NATIVE ->
        withContext(Dispatchers.IO) {
          nativeCoordinator.assembleDebug(workspace, modulePath, logger)
        }

      BuildBackend.GRADLE ->
        withContext(Dispatchers.IO) {
          assembleWithGradle(module, logger)
        }
    }
  }

  private fun assembleWithGradle(
    module: AndroidModule,
    logger: (String) -> Unit
  ): BuildResult {
    val buildService = Lookup.getDefault()
      .lookup(BuildService.KEY_BUILD_SERVICE)

    if (buildService == null) {
      return BuildResult(
        false,
        message = "Gradle fallback is unavailable"
      )
    }

    val variant = module.getVariant("debug")
      ?: return BuildResult(false, message = "Debug variant is unavailable")

    val taskName = variant.mainArtifact.assembleTaskName
      .takeIf { it.isNotBlank() }
      ?: return BuildResult(false, message = "No assemble task is available")

    logger("GRADLE FALLBACK: " + taskName)

    return runCatching {
      val result = buildService.executeTasks(taskName).get()

      if (result.isSuccessful) {
        BuildResult(
          success = true,
          message = "Gradle fallback completed"
        )
      } else {
        BuildResult(
          success = false,
          message = "Gradle fallback failed: " +
            (result.failure?.name ?: "UNKNOWN")
        )
      }
    }.getOrElse {
      BuildResult(
        success = false,
        message = "Gradle fallback error: " +
          (it.message ?: it.javaClass.simpleName)
      )
    }
  }
}
