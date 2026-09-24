package com.itsaky.androidide.actions

import android.content.Context
import android.widget.Toast
import com.itsaky.androidide.R
import com.itsaky.androidide.build.NativeBuildCoordinator
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.projects.android.AndroidModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.itsaky.androidide.actions.ActionItem.Location.EDITOR_TOOLBAR

class NativeBuildAction : ActionItem {

  companion object {
    const val ID = "androidide.pro.build.native"
  }

  override val id: String = ID
  override var label: String = ""
  override var visible: Boolean = true
  override var enabled: Boolean = true
  override var icon: android.graphics.drawable.Drawable? = null
  override var requiresUIThread: Boolean = false
  override var location: ActionItem.Location = EDITOR_TOOLBAR

  override fun prepare(data: ActionData) {
    super.prepare(data)
    val context = data.get(Context::class.java) ?: return
    label = context.getString(R.string.action_build_project)
    icon = context.getDrawable(R.drawable.ic_build)

    enabled = runCatching {
      IProjectManager.getInstance().getWorkspace()?.androidProjects()?.any() == true
    }.getOrDefault(false)
  }

  override fun getShowAsActionFlags(data: ActionData): Int =
    android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM

  override suspend fun execAction(data: ActionData): Any {
    val context = data.get(Context::class.java)
      ?: return com.itsaky.androidide.build.api.BuildResult(
        success = false,
        message = "Editor context is unavailable"
      )

    val manager = IProjectManager.getInstance()
    val workspace = manager.getWorkspace()
      ?: return com.itsaky.androidide.build.api.BuildResult(
        success = false,
        message = "No project is open"
      )

    val file = data.get(java.io.File::class.java)
    val module = if (file != null) {
      workspace.findModuleForFile(file, false) as? AndroidModule
    } else null
    val target = module ?: workspace.androidProjects().firstOrNull()

    if (target == null) {
      return com.itsaky.androidide.build.api.BuildResult(
        success = false,
        message = "No Android application module is available"
      )
    }

    return withContext(Dispatchers.IO) {
      NativeBuildCoordinator(context).assembleDebug(target)
    }
  }

  override fun postExec(data: ActionData, result: Any) {
    val context = data.get(Context::class.java) ?: return
    val build = result as? com.itsaky.androidide.build.api.BuildResult ?: return

    Toast.makeText(
      context,
      if (build.success) {
        context.getString(R.string.action_build_success) +
          (build.outputApk?.let { ": " + it } ?: "")
      } else {
        context.getString(R.string.action_build_failed) +
          ": " + build.message
      },
      Toast.LENGTH_LONG
    ).show()
  }
}
