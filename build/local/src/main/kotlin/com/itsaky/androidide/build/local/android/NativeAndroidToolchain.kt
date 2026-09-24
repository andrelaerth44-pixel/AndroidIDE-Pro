/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import java.nio.file.Path

/** Explicit Android SDK/toolchain inputs for the Gradle-free backend. */
data class NativeAndroidToolchain(
  val aapt2: Path,
  val androidJar: Path,
  val zipalign: Path? = null,
  val apksigner: Path? = null,
  val debugSigning: ApkSigningInputs? = null,
) {
  fun canCompile(): Boolean = aapt2.toFile().isFile && androidJar.toFile().isFile

  fun canSignDebug(): Boolean =
    canCompile() &&
      zipalign?.toFile()?.isFile == true &&
      apksigner?.toFile()?.isFile == true &&
      debugSigning != null
}
