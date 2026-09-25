package com.itsaky.androidide.native.model

enum class AbiTarget(
  val androidTriplePrefix: String,
  val androidAbiName: String,
) {
  ARM64_V8A(
    androidTriplePrefix = "aarch64-linux-android",
    androidAbiName = "arm64-v8a",
  ),
  ARMEABI_V7A(
    androidTriplePrefix = "armv7a-linux-androideabi",
    androidAbiName = "armeabi-v7a",
  ),
  X86(
    androidTriplePrefix = "i686-linux-android",
    androidAbiName = "x86",
  ),
  X86_64(
    androidTriplePrefix = "x86_64-linux-android",
    androidAbiName = "x86_64",
  );

  companion object {
    fun fromAndroidAbi(name: String): AbiTarget? =
      values().firstOrNull { it.androidAbiName == name }
  }
}
