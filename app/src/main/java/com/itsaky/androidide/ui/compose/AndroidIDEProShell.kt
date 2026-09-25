package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AndroidIDEProShell(
  modifier: Modifier = Modifier,
  title: String = "AndroidIDE Pro",
  fileName: String = "MainActivity.kt",
  content: @Composable RowScope.() -> Unit = {},
) {
  AndroidIDETheme {
    Box(
      modifier =
        modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GlassSurface(
          modifier = Modifier.fillMaxWidth().statusBarsPadding().height(58.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(IdeIcons.Code, contentDescription = null)
            Spacer(Modifier.padding(horizontal = 5.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
              fileName,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = {}) {
              Icon(IdeIcons.Search, contentDescription = "Search")
            }
            IconButton(onClick = {}) {
              Icon(IdeIcons.Build, contentDescription = "Build")
            }
          }
        }

        Row(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          GlassPanel(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.08f),
          ) {
            Column(
              modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              GlassIconButton(IdeIcons.Folder, "Explorer")
              GlassIconButton(IdeIcons.Code, "Editor")
              GlassIconButton(IdeIcons.Build, "Build")
              Spacer(Modifier.weight(1f))
            }
          }

          GlassSurface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
          ) {
            Box(
              modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large),
            ) {
              content()
            }
          }
        }

        GlassRow(
          modifier = Modifier.fillMaxWidth().height(54.dp).windowInsetsPadding(WindowInsets.navigationBars),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "Problems",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(
              "0",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.weight(1f))
            Text(
              "Ready",
              color = MaterialTheme.colorScheme.primary,
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun GlassIconButton(
  imageVector: androidx.compose.ui.graphics.vector.ImageVector,
  description: String,
) {
  SurfaceIconButton(
    imageVector = imageVector,
    contentDescription = description,
  )
}

@Composable
private fun SurfaceIconButton(
  imageVector: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
) {
  IconButton(onClick = {}) {
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun AndroidIDEProShellPreview() {
  AndroidIDEProShell(
    content = {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          "Editor",
          color = Color.Gray,
          style = MaterialTheme.typography.headlineSmall,
        )
      }
    },
  )
}
