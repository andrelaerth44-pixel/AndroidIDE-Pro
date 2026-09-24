package com.itsaky.androidide.build

import android.content.Context
import com.itsaky.androidide.build.api.BuildResult
import com.itsaky.androidide.projects.IWorkspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class BuildBackend {
  NATIVE
}

data class BuildRoute(
  val backend: BuildBackend,
  val reason: String
)

class NativeBuildCompatibility {

  fun route(): BuildRoute =
    BuildRoute(
      BuildBackend.NATIVE,
      "AndroidIDE Pro uses the built-in native build engine for every supported language"
    )
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
    val route = NativeBuildCompatibility().route()
    logger("BUILD ROUTE: " + route.backend + " — " + route.reason)

    return withContext(Dispatchers.IO) {
      nativeCoordinator.assembleDebug(workspace, modulePath, logger)
    }
  }
}
