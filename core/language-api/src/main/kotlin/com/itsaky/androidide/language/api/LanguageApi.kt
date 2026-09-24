package com.itsaky.androidide.language.api

import java.nio.file.Path

@JvmInline
value class LanguageId(val value: String)

enum class BackendCapability {
  PARSE,
  ERROR_RECOVERY,
  COMPLETION,
  DIAGNOSTICS,
  NAVIGATION,
  REFERENCES,
  FORMATTING,
  REFACTORING,
  INCREMENTAL,
  COMPILE
}

data class CompilationContext(
  val moduleId: String,
  val sourceRoots: List<Path>,
  val classpath: List<Path>,
  val languageLevel: String,
  val properties: Map<String, String> = emptyMap()
)

data class LanguageDiagnostic(
  val file: Path,
  val line: Int,
  val column: Int,
  val severity: Severity,
  val message: String,
  val code: String? = null
) {
  enum class Severity {
    INFO,
    WARNING,
    ERROR
  }
}

interface SourceAnalyzer {
  fun analyze(file: Path): List<LanguageDiagnostic>
}

data class CompileResult(
  val success: Boolean,
  val outputDirectories: List<Path> = emptyList(),
  val diagnostics: List<LanguageDiagnostic> = emptyList()
)

interface SourceCompiler {
  fun compile(files: List<Path>): CompileResult
}

interface LanguageBackend {
  val id: String
  val languages: Set<LanguageId>
  val capabilities: Set<BackendCapability>

  fun createAnalyzer(context: CompilationContext): SourceAnalyzer

  fun createCompiler(context: CompilationContext): SourceCompiler?
}

/**
 * Immutable lookup table for language backends shipped by AndroidIDE Pro.
 *
 * There is intentionally no register/unregister API here. Language backends
 * are assembled by the application itself and are not dynamically installed.
 */
class BuiltInLanguageBackendRegistry(
  backends: Iterable<LanguageBackend>
) {
  private val builtIns: List<LanguageBackend> = backends.toList()

  fun findFor(
    language: LanguageId,
    requiredCapabilities: Set<BackendCapability> = emptySet()
  ): List<LanguageBackend> =
    builtIns.filter { backend ->
      language in backend.languages &&
        backend.capabilities.containsAll(requiredCapabilities)
    }

  fun all(): List<LanguageBackend> = builtIns
}
