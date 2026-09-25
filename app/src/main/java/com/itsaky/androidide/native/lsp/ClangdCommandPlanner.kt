package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.native.build.NativeCommandSpec
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File

object ClangdCommandPlanner {

  fun plan(
    toolchain: NativeToolchain,
    workspaceRoot: File,
  ): NativeCommandSpec {
    require(workspaceRoot.isDirectory) {
      "Clangd workspace root does not exist: " + workspaceRoot.absolutePath
    }

    val clangd =
      requireNotNull(toolchain.tool(NativeToolId.CLANGD)?.path) {
        "Native tool 'CLANGD' is not available"
      }

    return NativeCommandSpec(
      executable = clangd,
      arguments =
        listOf(
          "--compile-commands-dir=" + workspaceRoot.absolutePath,
          "--background-index=0",
          "--clang-tidy=false",
        ),
      workingDirectory = workspaceRoot,
    )
  }
}
