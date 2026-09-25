package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class IdeStatusBarState(
  val language: String = "Kotlin",
  val abi: String = "arm64-v8a",
  val branch: String = "main",
  val line: Int = 1,
  val column: Int = 1,
  val message: String = "Ready",
)

@Composable
fun IdeStatusBar(
  state: IdeStatusBarState,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    tonalElevation = 0.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      StatusItem(text = state.language)
      StatusItem(text = state.abi)
      StatusItem(text = state.branch)
      Text(
        text = state.message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
      StatusItem(text = "Ln ${state.line}, Col ${state.column}")
    }
  }
}

@Composable
private fun StatusItem(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
  )
}
