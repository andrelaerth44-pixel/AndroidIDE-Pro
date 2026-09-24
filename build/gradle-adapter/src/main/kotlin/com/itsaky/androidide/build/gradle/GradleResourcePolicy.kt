/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.gradle

/**
 * Resource limits for the Gradle compatibility backend.
 *
 * Gradle is not the primary AndroidIDE Pro build engine. When compatibility is required,
 * the invocation is constrained to reduce RAM pressure and CPU contention on mobile devices.
 */
data class GradleResourcePolicy(
  val maxHeapMb: Int = DEFAULT_HEAP_MB,
  val maxWorkers: Int = 1,
  val daemonEnabled: Boolean = false,
  val parallelEnabled: Boolean = false,
  val fileSystemWatchEnabled: Boolean = false,
  val buildCacheEnabled: Boolean = false,
) {
  init {
    require(maxHeapMb in MIN_HEAP_MB..MAX_HEAP_MB) {
      "Gradle heap must be between $MIN_HEAP_MB and $MAX_HEAP_MB MiB"
    }
    require(maxWorkers == 1) {
      "Gradle compatibility backend is intentionally limited to one worker"
    }
  }

  fun toArguments(): List<String> = listOf(
    if (daemonEnabled) "--daemon" else "--no-daemon",
    "--max-workers=$maxWorkers",
    if (parallelEnabled) "--parallel" else "--no-parallel",
    if (fileSystemWatchEnabled) "--watch-fs" else "--no-watch-fs",
    if (buildCacheEnabled) "--build-cache" else "--no-build-cache",
    "-Dorg.gradle.workers.max=$maxWorkers",
    "-Dorg.gradle.daemon=$daemonEnabled",
    "-Dorg.gradle.parallel=$parallelEnabled",
    "-Dorg.gradle.vfs.watch=$fileSystemWatchEnabled",
    "-Dorg.gradle.jvmargs=-Xmx" + maxHeapMb +
      "m -XX:MaxMetaspaceSize=" + MAX_METASPACE_MB + "m -Dfile.encoding=UTF-8",
  )

  companion object {
    const val MIN_HEAP_MB = 256
    const val DEFAULT_HEAP_MB = 384
    const val MAX_HEAP_MB = 512
    const val MAX_METASPACE_MB = 256
  }
}
