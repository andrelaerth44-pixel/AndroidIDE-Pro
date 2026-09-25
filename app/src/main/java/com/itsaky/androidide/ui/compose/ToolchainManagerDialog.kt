package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Dialog
import androidx.compose.material3.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itsaky.androidide.toolchain.ToolchainComponent
import com.itsaky.androidide.toolchain.ToolchainComponentStatus
import com.itsaky.androidide.toolchain.ToolchainSnapshot

@Composable
fun ToolchainManagerDialog(
  snapshot: ToolchainSnapshot,
  onRefresh: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Toolchain Manager",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "On-device compiler and SDK components",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(6.dp))
            Text(
              text = "Device ABI: \${snapshot.abi}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          IconButton(onClick = onRefresh) {
            Icon(IdeIcons.Refresh, contentDescription = "Refresh toolchains")
          }
        }
      }

      GlassPanel(modifier = Modifier.fillMaxWidth().weight(1f)) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          items(snapshot.components, key = { it.id }) { component ->
            ToolchainComponentRow(component)
          }
        }
      }
    }
  }
}

@Composable
private fun ToolchainComponentRow(component: ToolchainComponent) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      imageVector =
        when (component.status) {
          ToolchainComponentStatus.READY -> IdeIcons.Check
          ToolchainComponentStatus.MISSING -> IdeIcons.Error
          ToolchainComponentStatus.PLANNED -> IdeIcons.Pending
        },
      contentDescription = null,
      tint =
        when (component.status) {
          ToolchainComponentStatus.READY -> MaterialTheme.colorScheme.primary
          ToolchainComponentStatus.MISSING -> MaterialTheme.colorScheme.error
          ToolchainComponentStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
        },
      modifier = Modifier.size(22.dp),
    )

    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = component.name,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.weight(1f),
        )
        Text(
          text =
            when (component.status) {
              ToolchainComponentStatus.READY -> "Ready"
              ToolchainComponentStatus.MISSING -> "Missing"
              ToolchainComponentStatus.PLANNED -> "Planned"
            },
          style = MaterialTheme.typography.labelMedium,
          color =
            when (component.status) {
              ToolchainComponentStatus.READY -> MaterialTheme.colorScheme.primary
              ToolchainComponentStatus.MISSING -> MaterialTheme.colorScheme.error
              ToolchainComponentStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
      }

      Text(
        text = component.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      component.path?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
        )
      }
    }
  }
}
