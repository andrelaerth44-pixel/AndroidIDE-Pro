package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Dialog
import androidx.compose.material3.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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

data class CommandPaletteItem(
  val title: String,
  val subtitle: String? = null,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val onClick: () -> Unit,
)

@Composable
fun CommandPalette(
  items: List<CommandPaletteItem>,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }

  val filteredItems =
    remember(query, items) {
      val normalized = query.trim().lowercase()
      if (normalized.isEmpty()) {
        items
      } else {
        items.filter {
          it.title.lowercase().contains(normalized) ||
            it.subtitle?.lowercase()?.contains(normalized) == true
        }
      }
    }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.28f))
          .padding(horizontal = 18.dp, vertical = 48.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      GlassSurface(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.72f),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
              Icon(Icons.Outlined.Search, contentDescription = null)
            },
            placeholder = { Text("Search commands, files and actions") },
          )

          Text(
            text = if (query.isBlank()) "Quick actions" else "${filteredItems.size} result(s)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
          )

          LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(filteredItems, key = { it.title }) { item ->
              GlassRow(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 1.dp),
              ) {
                Row(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable {
                        onDismiss()
                        item.onClick()
                      }
                      .padding(horizontal = 12.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = null,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                  )
                  if (!item.subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                      text = item.subtitle!!,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                }
              }
            }
          }
        }
      }
    }
  }
}
