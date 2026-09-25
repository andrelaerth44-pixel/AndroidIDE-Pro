package com.itsaky.androidide.native.build

data class NativeProcessResult(
  val exitCode: Int,
  val output: String,
  val durationMs: Long,
) {
  val success: Boolean
    get() = exitCode == 0
}
