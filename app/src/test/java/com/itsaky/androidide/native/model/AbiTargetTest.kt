package com.itsaky.androidide.native.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AbiTargetTest {

  @Test
  fun resolvesCanonicalAndroidAbiNames() {
    assertEquals(AbiTarget.ARM64_V8A, AbiTarget.fromAndroidAbi("arm64-v8a"))
    assertEquals(AbiTarget.ARMEABI_V7A, AbiTarget.fromAndroidAbi("armeabi-v7a"))
    assertEquals(AbiTarget.X86, AbiTarget.fromAndroidAbi("x86"))
    assertEquals(AbiTarget.X86_64, AbiTarget.fromAndroidAbi("x86_64"))
    assertNull(AbiTarget.fromAndroidAbi("mips"))
  }
}
