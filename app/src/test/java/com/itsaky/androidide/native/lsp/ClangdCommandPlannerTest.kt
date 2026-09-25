package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.toolchain.NativeTool
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClangdCommandPlannerTest {

  @Test
  fun commandPointsClangdAtWorkspaceCompilationDatabase() {
    val root = Files.createTempDirectory("clangd-launch").toFile()
    try {
      val clangd = File(root, "clangd")
      val toolchain =
        NativeToolchain(
          root = root,
          sysroot = File(root, "sysroot"),
          tools =
            listOf(
              NativeTool(
                id = NativeToolId.CLANGD,
                displayName = "Clangd",
                path = clangd,
              )
            ),
        )

      val command = ClangdCommandPlanner.plan(toolchain, root)

      assertEquals(clangd.absolutePath, command.executable.absolutePath)
      assertEquals(root.absolutePath, command.workingDirectory?.absolutePath)
      assertTrue(
        command.arguments.contains(
          "--compile-commands-dir=" + root.absolutePath
        )
      )
      assertTrue(command.arguments.contains("--background-index=0"))
    } finally {
      root.deleteRecursively()
    }
  }
}
