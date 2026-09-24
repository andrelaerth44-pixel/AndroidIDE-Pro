package com.itsaky.androidide.build.android

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeBuildConfigurationTest {

  @Test
  fun loadsRelativeNativeBuildConfiguration() {
    val root = Files.createTempDirectory("androidide-native-config")
    val cpp = root.resolve("src/main/cpp")
    Files.createDirectories(cpp)

    Files.writeString(
      cpp.resolve("androidide-native.properties"),
      """
        libraryName=brushengine
        includeDirs=include,third_party/foo/include
        libraryDirs=third_party/foo/lib
        linkLibraries=log,android,GLESv3
        staticLibraries=libs/libbrush.a
        cFlags=-Wall -Wextra
        cppFlags=-Wall -Wextra -fno-exceptions
        linkerFlags=-Wl,--gc-sections
      """.trimIndent()
    )

    val configuration = NativeBuildConfiguration.load(root)

    assertEquals("brushengine", configuration.libraryName)
    assertEquals(2, configuration.includeDirs.size)
    assertTrue(
      configuration.includeDirs.first()
        .endsWith("src/main/cpp/include")
    )
    assertEquals(
      listOf("log", "android", "GLESv3"),
      configuration.linkLibraries
    )
    assertTrue(
      configuration.staticLibraries.single()
        .endsWith("src/main/cpp/libs/libbrush.a")
    )
    assertEquals(listOf("-Wall", "-Wextra"), configuration.cFlags)
    assertEquals(
      listOf("-Wall", "-Wextra", "-fno-exceptions"),
      configuration.cppFlags
    )
  }
}
