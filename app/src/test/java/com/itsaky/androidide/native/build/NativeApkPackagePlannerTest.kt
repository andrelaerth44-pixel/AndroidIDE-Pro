package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.utils.Environment
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeApkPackagePlannerTest {

  @Test
  fun createsResourceThenLinkThenNativeMergePlan() {
    val root = Files.createTempDirectory("native-apk-plan").toFile()
    val previousAapt2 = Environment.AAPT2
    try {
      Environment.AAPT2 = root.resolve("aapt2").apply { writeText("") }

      val res = root.resolve("src/main/res").apply { mkdirs() }
      val manifest = root.resolve("src/main/AndroidManifest.xml").apply { writeText("<manifest/>") }
      val compiled = root.resolve("compiled.zip")
      val unsigned = root.resolve("unsigned.apk")
      val merged = root.resolve("merged.apk")
      val native = root.resolve("libhello.so").apply { writeText("native") }
      val androidJar = root.resolve("android.jar").apply { writeText("") }

      val plan =
        NativeApkPackagePlanner.plan(
          NativeApkPackageRequest(
            moduleRoot = root,
            resourceDirectory = res,
            manifest = manifest,
            compiledResources = compiled,
            unsignedApk = unsigned,
            mergedApk = merged,
            nativeLibrary = native,
            moduleName = "hello",
            abi = AbiTarget.ARM64_V8A,
            androidJar = androidJar,
          )
        )

      assertEquals("compile", plan.compileResources.arguments.first())
      assertEquals("link", plan.linkResources.arguments.first())
      assertEquals(native.absolutePath, plan.mergeNativeLibrary.absolutePath)
      assertTrue(plan.linkResources.arguments.contains(compiled.absolutePath))
    } finally {
      Environment.AAPT2 = previousAapt2
      root.deleteRecursively()
    }
  }
}
