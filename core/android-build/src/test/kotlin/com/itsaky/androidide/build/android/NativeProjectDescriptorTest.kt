package com.itsaky.androidide.build.android

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeProjectDescriptorTest {

  @Test
  fun parsesStandaloneProjectDescriptor() {
    val root = Files.createTempDirectory("androidide-native-project")
    Files.writeString(
      root.resolve(NativeProjectDescriptor.FILE_NAME),
      """
        namespace=com.example.nativeapp
        applicationId=com.example.nativeapp
        compileSdk=35
        minSdk=26
        targetSdk=35
        sdkRoot=/data/sdk
        versionCode=7
        versionName=2.1
        nativeActivity=true
        nativeLibraryName=main
      """.trimIndent()
    )

    val descriptor = NativeProjectDescriptor.load(root)

    assertEquals("com.example.nativeapp", descriptor.namespace)
    assertEquals(35, descriptor.compileSdk)
    assertEquals(26, descriptor.minSdk)
    assertEquals(35, descriptor.targetSdk)
    assertEquals(7, descriptor.versionCode)
    assertEquals("2.1", descriptor.versionName)
    assertTrue(descriptor.nativeActivity)
    assertEquals("main", descriptor.nativeLibraryName)
  }
}
