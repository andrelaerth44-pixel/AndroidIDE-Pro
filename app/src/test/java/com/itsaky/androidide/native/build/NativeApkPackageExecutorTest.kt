package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.utils.Environment
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeApkPackageExecutorTest {

  @Test
  fun executesAapt2StagesAndMergesNativeLibrary() {
    val root = Files.createTempDirectory("native-apk-executor").toFile()
    val previousAapt2 = Environment.AAPT2
    try {
      val fakeAapt2 = root.resolve("aapt2")
      fakeAapt2.writeText("")
      Environment.AAPT2 = fakeAapt2

      val resources = root.resolve("res").apply { mkdirs() }
      val manifest = root.resolve("AndroidManifest.xml").apply { writeText("<manifest/>") }
      val compiled = root.resolve("compiled.zip")
      val unsigned = root.resolve("unsigned.apk")
      val merged = root.resolve("merged.apk")
      val native = root.resolve("libhello.so").apply { writeText("native") }
      val androidJar = root.resolve("android.jar").apply { writeText("") }
      ZipOutputStream(unsigned.outputStream()).use { zip ->
        zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
        zip.write(byteArrayOf(1))
        zip.closeEntry()
      }

      val stages = AtomicInteger(0)
      val executor =
        NativeApkPackageExecutor { command, _, _, _ ->
          stages.incrementAndGet()
          if (command.arguments.first() == "compile") {
            compiled.writeText("compiled")
          } else {
            // The fake linker emits a minimal APK that the native merge stage can mutate.
            unsigned.delete()
            ZipOutputStream(unsigned.outputStream()).use { zip ->
              zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
              zip.write(byteArrayOf(1))
              zip.closeEntry()
            }
          }
          NativeProcessResult(0, "ok")
        }

      val result =
        executor.execute(
          request =
            NativeApkPackageRequest(
              moduleRoot = root,
              resourceDirectory = resources,
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

      assertTrue(result.success)
      assertEquals(2, stages.get())
      assertTrue(result.outputApk?.isFile == true)
    } finally {
      Environment.AAPT2 = previousAapt2
      root.deleteRecursively()
    }
  }
}
