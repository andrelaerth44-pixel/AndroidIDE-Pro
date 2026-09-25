package com.itsaky.androidide.native.build

import java.io.File

data class NativeBuildResult(
  val success: Boolean,
  val executedTasks: List<String>,
  val failedTaskId: String? = null,
  val message: String? = null,
  val outputFile: File? = null,
)
