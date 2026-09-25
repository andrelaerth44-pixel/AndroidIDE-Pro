package com.itsaky.androidide.native.build

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeBuildCacheTest {

  @Test
  fun recordsAndRecognizesUnchangedCommandInputs() {
    val root = Files.createTempDirectory("native-cache").toFile()
    try {
      val source = File(root, "native.cpp").apply { writeText("int value() { return 1; }") }
      val output = File(root, "native.o").apply { writeText("object") }
      val header = File(root, "native.hpp").apply { writeText("int value();") }
      val command =
        NativeCommandSpec(
          executable = File("/toolchain/clang++"),
          arguments = listOf(
            "-std=c++20",
            "-I" + root.absolutePath,
            "-c",
            source.absolutePath,
            "-o",
            output.absolutePath,
          ),
          workingDirectory = root,
        )
      val cache = NativeBuildCache(File(root, "cache.properties"))

      assertFalse(cache.isUpToDate(command))
      cache.record(command)
      assertTrue(cache.isUpToDate(command))

      source.writeText("int value() { return 2; }")
      assertFalse(cache.isUpToDate(command))

      cache.record(command)
      header.writeText("int value(); // changed")
      assertFalse(cache.isUpToDate(command))
    } finally {
      root.deleteRecursively()
    }
  }
}
