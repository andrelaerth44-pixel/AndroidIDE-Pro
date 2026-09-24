/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AndroidBuildTaskNamesTest {

  @Test
  fun debugApkOrderMatchesObservedPipeline() {
    assertEquals(17, AndroidBuildTaskNames.DEBUG_APK_ORDER.size)
    assertEquals("generateSources", AndroidBuildTaskNames.DEBUG_APK_ORDER.first())
    assertEquals("assemble", AndroidBuildTaskNames.DEBUG_APK_ORDER.last())
    assertEquals("packageApk", AndroidBuildTaskNames.DEBUG_APK_ORDER[14])
    assertEquals("sign", AndroidBuildTaskNames.DEBUG_APK_ORDER[15])
  }

  @Test
  fun taskNameGetsVariantSuffix() {
    assertEquals(
      ":app:compileJavaDebug",
      AndroidBuildTaskNames.forModule("app", AndroidBuildTaskNames.COMPILE_JAVA, "debug"),
    )
  }

  @Test
  fun invalidArgumentsAreRejected() {
    assertThrows(IllegalArgumentException::class.java) {
      AndroidBuildTaskNames.forVariant("", "debug")
    }
    assertThrows(IllegalArgumentException::class.java) {
      AndroidBuildTaskNames.forModule("app", AndroidBuildTaskNames.ASSEMBLE, "")
    }
  }
}
