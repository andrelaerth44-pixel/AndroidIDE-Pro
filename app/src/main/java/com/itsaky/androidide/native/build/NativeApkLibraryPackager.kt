package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Merges one native shared library into an unsigned APK.
 *
 * Signing is intentionally outside this class: APK v2/v3 signatures must be produced
 * after every ZIP mutation.
 */
object NativeApkLibraryPackager {

  fun merge(
    inputApk: File,
    nativeLibrary: File,
    abi: AbiTarget,
    moduleName: String,
    outputApk: File = inputApk,
  ): File {
    require(inputApk.isFile) {
      "Input APK does not exist: " + inputApk.absolutePath
    }
    require(nativeLibrary.isFile) {
      "Native library does not exist: " + nativeLibrary.absolutePath
    }
    require(nativeLibrary.extension.equals("so", ignoreCase = true)) {
      "Only shared native libraries can be merged into an APK"
    }
    require(moduleName.matches(Regex("[A-Za-z0-9_]+"))) {
      "Invalid native module name: " + moduleName
    }

    val entryName = "lib/" + abi.androidAbiName + "/lib" + moduleName + ".so"
    outputApk.parentFile?.mkdirs()
    val temp =
      File(
        outputApk.parentFile ?: inputApk.parentFile ?: File("."),
        outputApk.name + "." + UUID.randomUUID() + ".tmp",
      )

    try {
      ZipFile(inputApk).use { zip ->
        ZipOutputStream(FileOutputStream(temp)).use { output ->
          zip.entries().asSequence()
            .filterNot { it.name == entryName }
            .forEach { entry ->
              copyEntry(zip, entry, output)
            }

          val nativeEntry =
            ZipEntry(entryName).apply {
              method = ZipEntry.STORED
              time = nativeLibrary.lastModified()
            }
          val checksum = checksum(nativeLibrary)
          nativeEntry.size = nativeLibrary.length()
          nativeEntry.compressedSize = nativeLibrary.length()
          nativeEntry.crc = checksum
          output.putNextEntry(nativeEntry)
          FileInputStream(nativeLibrary).use { input ->
            input.copyTo(output)
          }
          output.closeEntry()
        }
      }

      Files.move(
        temp.toPath(),
        outputApk.toPath(),
        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        java.nio.file.StandardCopyOption.ATOMIC_MOVE,
      )
    } catch (atomicMoveFailure: java.nio.file.AtomicMoveNotSupportedException) {
      Files.move(
        temp.toPath(),
        outputApk.toPath(),
        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
      )
    } finally {
      temp.delete()
    }

    return outputApk
  }

  private fun copyEntry(
    zip: ZipFile,
    source: ZipEntry,
    output: ZipOutputStream,
  ) {
    val target = ZipEntry(source.name).apply {
      time = source.time
      comment = source.comment
      extra = source.extra
      method = source.method
      if (source.method == ZipEntry.STORED) {
        size = source.size
        compressedSize = source.compressedSize
        crc = source.crc
      }
    }

    output.putNextEntry(target)
    zip.getInputStream(source).use { input ->
      input.copyTo(output)
    }
    output.closeEntry()
  }

  private fun checksum(file: File): Long {
    val crc = CRC32()
    FileInputStream(file).use { input ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        crc.update(buffer, 0, count)
      }
    }
    return crc.value
  }
}
