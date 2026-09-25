package com.itsaky.androidide.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BuildStepState {
  PENDING,
  RUNNING,
  SUCCESS,
  FAILED,
}

enum class BuildLogLevel {
  INFO,
  WARNING,
  ERROR,
  DEBUG,
}

data class BuildStepUi(
  val id: String,
  val title: String,
  val detail: String? = null,
  val state: BuildStepState = BuildStepState.PENDING,
  val progress: Float? = null,
)

data class BuildLogUi(
  val id: String,
  val message: String,
  val level: BuildLogLevel = BuildLogLevel.INFO,
  val timestamp: String? = null,
)

data class BuildIssueUi(
  val id: String,
  val message: String,
  val file: String? = null,
  val line: Int? = null,
  val column: Int? = null,
  val isError: Boolean = true,
)

@Composable
fun AndroidIDEProBuildCenter(
  steps: List<BuildStepUi>,
  isBuilding: Boolean,
  status: String = "",
  onBuild: () -> Unit,
  onStop: () -> Unit,
  canStopBuild: Boolean = false,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  onIssueClick: (BuildIssueUi) -> Unit = {},
  logs: List<BuildLogUi> = emptyList(),
  issues: List<BuildIssueUi> = emptyList(),
  progress: Float? = null,
) {
  var selectedPanel by remember { mutableIntStateOf(0) }

  AndroidIDETheme {
    Column(
      modifier = modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Build Center",
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text =
                  status.ifBlank { if (isBuilding) "Building project…" else "Ready to build" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            IconButton(onClick = onRefresh) {
              Icon(IdeIcons.Refresh, contentDescription = "Refresh build")
            }

            if (isBuilding && canStopBuild) {
              IconButton(onClick = onStop) {
                Icon(IdeIcons.Stop, contentDescription = "Stop build")
              }
            } else if (!isBuilding) {
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

          if (isBuilding) {
            if (progress == null) {
              LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
              LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }

      GlassPanel(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Column(modifier = Modifier.fillMaxSize()) {
          TabRow(selectedTabIndex = selectedPanel) {
            Tab(
              selected = selectedPanel == 0,
              onClick = { selectedPanel = 0 },
              text = { Text("Pipeline") },
            )
            Tab(
              selected = selectedPanel == 1,
              onClick = { selectedPanel = 1 },
              text = { Text("Problems (${issues.size})") },
            )
            Tab(
              selected = selectedPanel == 2,
              onClick = { selectedPanel = 2 },
              text = { Text("Logs (${logs.size})") },
            )
          }

          when (selectedPanel) {
            0 -> PipelineList(steps)
            1 -> ProblemsList(issues, onIssueClick)
            else -> LogsList(logs)
          }
        }
      }
    }
  }
}

@Composable
private fun PipelineList(steps: List<BuildStepUi>) {
  if (steps.isEmpty()) {
    EmptyBuildState("No build steps yet.")
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(vertical = 8.dp),
  ) {
    items(steps, key = { it.id }) { step ->
      BuildStepRow(step)
    }
  }
}

@Composable
private fun ProblemsList(
  issues: List<BuildIssueUi>,
  onIssueClick: (BuildIssueUi) -> Unit,
) {
  if (issues.isEmpty()) {
    EmptyBuildState("No errors or warnings.")
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(vertical = 8.dp),
  ) {
    items(issues, key = { it.id }) { issue ->
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable(enabled = issue.file != null) { onIssueClick(issue) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          imageVector = if (issue.isError) IdeIcons.Error else IdeIcons.Pending,
          contentDescription = null,
          tint = if (issue.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
          Text(issue.message, style = MaterialTheme.typography.bodyMedium)
          val location =
            buildString {
              issue.file?.let { append(it) }
              issue.line?.let {
                if (isNotEmpty()) append(":")
                append(it)
              }
              issue.column?.let {
                if (isNotEmpty()) append(":")
                append(it)
              }
            }
          if (location.isNotBlank()) {
            Text(
              text = location,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LogsList(logs: List<BuildLogUi>) {
  if (logs.isEmpty()) {
    EmptyBuildState("Build output will appear here.")
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(vertical = 8.dp),
  ) {
    items(logs, key = { it.id }) { log ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Text(
          text = log.timestamp ?: log.level.name,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(end = 10.dp),
        )
        Text(
          text = log.message,
          style = MaterialTheme.typography.bodySmall,
          color = logColor(log.level),
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun EmptyBuildState(message: String) {
  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = message,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
private fun BuildStepRow(step: BuildStepUi) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
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
            BuildStepState.FAILED -> MaterialTheme.colorScheme.error
          },
      )

      Column(
        modifier = Modifier.weight(1f).padding(start = 12.dp),
      ) {
        Text(
          text = step.title,
          style = MaterialTheme.typography.bodyLarge,
        )
        if (!step.detail.isNullOrBlank()) {
          Text(
            text = step.detail!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    if (step.state == BuildStepState.RUNNING && step.progress != null) {
      LinearProgressIndicator(
        progress = step.progress.coerceIn(0f, 1f),
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 8.dp),
      )
    }
  }
}

private fun stepStateIcon(state: BuildStepState): ImageVector =
  when (state) {
    BuildStepState.PENDING -> IdeIcons.Pending
    BuildStepState.RUNNING -> Icons.Outlined.PlayArrow
    BuildStepState.SUCCESS -> IdeIcons.Check
    BuildStepState.FAILED -> IdeIcons.Error
  }

@Composable
private fun logColor(level: BuildLogLevel) =
  when (level) {
    BuildLogLevel.INFO -> MaterialTheme.colorScheme.onSurface
    BuildLogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
    BuildLogLevel.ERROR -> MaterialTheme.colorScheme.error
    BuildLogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
  }
