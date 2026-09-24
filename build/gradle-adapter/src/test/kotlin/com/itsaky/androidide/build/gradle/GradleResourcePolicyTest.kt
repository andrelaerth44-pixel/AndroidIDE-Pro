/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.gradle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleResourcePolicyTest {

  @Test
  fun defaultsAreMobileSafe() {
    val policy = GradleResourcePolicy()

    assertEquals(384, policy.maxHeapMb)
    assertEquals(1, policy.maxWorkers)
    assertTrue(policy.toArguments().contains("--no-daemon"))
    assertTrue(policy.toArguments().contains("--max-workers=1"))
    assertTrue(policy.toArguments().contains("--no-parallel"))
    assertTrue(
      policy.toArguments().contains(
        "-Dorg.gradle.jvmargs=-Xmx384m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8"
      )
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun rejectsMoreThanOneWorker() {
    GradleResourcePolicy(maxWorkers = 2)
  }

  @Test(expected = IllegalArgumentException::class)
  fun rejectsExcessiveHeap() {
    GradleResourcePolicy(maxHeapMb = 1024)
  }
}
