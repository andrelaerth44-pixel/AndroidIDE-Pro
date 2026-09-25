package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EditorWorkspaceTopBar(
  projectName: String,
  tabs: List<WorkspaceTab>,
  selectedTab: Int,
  commands: List<CommandPaletteItem>,
  onTabSelected: (Int) -> Unit,
  onTabClosed: (Int) -> Unit,
  onSave: () -> Unit,
  breadcrumbs: List<EditorBreadcrumb>,
  statusBarState: IdeStatusBarState,
  onExplorer: () -> Unit,
  onBuild: () -> Unit,
  onMore: () -> Unit,
) {
  var paletteOpen by remember { mutableStateOf(false) }

  GlassSurface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(34.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(IdeIcons.FolderOpen, contentDescription = null)
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = projectName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
          )
          Text(
            text = if (tabs.isEmpty()) "No open files" else "${tabs.size} open file(s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(onClick = onExplorer) {
          Icon(IdeIcons.FolderOpen, contentDescription = "Explorer")
        }
        IconButton(onClick = { paletteOpen = true }) {
          Icon(IdeIcons.Search, contentDescription = "Search")
        }
        IconButton(onClick = onSave) {
          Icon(IdeIcons.Save, contentDescription = "Save")
        }
        IconButton(onClick = onBuild) {
          Icon(IdeIcons.Build, contentDescription = "Build")
        }
        IconButton(onClick = onMore) {
          Icon(IdeIcons.More, contentDescription = "More")
        }
      }

      if (tabs.isNotEmpty()) {
        ScrollableTabRow(
          selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex),
          edgePadding = 6.dp,
          containerColor = Color.Transparent,
          divider = {},
        ) {
          tabs.forEachIndexed { index, tab ->
            Tab(
              selected = index == selectedTab,
              onClick = { onTabSelected(index) },
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(text = tab.title, maxLines = 1)
                  if (tab.modified) {
                    Surface(
                      modifier = Modifier.size(6.dp),
                      shape = MaterialTheme.shapes.small,
                      color = MaterialTheme.colorScheme.primary,
                    ) {}
                  }
                  IconButton(
                    onClick = { onTabClosed(index) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Close,
                      contentDescription = "Close ${tab.title}",
                      modifier = Modifier.size(17.dp),
                    )
                  }
                }
              },
            )
          }
        }
      }

      if (breadcrumbs.isNotEmpty()) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(32.dp)
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          breadcrumbs.forEachIndexed { index, breadcrumb ->
            val onClick = breadcrumb.onClick
            Text(
              text = breadcrumb.label,
              style = MaterialTheme.typography.labelSmall,
              color =
                if (onClick == null) {
                  MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                  MaterialTheme.colorScheme.onSurface
                },
              modifier =
                Modifier.then(
                  if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
                ).padding(vertical = 4.dp),
            )
            if (index < breadcrumbs.lastIndex) {
              Text(
                text = "/",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
              )
            }
          }
        }
      }

      IdeStatusBar(
        state = statusBarState,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
      )
    }
  }

  if (paletteOpen) {
    CommandPalette(
      items = commands,
      onDismiss = { paletteOpen = false },
    )
  }
}
