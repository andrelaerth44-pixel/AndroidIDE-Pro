package com.itsaky.androidide.native.build

import org.junit.Assert.assertNotNull
import org.junit.Test

class AndroidBuildToolchainTest {

  @Test
  fun locatorReturnsSnapshotWithoutThrowing() {
    assertNotNull(AndroidBuildToolchainLocator.locate())
  }
}
