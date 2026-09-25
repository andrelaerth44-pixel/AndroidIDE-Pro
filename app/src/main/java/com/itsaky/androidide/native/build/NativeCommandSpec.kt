package com.itsaky.androidide.native.build

import java.io.File

data class NativeCommandSpec(
  val executable: File,
  val arguments: List<String>,
  val workingDirectory: File? = null,
) {
  fun asCommandLine(): List<String> =
    listOf(executable.absolutePath) + arguments
}
