package com.itsaky.androidide.actions

import android.content.Context
import android.content.Intent
import com.itsaky.androidide.R
import com.itsaky.androidide.activities.build.BuildCenterActivity
import com.itsaky.androidide.build.NativeProjectLocator
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.projects.android.AndroidModule
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
  override var requiresUIThread: Boolean = true
  override var location: ActionItem.Location = EDITOR_TOOLBAR

  override fun prepare(data: ActionData) {
    super.prepare(data)
    val context = data.get(Context::class.java) ?: return
    label = context.getString(R.string.action_build_project)
    icon = context.getDrawable(R.drawable.ic_build)

    enabled = runCatching {
      val workspace = IProjectManager.getInstance().getWorkspace()
      val file = data.get(java.io.File::class.java)

      workspace?.androidProjects()?.any { it.isApplication } == true ||
        NativeProjectLocator.findRoot(file?.toPath()) != null
    }.getOrDefault(false)
  }

  override fun getShowAsActionFlags(data: ActionData): Int =
    android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM

  override suspend fun execAction(data: ActionData): Any {
    val context = data.get(Context::class.java) ?: return false
    val workspace = IProjectManager.getInstance().getWorkspace()
    val file = data.get(java.io.File::class.java)

    val module = if (workspace != null && file != null) {
      workspace.findModuleForFile(file, false) as? AndroidModule
    } else {
      null
    }

    val target = module?.takeIf { it.isApplication }
      ?: workspace?.androidProjects()?.firstOrNull { it.isApplication }

    val standaloneRoot = if (target == null) {
      NativeProjectLocator.findRoot(file?.toPath())
    } else {
      null
    }

    if (target == null && standaloneRoot == null) {
      return false
    }

    context.startActivity(
      Intent(context, BuildCenterActivity::class.java).apply {
        if (target != null) {
          putExtra(BuildCenterActivity.EXTRA_MODULE_PATH, target.path)
        } else {
          putExtra(
            BuildCenterActivity.EXTRA_PROJECT_ROOT,
            standaloneRoot.toString()
          )
        }
      }
    )

    return true
  }

  override fun postExec(data: ActionData, result: Any) = Unit
}
