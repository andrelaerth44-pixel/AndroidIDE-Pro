package com.itsaky.androidide.native.model

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NativeProjectConfigTest {

  @Test
  fun defaultConfigRoundTrips() {
    val module = Files.createTempDirectory("native-config").toFile()
    try {
      val file =
        NativeProjectConfigStore.writeDefault(
          moduleRoot = module,
          libraryType = NativeLibraryType.STATIC,
        )

      assertNotNull(file)
      val loaded = NativeProjectConfigStore.load(module)
      assertNotNull(loaded)
      assertEquals(NativeLibraryType.STATIC, loaded?.libraryType)
    } finally {
      module.deleteRecursively()
    }
  }
}
