package com.itsaky.androidide.native.build

data class NativeCompilationResult(
  val success: Boolean,
  val completedCommands: Int,
  val failedCommand: NativeCommandSpec? = null,
  val processResult: NativeProcessResult? = null,
)
