package com.itsaky.androidide.activities.build

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.setContent
import com.itsaky.androidide.R
import com.itsaky.androidide.ui.pro.BuildCenterScreen

class BuildCenterActivity : ComponentActivity() {

  companion object {
    const val EXTRA_MODULE_PATH = "androidide.pro.build.module.path"
    const val EXTRA_PROJECT_ROOT = "androidide.pro.build.project.root"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val modulePath = intent.getStringExtra(EXTRA_MODULE_PATH)
    val projectRoot = intent.getStringExtra(EXTRA_PROJECT_ROOT)

    val view = ComposeView(this).apply {
      setViewCompositionStrategy(
        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
      )
      setContent {
        BuildCenterScreen(
          modulePath = modulePath,
          projectRoot = projectRoot,
          onClose = ::finish
        )
      }
    }

    setContentView(view)
  }
}
