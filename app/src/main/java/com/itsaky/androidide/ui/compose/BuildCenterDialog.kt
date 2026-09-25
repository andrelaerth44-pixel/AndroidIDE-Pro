package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Dialog
import androidx.compose.material3.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BuildCenterDialog(
  state: BuildCenterUiState,
  onBuild: () -> Unit,
  onStop: () -> Unit,
  onRefresh: () -> Unit,
  onIssueClick: (BuildIssueUi) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 28.dp),
    ) {
      AndroidIDEProBuildCenter(
        steps = state.steps,
        isBuilding = state.isBuilding,
        onBuild = onBuild,
        onStop = onStop,
        canStopBuild = true,
        onRefresh = onRefresh,
        onIssueClick = onIssueClick,
        logs = state.logs,
        issues = state.issues,
        progress = state.progress,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
