package com.itsaky.androidide.native.build

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkEntryMergerTest {

  @Test
  fun replacesDexAndAddsAssets() {
    val root = Files.createTempDirectory("apk-merger").toFile()
    try {
      val input = root.resolve("input.apk")
      val output = root.resolve("output.apk")
      val newDex = byteArrayOf(9, 8, 7, 6)
      val asset = root.resolve("hello.txt").apply {
        writeText("hello")
      }

      ZipOutputStream(input.outputStream()).use { zip ->
        zip.putNextEntry(ZipEntry("classes.dex"))
        zip.write(byteArrayOf(1, 2, 3))
        zip.closeEntry()
      }

      val replacement = root.resolve("classes.dex").apply {
        writeBytes(newDex)
      }

      ApkEntryMerger.merge(
        inputApk = input,
        outputApk = output,
        replacementEntries = mapOf("classes.dex" to replacement),
        directory = asset.parentFile,
        extraPrefix = "assets-test-only",
      )

      ZipFile(output).use { zip ->
        assertArrayEquals(
          newDex,
          zip.getInputStream(zip.getEntry("classes.dex")).use { it.readBytes() },
        )
        assertTrue(zip.getEntry("assets-test-only/hello.txt") != null)
      }
    } finally {
      root.deleteRecursively()
    }
  }
}
