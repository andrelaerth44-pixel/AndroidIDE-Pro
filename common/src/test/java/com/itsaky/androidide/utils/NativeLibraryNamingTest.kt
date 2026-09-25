package com.itsaky.androidide.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeLibraryNamingTest {

  @Test
  fun sanitizesProjectNamesForNativeLibraries() {
    assertEquals("app", NativeLibraryNaming.sanitize("app"))
    assertEquals("native_demo", NativeLibraryNaming.sanitize("Native Demo"))
    assertEquals("my_game_2", NativeLibraryNaming.sanitize("My Game 2"))
    assertEquals("native_123demo", NativeLibraryNaming.sanitize("123Demo"))
    assertEquals("native", NativeLibraryNaming.sanitize("   "))
  }
}
