package com.itsaky.androidide.language

import java.nio.file.Files
import java.nio.file.Path

object NativeLanguageScanner {

  fun scan(roots: Iterable<Path>): NativeLanguageReport {
    val known = linkedMapOf<BuiltInLanguage, MutableList<Path>>()
    val unknown = mutableListOf<Path>()

    roots.forEach { root ->
      if (!Files.exists(root)) return@forEach

      Files.walk(root).use { stream ->
        stream
          .filter(Files::isRegularFile)
          .sorted { a, b -> a.toString().compareTo(b.toString()) }
          .forEach { file ->
            val language = BuiltInLanguageRegistry.find(file)
            if (language == null) {
              val extension = file.fileName.toString().substringAfterLast('.', "")
              if (extension.isNotEmpty() && !extension.matches(Regex("^(iml|gitkeep)$"))) {
                unknown += file
              }
            } else {
              known.getOrPut(language) { mutableListOf() } += file
            }
          }
      }
    }

    return NativeLanguageReport(
      filesByLanguage = known.mapValues { it.value.toList() },
      unknownFiles = unknown.toList()
    )
  }
}
