package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class EditorBreadcrumb(
  val label: String,
  val onClick: (() -> Unit)? = null,
)

@Composable
fun EditorHostV2(
  tabs: List<WorkspaceTab>,
  selectedTab: Int,
  breadcrumbs: List<EditorBreadcrumb> = emptyList(),
  onTabSelected: (Int) -> Unit,
  onTabClosed: (Int) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = Color.Transparent,
    ) {
      androidx.compose.foundation.layout.Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(48.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        tabs.forEachIndexed { index, tab ->
          EditorTab(
            tab = tab,
            selected = index == selectedTab,
            onClick = { onTabSelected(index) },
            onClose = { onTabClosed(index) },
          )
        }
      }
    }

    if (breadcrumbs.isNotEmpty()) {
      Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
      androidx.compose.foundation.layout.Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(36.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        breadcrumbs.forEachIndexed { index, breadcrumb ->
          val click = breadcrumb.onClick
          Text(
            text = breadcrumb.label,
            style = MaterialTheme.typography.labelMedium,
            color =
              if (click == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
              } else {
                MaterialTheme.colorScheme.onSurface
              },
            modifier =
              Modifier.then(
                if (click != null) Modifier.clickable(onClick = click) else Modifier,
              ).padding(vertical = 4.dp),
          )
          if (index < breadcrumbs.lastIndex) {
            Text(
              text = "/",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.outline,
            )
          }
        }
      }
    }

    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))

    Box(
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      content()
    }
  }
}

@Composable
private fun EditorTab(
  tab: WorkspaceTab,
  selected: Boolean,
  onClick: () -> Unit,
  onClose: () -> Unit,
) {
  Surface(
    modifier = Modifier.height(38.dp).widthIn(min = 132.dp),
    shape = MaterialTheme.shapes.medium,
    color =
      if (selected) {
        MaterialTheme.colorScheme.surfaceVariant
      } else {
        Color.Transparent
      },
    tonalElevation = if (selected) 1.dp else 0.dp,
  ) {
    androidx.compose.foundation.layout.Row(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tab.title,
          maxLines = 1,
          style = MaterialTheme.typography.bodyMedium,
        )
        if (!tab.subtitle.isNullOrBlank()) {
          Text(
            text = tab.subtitle!!,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (tab.modified) {
        Surface(
          modifier = Modifier.size(6.dp),
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primary,
        ) {}
      }

      IconButton(
        onClick = onClose,
        modifier = Modifier.size(34.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Close,
          contentDescription = "Close file",
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}
