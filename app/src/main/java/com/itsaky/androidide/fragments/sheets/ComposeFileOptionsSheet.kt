package com.itsaky.androidide.fragments.sheets

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.drawable.toBitmap
import com.itsaky.androidide.events.FileContextMenuItemClickEvent
import com.itsaky.androidide.models.SheetOption
import com.itsaky.androidide.ui.compose.AndroidIDETheme
import com.itsaky.androidide.ui.compose.GlassPanel
import org.greenrobot.eventbus.EventBus

class ComposeFileOptionsSheet : BaseBottomSheetFragment() {

  private val options = mutableStateListOf<SheetOption>()
  private var dismissOnItemClick = true

  fun addOption(option: SheetOption) {
    if (!options.contains(option)) {
      options += option
    }
  }

  fun setDismissOnItemClick(dismiss: Boolean): ComposeFileOptionsSheet {
    dismissOnItemClick = dismiss
    return this
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View =
    ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AndroidIDETheme {
          GlassPanel(
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              LazyColumn(
                modifier = Modifier.fillMaxWidth(),
              ) {
                items(options, key = { it.id }) { option ->
                  FileOptionRow(
                    option = option,
                    onClick = {
                      if (dismissOnItemClick) dismiss()

                      val event = FileContextMenuItemClickEvent(option)
                      event.put(Context::class.java, requireContext())
                      EventBus.getDefault().post(event)
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
}

@Composable
private fun FileOptionRow(
  option: SheetOption,
  onClick: () -> Unit,
) {
  val destructive = option.id?.contains("delete", ignoreCase = true) == true
  val contentColor =
    if (destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface

  val iconBitmap =
    remember(option.icon) {
      option.icon.toBitmap(width = 48, height = 48).asImageBitmap()
    }

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    androidx.compose.foundation.Image(
      bitmap = iconBitmap,
      contentDescription = null,
      modifier = Modifier.size(22.dp),
      colorFilter = ColorFilter.tint(contentColor),
    )

    Spacer(Modifier.width(16.dp))

    Text(
      text = option.title,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyLarge,
      color = contentColor,
    )
  }
}
