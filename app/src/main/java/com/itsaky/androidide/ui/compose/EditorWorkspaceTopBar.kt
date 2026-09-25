package com.itsaky.androidide.ui.compose

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
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditorWorkspaceTopBar(
  projectName: String,
  tabs: List<WorkspaceTab>,
  selectedTab: Int,
  commands: List<CommandPaletteItem>,
  onTabSelected: (Int) -> Unit,
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
            text = tabs.size.toString() + " open file(s)",
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
          containerColor = androidx.compose.ui.graphics.Color.Transparent,
          divider = {},
        ) {
          tabs.forEachIndexed { index, tab ->
            Tab(
              selected = index == selectedTab,
              onClick = { onTabSelected(index) },
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Text(text = tab.title, maxLines = 1)
                  if (tab.modified) {
                    Box(
                      modifier = Modifier.size(6.dp),
                    ) {
                      Spacer(
                        modifier =
                          Modifier
                            .size(6.dp)
                            .padding(0.dp),
                      )
                    }
                  }
                }
              },
            )
          }
        }
      }
    }
  }

  if (paletteOpen) {
    CommandPalette(
      items = commands,
      onDismiss = { paletteOpen = false },
    )
  }
}
