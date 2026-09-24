package com.itsaky.androidide.language

enum class BuiltInLanguage(
  val id: String,
  val displayName: String,
  val extensions: Set<String>,
  val nativeCompiler: NativeCompilerKind?
) {
  JAVA("java", "Java", setOf("java"), NativeCompilerKind.JAVA),
  KOTLIN("kotlin", "Kotlin", setOf("kt", "kts"), NativeCompilerKind.KOTLIN),
  C("c", "C", setOf("c", "h"), NativeCompilerKind.C),
  CPP("cpp", "C++", setOf("cc", "cpp", "cxx", "hh", "hpp", "hxx"), NativeCompilerKind.CPP),
  XML("xml", "XML", setOf("xml"), null),
  JSON("json", "JSON", setOf("json"), null),
  MARKDOWN("markdown", "Markdown", setOf("md", "markdown"), null)
}

enum class NativeCompilerKind {
  JAVA,
  KOTLIN,
  C,
  CPP
}
