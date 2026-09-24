package com.itsaky.androidide.ui.pro

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itsaky.androidide.models.MainScreenAction

@Composable
fun ProHomeScreen(
  actions: List<MainScreenAction>,
  onAction: (MainScreenAction) -> Unit,
  onLongAction: ((MainScreenAction) -> Unit)? = null
) {
  val primary = actions.take(2)
  val secondary = actions.drop(2)

  AndroidIDEProTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.surface,
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
              MaterialTheme.colorScheme.background
            )
          )
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .padding(horizontal = 20.dp)
      ) {
        Header()

        primary.forEachIndexed { index, action ->
          PrimaryActionCard(
            action = action,
            featured = index == 0,
            onClick = { onAction(action) },
            onLongClick = {
              onLongAction?.invoke(action)
            }
          )
        }

        Text(
          text = stringResource(com.itsaky.androidide.resources.R.string.androidide_pro_workspace),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)
        )

        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(secondary, key = { it.id }) { action ->
            SecondaryActionCard(
              action = action,
              onClick = { onAction(action) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Header() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 28.dp, bottom = 20.dp)
  ) {
    Text(
      text = "AndroidIDE Pro",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Text(
      text = stringResource(com.itsaky.androidide.resources.R.string.androidide_pro_tagline),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 4.dp)
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrimaryActionCard(
  action: MainScreenAction,
  featured: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) {
  val view = LocalView.current
  val container = if (featured) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
  } else {
    MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 12.dp)
      .combinedClickable(
        onClick = onClick,
        onLongClick = {
          if (action.onLongClick != null) {
            action.onLongClick?.invoke(action, view)
          } else {
            onLongClick()
          }
        }
      ),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = container),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        modifier = Modifier.alpha(if (featured) 1f else 0.9f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
      ) {
        Icon(
          painter = painterResource(action.icon),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(14.dp)
        )
      }

      Column(
        modifier = Modifier.padding(start = 16.dp)
      ) {
        Text(
          text = stringResource(action.text),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )

        if (featured) {
          Text(
            text = stringResource(com.itsaky.androidide.resources.R.string.androidide_pro_create_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SecondaryActionCard(
  action: MainScreenAction,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    onClick = onClick,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    ),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        painter = painterResource(action.icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )

      Text(
        text = stringResource(action.text),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
