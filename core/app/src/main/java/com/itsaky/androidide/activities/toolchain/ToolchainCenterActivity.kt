package com.itsaky.androidide.activities.toolchain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.itsaky.androidide.ui.pro.ToolchainCenterScreen

class ToolchainCenterActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ToolchainCenterScreen(onClose = ::finish)
    }
  }
}
