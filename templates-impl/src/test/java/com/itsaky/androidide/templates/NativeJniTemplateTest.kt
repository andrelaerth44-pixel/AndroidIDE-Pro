package com.itsaky.androidide.templates

import com.itsaky.androidide.templates.impl.nativeJni.nativeJniProject
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeJniTemplateTest {

  @Test
  fun createsGradleFreeJniProject() {
    val output = Files.createTempDirectory("native-jni-template").toFile()
    try {
      val template = nativeJniProject()
      template.parameters.first { it.name == R.string.project_app_name }
        .setValue("JNI Demo")
      template.parameters.first { it.name == R.string.package_name }
        .setValue("com.example.jnidemo")
      template.parameters.first { it.name == R.string.wizard_save_location }
        .setValue(output.absolutePath)

      val result = template.recipe.execute(TestRecipeExecutor())
      assertTrue(result != null)

      val project = output.resolve("JNI Demo")
      assertTrue(project.resolve("src/main/java/com/example/jnidemo/NativeBridge.java").isFile)
      assertTrue(project.resolve("src/main/java/com/example/jnidemo/MainActivity.java").isFile)
      assertTrue(project.resolve("src/main/cpp/native_bridge.cpp").isFile)
      assertTrue(project.resolve("src/main/res").isDirectory)
      assertTrue(project.resolve("src/main/AndroidManifest.xml").isFile)
      assertTrue(
        project.resolve("src/main/java/com/example/jnidemo/NativeBridge.java")
          .readText()
          .contains("System.loadLibrary(\"jni_demo\")")
      )
      assertTrue(project.resolve(".androidide/native.json").isFile)
      assertTrue(project.resolve("build.gradle").exists().not())
    } finally {
      output.deleteRecursively()
    }
  }
}