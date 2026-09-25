package com.itsaky.androidide.native.build

import com.itsaky.androidide.utils.Environment
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Aapt2CommandPlannerTest {

  @Test
  fun compilePlanUsesDirMode() {
    val root = Files.createTempDirectory("aapt2-compile").toFile()
    try {
      val executable = root.resolve("aapt2")
      executable.writeText("")
      val old = Environment.AAPT2
      Environment.AAPT2 = executable

      val resourceDir = root.resolve("res").apply { mkdirs() }
      val output = root.resolve("compiled.zip")
      val plan = Aapt2CommandPlanner.planCompile(resourceDir, output)

      assertEquals(executable.absolutePath, plan.executable.absolutePath)
      assertEquals("compile", plan.arguments[0])
      assertTrue(plan.arguments.contains("--dir"))
      assertTrue(plan.arguments.contains(resourceDir.absolutePath))
      assertTrue(plan.arguments.contains(output.absolutePath))

      Environment.AAPT2 = old
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun linkPlanUsesPlatformJarAndCompiledZip() {
    val root = Files.createTempDirectory("aapt2-link").toFile()
    try {
      val executable = root.resolve("aapt2").apply { writeText("") }
      val old = Environment.AAPT2
      Environment.AAPT2 = executable

      val compiled = root.resolve("compiled.zip").apply { writeText("") }
      val manifest = root.resolve("AndroidManifest.xml").apply { writeText("<manifest/>") }
      val androidJar = root.resolve("android.jar").apply { writeText("") }
      val output = root.resolve("unsigned.apk")
      val javaOut = root.resolve("generated")

      val plan =
        Aapt2CommandPlanner.planLink(
          compiledResources = compiled,
          manifest = manifest,
          androidJar = androidJar,
          outputApk = output,
          javaSourceOutput = javaOut,
          minSdk = 28,
          targetSdk = 35,
        )

      assertEquals("link", plan.arguments[0])
      assertTrue(plan.arguments.contains("--manifest"))
      assertTrue(plan.arguments.contains("-I"))
      assertTrue(plan.arguments.contains(androidJar.absolutePath))
      assertTrue(plan.arguments.contains("-R"))
      assertTrue(plan.arguments.contains(compiled.absolutePath))
      assertTrue(plan.arguments.contains("--java"))
      assertTrue(plan.arguments.contains(javaOut.absolutePath))
      assertTrue(plan.arguments.contains("--min-sdk-version"))
      assertTrue(plan.arguments.contains("28"))
      assertTrue(plan.arguments.contains("--target-sdk-version"))
      assertTrue(plan.arguments.contains("35"))

      Environment.AAPT2 = old
    } finally {
      root.deleteRecursively()
    }
  }
}
