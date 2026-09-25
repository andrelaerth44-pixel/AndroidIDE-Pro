package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeApkLibraryPackagerTest {

  @Test
  fun mergesArm64LibraryAsStoredZipEntry() {
    val root = Files.createTempDirectory("apk-native-merge").toFile()
    try {
      val apk = root.resolve("input.apk")
      val library = root.resolve("libapp.so")
      val output = root.resolve("output.apk")
      val nativeBytes = "native-library".toByteArray()
      library.writeBytes(nativeBytes)

      ZipOutputStream(apk.outputStream()).use { zip ->
        zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
        zip.write(byteArrayOf(1, 2, 3))
        zip.closeEntry()
        zip.putNextEntry(ZipEntry("classes.dex"))
        zip.write(byteArrayOf(4, 5, 6))
        zip.closeEntry()
      }

      NativeApkLibraryPackager.merge(
        inputApk = apk,
        nativeLibrary = library,
        abi = AbiTarget.ARM64_V8A,
        moduleName = "app",
        outputApk = output,
      )

      ZipFile(output).use { zip ->
        val entry = zip.getEntry("lib/arm64-v8a/libapp.so")
        assertNotNull(entry)
        assertEquals(ZipEntry.STORED, entry.method)
        assertEquals(nativeBytes.size.toLong(), entry.size)
        assertArrayEquals(nativeBytes, zip.getInputStream(entry).use { it.readBytes() })
        assertTrue(zip.getEntry("AndroidManifest.xml") != null)
        assertTrue(zip.getEntry("classes.dex") != null)
      }
    } finally {
      root.deleteRecursively()
    }
  }
}
