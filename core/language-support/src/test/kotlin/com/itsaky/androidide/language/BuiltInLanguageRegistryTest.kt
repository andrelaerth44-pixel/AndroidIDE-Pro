package com.itsaky.androidide.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BuiltInLanguageRegistryTest {

  @Test
  fun coreLanguagesAreAlwaysPresent() {
    assertEquals(
      setOf("java", "kotlin", "c", "cpp", "xml", "json", "markdown"),
      BuiltInLanguageRegistry.languages.map(BuiltInLanguage::id).toSet()
    )
  }

  @Test
  fun nativeBuildStatesAreExplicit() {
    assertEquals(
      BuiltInLanguageSupport.NATIVE_BUILD_READY,
      BuiltInLanguage.JAVA.support
    )
    assertEquals(
      BuiltInLanguageSupport.NATIVE_BUILD_READY,
      BuiltInLanguage.KOTLIN.support
    )
    assertEquals(
      BuiltInLanguageSupport.NATIVE_BUILD_REQUIRES_CORE_TOOLCHAIN,
      BuiltInLanguage.C.support
    )
    assertEquals(
      BuiltInLanguageSupport.NATIVE_BUILD_REQUIRES_CORE_TOOLCHAIN,
      BuiltInLanguage.CPP.support
    )
  }

  @Test
  fun compilerLanguagesAreBuiltIn() {
    assertSame(
      BuiltInLanguage.KOTLIN,
      BuiltInLanguageRegistry.findByExtension(".kt")
    )
    assertSame(
      BuiltInLanguage.C,
      BuiltInLanguageRegistry.findByExtension("c")
    )
    assertSame(
      BuiltInLanguage.CPP,
      BuiltInLanguageRegistry.findByExtension("cpp")
    )
  }
}
