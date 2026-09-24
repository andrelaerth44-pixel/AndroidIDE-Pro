package com.itsaky.androidide.plugin.api

import org.junit.Assert.assertFalse
import org.junit.Test

class PluginCapabilityTest {

  @Test
  fun languageAndBuildBackendsAreNotPluginCapabilities() {
    val names = PluginCapability.entries.map { it.name }
    assertFalse(names.any { it.contains("LANGUAGE") })
    assertFalse(names.any { it == "BUILD" || it.endsWith("_BUILD") })
  }
}
