package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.native.build.NativeCommandFactory
import com.itsaky.androidide.toolchain.NativeTool
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchain
import com.itsaky.androidide.native.model.AbiTarget
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClangdCompilationDatabaseTest {

  @Test
  fun databaseContainsCompileArguments() {
    val root = Files.createTempDirectory("clangd-db").toFile()
    try {
      val source = File(root, "native.cpp")
      val output = File(root, "native.o")

      val toolchain =
        NativeToolchain(
          root = File("/toolchain"),
          sysroot = File("/toolchain/sysroot"),
          tools =
            listOf(
              NativeTool(
                id = NativeToolId.CLANGXX,
                displayName = "Clang++",
                path = File("/toolchain/bin/clang++"),
              )
            ),
        )

      val command =
        NativeCommandFactory(toolchain, 28).compileCpp(
          abi = AbiTarget.ARM64_V8A,
          source = source,
          output = output,
        )

      val entries = ClangdCompilationDatabase.fromCommands(listOf(command))
      assertEquals(1, entries.size)
      assertEquals(source.absolutePath, entries.single().file)
      assertTrue(entries.single().arguments.contains("-std=c++20"))

      val file =
        ClangdCompilationDatabase.write(
          outputDirectory = root,
          commands = listOf(command),
        )

      assertTrue(file.isFile)
      assertTrue(file.readText().contains("\"compile_commands\"").not())
      assertTrue(file.readText().contains("native.cpp"))
      assertTrue(file.readText().contains("aarch64-linux-android28"))
    } finally {
      root.deleteRecursively()
    }
  }
}
