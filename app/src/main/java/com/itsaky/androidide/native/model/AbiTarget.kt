package com.itsaky.androidide.native.model

enum class AbiTarget(
  val androidTriplePrefix: String,
  val androidAbiName: String,
) {
  ARM64_V8A(
    androidTriplePrefix = "aarch64-linux-android",
    androidAbiName = "arm64-v8a",
  ),
}
