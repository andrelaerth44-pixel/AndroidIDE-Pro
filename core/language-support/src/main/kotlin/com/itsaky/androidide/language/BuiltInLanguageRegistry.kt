package com.itsaky.androidide.language

import java.nio.file.Files
import java.nio.file.Path

/**
 * The language set is part of AndroidIDE Pro itself.
 *
 * Languages are not discovered, enabled, disabled or installed as plugins.
 * New language implementations become part of the IDE core when they are
 * implemented and shipped.
 */
object BuiltInLanguageRegistry {

  val languages: List<BuiltInLanguage> = BuiltInLanguage.entries.toList()

  private val byExtension: Map<String, BuiltInLanguage> =
    languages
      .flatMap { language -> language.extensions.map { it.lowercase() to language } }
      .toMap()

  fun findByExtension(extension: String): BuiltInLanguage? =
    byExtension[extension.removePrefix(".").lowercase()]

  fun find(path: Path): BuiltInLanguage? =
    findByExtension(path.fileName.toString().substringAfterLast('.', ""))

  fun sourceFiles(root: Path): List<Path> {
    if (!Files.exists(root)) return emptyList()

    return Files.walk(root).use { stream ->
      stream
        .filter(Files::isRegularFile)
        .filter { find(it) != null }
        .sorted { a, b -> a.toString().compareTo(b.toString()) }
        .toList()
    }
  }

  fun compilerLanguages(root: Path): Set<NativeCompilerKind> =
    sourceFiles(root)
      .mapNotNull { find(it)?.nativeCompiler }
      .toSet()
}
