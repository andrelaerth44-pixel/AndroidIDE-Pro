package com.itsaky.androidide.templates

import com.itsaky.androidide.templates.impl.nativeActivity.nativeActivityProject
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeActivityTemplateTest {

  @Test
  fun createsGradleFreeNativeActivityProject() {
    val output = Files.createTempDirectory("native-template").toFile()
    try {
      val template = nativeActivityProject()
      val projectName = template.parameters.first { it.name == R.string.project_app_name }
      val packageName = template.parameters.first { it.name == R.string.package_name }
      val saveLocation =
        template.parameters.first { it.name == R.string.wizard_save_location }

      projectName.setValue("Native Demo")
      packageName.setValue("com.example.nativedemo")
      saveLocation.setValue(output.absolutePath)

      val result = template.recipe.execute(TestRecipeExecutor())
      assertTrue(result != null)

      val project = output.resolve("Native Demo")
      assertTrue(
        project.resolve(
          "src/main/java/com/example/nativedemo/NativeHostActivity.java"
        ).isFile
      )
      assertTrue(project.resolve("src/main/cpp/native_activity.cpp").isFile)
      assertTrue(project.resolve("src/main/res").isDirectory)
      assertTrue(project.resolve("src/main/AndroidManifest.xml").isFile)
      assertTrue(project.resolve("src/main/AndroidManifest.xml").readText().contains("android.app.lib_name"))
      assertTrue(project.resolve("src/main/AndroidManifest.xml").readText().contains("native_demo"))
      assertTrue(project.resolve(".androidide/native.json").isFile)
      assertTrue(!project.resolve("build.gradle").exists())
      assertTrue(!project.resolve("settings.gradle").exists())
    } finally {
      output.deleteRecursively()
    }
  }
}
