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

/** Direct D8 port used by the Gradle-free Android pipeline. */
interface Dexer {
  fun dexArchive(
    inputs: List<Path>,
    classpath: List<Path>,
    androidJar: Path,
    minApi: Int,
    release: Boolean,
    outDir: Path,
    threads: Int = 1,
  ): AndroidToolResult

  fun dex(
    inputs: List<Path>,
    androidJar: Path,
    minApi: Int,
    release: Boolean,
    outDir: Path,
    threads: Int = 1,
  ): AndroidToolResult
}
