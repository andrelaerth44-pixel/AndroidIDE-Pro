package com.itsaky.androidide.language.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInLanguageBackendRegistryTest {

  @Test
  fun registryHasNoDynamicMutationApi() {
    val methodNames = BuiltInLanguageBackendRegistry::class.java.methods
      .map { it.name }
      .toSet()

    assertFalse("register" in methodNames)
    assertFalse("unregister" in methodNames)
    assertTrue("findFor" in methodNames)
    assertTrue("all" in methodNames)
  }
}
