package com.itsaky.androidide.ui.compose

data class BuildCenterUiState(
  val isBuilding: Boolean = false,
  val status: String = "Ready",
  val progress: Float? = null,
  val steps: List<BuildStepUi> = emptyList(),
  val logs: List<BuildLogUi> = emptyList(),
  val issues: List<BuildIssueUi> = emptyList(),
)

