package com.itsaky.androidide.native.build

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkEntryMerger {

  fun merge(
    inputApk: File,
    outputApk: File,
    replacementEntries: Map<String, File>,
    directory: File? = null,
    extraPrefix: String? = null,
  ): File {
    require(inputApk.isFile) { "Input APK does not exist: " + inputApk.absolutePath }
    outputApk.parentFile?.mkdirs()

    val temp =
      File(
        outputApk.parentFile ?: inputApk.parentFile ?: File("."),
        outputApk.name + "." + UUID.randomUUID() + ".tmp",
      )

    val normalized = replacementEntries.mapKeys { it.key.removePrefix("/") }
      .filterValues(File::isFile)

    try {
      ZipFile(inputApk).use { zip ->
        ZipOutputStream(FileOutputStream(temp)).use { out ->
          zip.entries().asSequence()
            .filterNot { normalized.containsKey(it.name) }
            .forEach { entry ->
              copy(zip, entry, out)
            }

          normalized.toSortedMap().forEach { (name, file) ->
            writeStored(out, name, file)
          }

          if (directory?.isDirectory == true) {
            directory.walkTopDown()
              .filter(File::isFile)
              .sortedBy(File::getAbsolutePath)
              .forEach { file ->
                val relative =
                  file.relativeTo(directory).invariantSeparatorsPath
                val name =
                  (extraPrefix.orEmpty().trim('/').takeIf { it.isNotBlank() }?.let { "$it/" } ?: "") +
                    relative
                if (!normalized.containsKey(name)) {
                  writeStored(out, name, file)
                }
              }
          }
        }
      }

      runCatching {
        Files.move(
          temp.toPath(),
          outputApk.toPath(),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING,
          java.nio.file.StandardCopyOption.ATOMIC_MOVE,
        )
      }.getOrElse {
        Files.move(
          temp.toPath(),
          outputApk.toPath(),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        )
      }
    } finally {
      temp.delete()
    }

    return outputApk
  }

  private fun copy(zip: ZipFile, entry: ZipEntry, out: ZipOutputStream) {
    val target =
      ZipEntry(entry.name).apply {
        time = entry.time
        comment = entry.comment
        extra = entry.extra
        method = entry.method
        if (entry.method == ZipEntry.STORED) {
          size = entry.size
          compressedSize = entry.compressedSize
          crc = entry.crc
        }
      }

    out.putNextEntry(target)
    zip.getInputStream(entry).use { it.copyTo(out) }
    out.closeEntry()
  }

  private fun writeStored(out: ZipOutputStream, name: String, file: File) {
    val entry =
      ZipEntry(name).apply {
        method = ZipEntry.STORED
        size = file.length()
        compressedSize = file.length()
        crc = crc(file)
        time = file.lastModified()
      }
    out.putNextEntry(entry)
    FileInputStream(file).use { it.copyTo(out) }
    out.closeEntry()
  }

  private fun crc(file: File): Long {
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
