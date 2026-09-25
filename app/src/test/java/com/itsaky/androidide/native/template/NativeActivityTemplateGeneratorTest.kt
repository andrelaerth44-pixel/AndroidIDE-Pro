package com.itsaky.androidide.native.template

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeActivityTemplateGeneratorTest {

  @Test
  fun generatesNativeActivityProject() {
    val root = Files.createTempDirectory("native-activity").toFile()
    try {
      val generated =
        NativeActivityTemplateGenerator.generate(
          root,
          NativeActivityTemplateSpec(
            packageName = "com.example.nativeapp",
          ),
        )

      assertEquals(4, generated.size)

      val javaFile = root.resolve("src/main/java/com/example/nativeapp/NativeHostActivity.java")
      val cppFile = root.resolve("src/main/cpp/native_activity.cpp")
      val manifestFile = root.resolve("src/main/AndroidManifest.xml")
      val configFile = root.resolve(".androidide/native.json")

      assertTrue(javaFile.isFile)
      assertTrue(cppFile.isFile)
      assertTrue(manifestFile.isFile)
      assertTrue(configFile.isFile)

      assertTrue(javaFile.readText().contains("extends android.app.NativeActivity"))
      assertTrue(cppFile.readText().contains("ANativeActivity_onCreate"))
      assertTrue(manifestFile.readText().contains("android.app.lib_name"))
      assertTrue(manifestFile.readText().contains(root.name))
    } finally {
      root.deleteRecursively()
    }
  }
}
