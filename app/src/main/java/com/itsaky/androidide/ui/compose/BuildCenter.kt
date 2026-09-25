package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BuildStepState {
  PENDING,
  RUNNING,
  SUCCESS,
  UP_TO_DATE,
  FAILED,
  BLOCKED,
}

fun buildStepStateLabel(state: BuildStepState): String =
  when (state) {
    BuildStepState.PENDING -> "Pending"
    BuildStepState.RUNNING -> "Running"
    BuildStepState.SUCCESS -> "Success"
    BuildStepState.UP_TO_DATE -> "Up to date"
    BuildStepState.FAILED -> "Failed"
    BuildStepState.BLOCKED -> "Blocked"
  }

data class BuildStepUi(
  val id: String,
  val title: String,
  val detail: String? = null,
  val state: BuildStepState = BuildStepState.PENDING,
)

@Composable
fun AndroidIDEProBuildCenter(
  steps: List<BuildStepUi>,
  isBuilding: Boolean,
  onBuild: () -> Unit,
  onStop: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AndroidIDETheme {
    Column(
      modifier = modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Build Center",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = if (isBuilding) "Building project…" else "Ready to build",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          IconButton(onClick = onRefresh) {
            Icon(IdeIcons.Refresh, contentDescription = "Refresh build")
          }

          if (isBuilding) {
            IconButton(onClick = onStop) {
              Icon(IdeIcons.Stop, contentDescription = "Stop build")
            }
          } else {
            Button(
              onClick = onBuild,
              contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
              Icon(
                imageVector = IdeIcons.Play,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.size(6.dp))
              Text("Build")
            }
          }
        }
      }

      GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "Pipeline",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )

          LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
          ) {
            items(steps, key = { it.id }) { step ->
              BuildStepRow(step)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BuildStepRow(step: BuildStepUi) {
  GlassRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = stepStateIcon(step.state),
      contentDescription = null,
      modifier = Modifier.size(20.dp),
      tint =
        when (step.state) {
          BuildStepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
          BuildStepState.RUNNING -> MaterialTheme.colorScheme.primary
          BuildStepState.SUCCESS -> MaterialTheme.colorScheme.primary
          BuildStepState.UP_TO_DATE -> MaterialTheme.colorScheme.primary
          BuildStepState.FAILED -> MaterialTheme.colorScheme.error
          BuildStepState.BLOCKED -> MaterialTheme.colorScheme.error
        },
    )

    Column(
      modifier = Modifier.weight(1f).padding(start = 12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = step.title,
          style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = buildStepStateLabel(step.state),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (!step.detail.isNullOrBlank()) {
        Text(
          text = step.detail!!,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
  }
}

private fun stepStateIcon(state: BuildStepState): ImageVector =
  when (state) {
    BuildStepState.PENDING -> IdeIcons.Pending
    BuildStepState.RUNNING -> IdeIcons.Play
    BuildStepState.SUCCESS -> IdeIcons.Check
    BuildStepState.UP_TO_DATE -> IdeIcons.Check
    BuildStepState.FAILED -> IdeIcons.Error
    BuildStepState.BLOCKED -> IdeIcons.Error
  }
