package com.itsaky.androidide.native.build

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Properties

/**
 * Small persistent cache for native compiler/linker commands.
 *
 * The cache is deliberately command-centric so changes to source content, compiler
 * flags, target ABI or dependent object files invalidate the corresponding output.
 */
class NativeBuildCache(
  private val stateFile: File,
) {

  private val properties = Properties()

  init {
    if (stateFile.isFile) {
      runCatching {
        FileInputStream(stateFile).use(properties::load)
      }
    }
  }

  @Synchronized
  fun isUpToDate(command: NativeCommandSpec): Boolean {
    val output = outputFile(command) ?: return false
    if (!output.isFile) return false

    val key = output.absolutePath
    val expected = fingerprint(command)
    return properties.getProperty(key) == expected
  }

  @Synchronized
  fun record(command: NativeCommandSpec) {
    val output = outputFile(command) ?: return
    properties.setProperty(output.absolutePath, fingerprint(command))
    stateFile.parentFile?.mkdirs()
    FileOutputStream(stateFile).use {
      properties.store(it, "AndroidIDE Pro native build cache")
    }
  }

  private fun fingerprint(command: NativeCommandSpec): String {
    val digest = MessageDigest.getInstance("SHA-256")

    command.asCommandLine().forEach { argument ->
      digest.update(argument.toByteArray())
      digest.update(0)
    }

    command.executable
      .takeIf(File::isFile)
      ?.let { hashFile(it, digest) }

    commandInputFiles(command)
      .plus(commandHeaderFiles(command))
      .distinct()
      .sortedBy(File::getAbsolutePath)
      .forEach { hashFile(it, digest) }

    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun hashFile(
    file: File,
    digest: MessageDigest,
  ) {
    digest.update(file.absolutePath.toByteArray())
    digest.update(0)
    digest.update(file.length().toString().toByteArray())
    digest.update(0)
    FileInputStream(file).use { input ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
      }
    }
  }

  private fun commandHeaderFiles(command: NativeCommandSpec): List<File> {
    val includeRoots =
      command.arguments
        .filter { it.startsWith("-I") && it.length > 2 }
        .map { File(it.substring(2)) }
        .filter(File::isDirectory)

    return includeRoots
      .asSequence()
      .flatMap { root ->
        root.walkTopDown()
          .filter { it.isFile && it.extension.lowercase() in HEADER_EXTENSIONS }
      }
      .toList()
  }

  private fun commandInputFiles(command: NativeCommandSpec): List<File> {
    val inputs = mutableListOf<File>()

    command.arguments
      .windowed(2, 1)
      .firstOrNull { it.first() == "-c" }
      ?.last()
      ?.let { File(it) }
      ?.takeIf(File::isFile)
      ?.let(inputs::add)

    command.arguments
      .filter { it.endsWith(".o") }
      .map(::File)
      .filter(File::isFile)
      .forEach(inputs::add)

    return inputs.distinct()
  }

  private val HEADER_EXTENSIONS = setOf("h", "hh", "hpp", "hxx")

  private fun outputFile(command: NativeCommandSpec): File? =
    command.arguments
      .windowed(2, 1)
      .firstOrNull { it.first() == "-o" }
      ?.last()
      ?.let(::File)
}

