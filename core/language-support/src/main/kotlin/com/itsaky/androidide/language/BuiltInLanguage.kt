package com.itsaky.androidide.language

enum class BuiltInLanguageSupport {
  NATIVE_BUILD_READY,
  NATIVE_BUILD_REQUIRES_CORE_TOOLCHAIN,
  EDITOR_AND_ANALYSIS_ONLY
}

enum class BuiltInLanguage(
  val id: String,
  val displayName: String,
  val extensions: Set<String>,
  val nativeCompiler: NativeCompilerKind?,
  val support: BuiltInLanguageSupport
) {
  JAVA(
    "java",
    "Java",
    setOf("java"),
    NativeCompilerKind.JAVA,
    BuiltInLanguageSupport.NATIVE_BUILD_READY
  ),
  KOTLIN(
    "kotlin",
    "Kotlin",
    setOf("kt", "kts"),
    NativeCompilerKind.KOTLIN,
    BuiltInLanguageSupport.NATIVE_BUILD_READY
  ),
  C(
    "c",
    "C",
    setOf("c", "h"),
    NativeCompilerKind.C,
    BuiltInLanguageSupport.NATIVE_BUILD_REQUIRES_CORE_TOOLCHAIN
  ),
  CPP(
    "cpp",
    "C++",
    setOf("cc", "cpp", "cxx", "hh", "hpp", "hxx"),
    NativeCompilerKind.CPP,
    BuiltInLanguageSupport.NATIVE_BUILD_REQUIRES_CORE_TOOLCHAIN
  ),
  XML(
    "xml",
    "XML",
    setOf("xml"),
    null,
    BuiltInLanguageSupport.NATIVE_BUILD_READY
  ),
  JSON(
    "json",
    "JSON",
    setOf("json"),
    null,
    BuiltInLanguageSupport.EDITOR_AND_ANALYSIS_ONLY
  ),
  MARKDOWN(
    "markdown",
    "Markdown",
    setOf("md", "markdown"),
    null,
    BuiltInLanguageSupport.EDITOR_AND_ANALYSIS_ONLY
  )
}

enum class NativeCompilerKind {
  JAVA,
  KOTLIN,
  C,
  CPP
}
