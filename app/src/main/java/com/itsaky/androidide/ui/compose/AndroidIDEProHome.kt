package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.itsaky.androidide.resources.R

data class MainScreenAction(
  val titleRes: Int,
  val icon: ImageVector,
  val onClick: () -> Unit,
)

@Composable
fun AndroidIDEProHome(
  actions: List<MainScreenAction>,
  modifier: Modifier = Modifier,
) {
  var paletteOpen by remember { mutableStateOf(false) }

  AndroidIDETheme {
    Column(
      modifier = modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
          Text(
            text = "AndroidIDE Pro",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "Fast, native Android development.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      GlassRow(modifier = Modifier.fillMaxWidth()) {
        TextButton(
          onClick = { paletteOpen = true },
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
          Icon(IdeIcons.Search, contentDescription = null)
          Spacer(Modifier.padding(start = 8.dp))
          Text(
            text = "Search commands, files and actions",
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "Ctrl K",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(actions, key = { it.titleRes }) { action ->
          GlassRow(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable(onClick = action.onClick)
                  .padding(horizontal = 16.dp, vertical = 13.dp),
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = action.icon,
                contentDescription = null,
              )
              Spacer(Modifier.padding(start = 14.dp))
              Text(
                text = androidx.compose.ui.res.stringResource(action.titleRes),
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }
        }
      }

      if (paletteOpen) {
        val paletteItems =
          actions.map { action ->
            CommandPaletteItem(
              title = androidx.compose.ui.res.stringResource(action.titleRes),
              icon = action.icon,
              onClick = action.onClick,
            )
          }

        CommandPalette(
          items = paletteItems,
          onDismiss = { paletteOpen = false },
        )
      }
    }
  }
}
