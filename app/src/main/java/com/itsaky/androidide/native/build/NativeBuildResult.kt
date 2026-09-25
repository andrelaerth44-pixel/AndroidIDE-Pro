package com.itsaky.androidide.native.build

data class NativeBuildResult(
  val success: Boolean,
  val executedTasks: List<String>,
  val failedTaskId: String? = null,
  val message: String? = null,
)
