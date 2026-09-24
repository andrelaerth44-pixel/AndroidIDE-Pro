package com.itsaky.androidide.toolchain.api

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Paths

class ToolchainPathsTest {

  @Test
  fun pathsStayInsideToolchainRoot() {
    val root = Paths.get("/data/user/0/com.example/files/toolchains")
    val paths = BuiltInToolchainPaths(root)

    assertEquals(
      root.resolve("kotlin"),
      paths.forKind(ToolchainKind.KOTLIN)
    )
    assertEquals(
      root.resolve("llvm/arm64-v8a/1"),
      paths.forRequest(
        ToolchainRequest(
          ToolchainKind.LLVM,
          "arm64-v8a",
          "1"
        )
      )
    )
  }
}
