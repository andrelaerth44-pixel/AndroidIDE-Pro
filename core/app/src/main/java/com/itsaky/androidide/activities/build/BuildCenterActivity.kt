package com.itsaky.androidide.activities.build

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.itsaky.androidide.R
import com.itsaky.androidide.ui.pro.BuildCenterScreen

class BuildCenterActivity : ComponentActivity() {

  companion object {
    const val EXTRA_MODULE_PATH = "androidide.pro.build.module.path"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val modulePath = intent.getStringExtra(EXTRA_MODULE_PATH)

    setContent {
      BuildCenterScreen(
        modulePath = modulePath,
        onClose = ::finish
      )
    }
  }
}
