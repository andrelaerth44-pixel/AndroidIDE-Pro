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

class LanguageBackendRegistry {
  private val backends = linkedMapOf<String, LanguageBackend>()

  fun register(backend: LanguageBackend): Boolean {
    if (backends.containsKey(backend.id)) return false
    backends[backend.id] = backend
    return true
  }

  fun unregister(id: String): LanguageBackend? =
    backends.remove(id)

  fun findFor(
    language: LanguageId,
    requiredCapabilities: Set<BackendCapability> = emptySet()
  ): List<LanguageBackend> =
    backends.values.filter { backend ->
      language in backend.languages &&
        backend.capabilities.containsAll(requiredCapabilities)
    }

  fun all(): List<LanguageBackend> =
    backends.values.toList()
}
