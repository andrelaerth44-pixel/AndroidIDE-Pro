package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
  AndroidIDETheme {
    Column(
      modifier = modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      GlassSurface(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(Modifier.padding(20.dp)) {
          Text(
            text = "AndroidIDE Pro",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "A fast, native Android IDE.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(actions) { action ->
          GlassRow(
            modifier = Modifier.fillMaxWidth(),
          ) {
            TextButton(
              onClick = action.onClick,
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
              Icon(
                imageVector = action.icon,
                contentDescription = null,
              )
              androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
              Text(
                text = androidx.compose.ui.res.stringResource(action.titleRes),
                style = MaterialTheme.typography.titleMedium,
              )
              androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}
