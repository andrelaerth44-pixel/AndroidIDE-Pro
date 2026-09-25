package com.itsaky.androidide.native.build

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidProjectModelTest {
  @Test
  fun loaderFindsAndroidSourcesAndManifestPackage() {
    val root = Files.createTempDirectory("android-project-model").toFile()
    try {
      val main = root.resolve("src/main")
      main.resolve("res/values").mkdirs()
      main.resolve("java/com/example").mkdirs()
      main.resolve("kotlin/com/example").mkdirs()
      main.resolve("AndroidManifest.xml").writeText(
        """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.app"></manifest>"""
      )
      main.resolve("java/com/example/MainActivity.java").writeText("class MainActivity {}")
      main.resolve("kotlin/com/example/Screen.kt").writeText("class Screen")

      val model = checkNotNull(AndroidProjectModelLoader.load(root))
      assertEquals("com.example.app", model.applicationId)
      assertEquals(1, model.javaSources.size)
      assertEquals(1, model.kotlinSources.size)
      assertTrue(model.hasJava)
      assertTrue(model.hasKotlin)
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun loaderRejectsIncompleteModule() {
    val root = Files.createTempDirectory("android-project-invalid").toFile()
    try {
      assertEquals(null, AndroidProjectModelLoader.load(root))
    } finally {
      root.deleteRecursively()
    }
  }
}
