package com.itsaky.androidide.language

import java.nio.file.Path

data class NativeLanguageReport(
  val filesByLanguage: Map<BuiltInLanguage, List<Path>>,
  val unknownFiles: List<Path>
) {
  val compilerKinds: Set<NativeCompilerKind>
    get() = filesByLanguage.keys.mapNotNull { it.nativeCompiler }.toSet()

  val hasSources: Boolean
    get() = filesByLanguage.values.any { it.isNotEmpty() }
}
