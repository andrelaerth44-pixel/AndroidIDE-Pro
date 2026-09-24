package com.itsaky.androidide.actions

import android.content.Context
import android.content.Intent
import com.itsaky.androidide.R
import com.itsaky.androidide.activities.build.BuildCenterActivity
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
      IProjectManager.getInstance().getWorkspace()?.androidProjects()?.any { it.isApplication } == true
    }.getOrDefault(false)
  }

  override fun getShowAsActionFlags(data: ActionData): Int =
    android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM

  override suspend fun execAction(data: ActionData): Any {
    val context = data.get(Context::class.java) ?: return false
    val workspace = IProjectManager.getInstance().getWorkspace() ?: return false

    val file = data.get(java.io.File::class.java)
    val module = if (file != null) {
      workspace.findModuleForFile(file, false) as? AndroidModule
    } else null

    val target = module?.takeIf { it.isApplication }
      ?: workspace.androidProjects().firstOrNull { it.isApplication }
      ?: return false

    context.startActivity(
      Intent(context, BuildCenterActivity::class.java).apply {
        putExtra(BuildCenterActivity.EXTRA_MODULE_PATH, target.path)
      }
    )

    return true
  }

  override fun postExec(data: ActionData, result: Any) = Unit
}
