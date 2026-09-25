package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class WorkspaceTab(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val modified: Boolean = false,
)

@Composable
fun AndroidIDEProWorkspaceV2(
  tabs: List<WorkspaceTab>,
  selectedTab: Int,
  projectName: String,
  content: @Composable () -> Unit,
  onTabSelected: (Int) -> Unit,
  onExplorer: () -> Unit,
  onSearch: () -> Unit,
  onBuild: () -> Unit,
  onMore: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AndroidIDETheme {
    Column(
      modifier =
        modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background),
    ) {
      GlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              modifier = Modifier.size(36.dp),
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(IdeIcons.FolderOpen, contentDescription = null)
              }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = projectName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
              )
              Text(
                text = "${tabs.size} open file(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            IconButton(onClick = onExplorer) {
              Icon(IdeIcons.FolderOpen, contentDescription = "Explorer")
            }
            IconButton(onClick = onSearch) {
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
              edgePadding = 8.dp,
              containerColor = Color.Transparent,
              divider = {},
            ) {
              tabs.forEachIndexed { index, tab ->
                Tab(
                  selected = selectedTab == index,
                  onClick = { onTabSelected(index) },
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = tab.title,
                        maxLines = 1,
                      )
                      if (tab.modified) {
                        Spacer(Modifier.width(5.dp))
                        Box(
                          modifier =
                            Modifier
                              .size(6.dp)
                              .background(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small,
                              ),
                        )
                      }
                    }
                  },
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GlassPanel(
          modifier = Modifier.width(54.dp).fillMaxHeight(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            WorkspaceRailButton(
              icon = IdeIcons.FolderOpen,
              selected = true,
              onClick = onExplorer,
              description = "Explorer",
            )
            WorkspaceRailButton(
              icon = IdeIcons.Search,
              onClick = onSearch,
              description = "Search",
            )
            WorkspaceRailButton(
              icon = IdeIcons.Build,
              onClick = onBuild,
              description = "Build",
            )
            Spacer(Modifier.weight(1f))
            WorkspaceRailButton(
              icon = IdeIcons.More,
              onClick = onMore,
              description = "More",
            )
          }
        }

        GlassSurface(
          modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            content()
          }
        }
      }

      GlassRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Problems 0",
            style = MaterialTheme.typography.labelMedium,
          )
          Spacer(Modifier.width(14.dp))
          Text(
            text = "Build ready",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.weight(1f))
          Text(
            text = "Native",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun WorkspaceRailButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean = false,
  onClick: () -> Unit,
  description: String,
) {
  val container =
    if (selected) {
      MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
      Color.Transparent
    }

  Surface(
    modifier =
      Modifier
        .size(42.dp)
        .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    color = container,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = description,
        tint =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
