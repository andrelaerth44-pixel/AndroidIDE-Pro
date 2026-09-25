package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GlassShape = RoundedCornerShape(20.dp)

private val GlassContainerAlpha = 0.82f
private val GlassBorderAlpha = 0.18f

@Composable
private fun glassColor(): Color =
  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GlassContainerAlpha)

@Composable
private fun glassBorder(): BorderStroke =
  BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline.copy(alpha = GlassBorderAlpha),
  )

@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier,
    shape = GlassShape,
    color = glassColor(),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = glassBorder(),
    content = { androidx.compose.foundation.layout.Row { content() } },
  )
}

@Composable
fun GlassPanel(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier,
    shape = GlassShape,
    color = glassColor(),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = glassBorder(),
    content = { androidx.compose.foundation.layout.Column { content() } },
  )
}

@Composable
fun GlassRow(
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    color = glassColor(),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = glassBorder(),
    content = content,
  )
}
