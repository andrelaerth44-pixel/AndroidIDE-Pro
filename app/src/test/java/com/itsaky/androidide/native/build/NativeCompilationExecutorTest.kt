package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.NativeSourceSet
import com.itsaky.androidide.toolchain.NativeTool
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeCompilationExecutorTest {

  private val toolchain =
    NativeToolchain(
      root = File("/toolchain"),
      sysroot = File("/toolchain/sysroot"),
      tools =
        listOf(
          NativeTool(
            id = NativeToolId.CLANG,
            displayName = "Clang",
            path = File("/toolchain/bin/clang"),
          ),
          NativeTool(
            id = NativeToolId.CLANGXX,
            displayName = "Clang++",
            path = File("/toolchain/bin/clang++"),
          ),
        ),
    )

  @Test
  fun compilationRunsCThenCpp() {
    val root = Files.createTempDirectory("native-exec").toFile()
    try {
      val c = File(root, "native.c").apply { writeText("int c() { return 0; }") }
      val cpp = File(root, "native.cpp").apply { writeText("int cpp() { return 0; }") }

      val commands = mutableListOf<NativeCommandSpec>()
      val executor =
        NativeCompilationExecutor(
          toolchain = toolchain,
          androidApiLevel = 28,
          commandExecutor = { command, _ ->
            commands += command
            NativeProcessResult(0, "", 1)
          },
        )

      val result =
        executor.compile(
          sourceRoot = root,
          sourceSet =
            NativeSourceSet(
              cSources = listOf(c.toPath()),
              cppSources = listOf(cpp.toPath()),
            ),
          buildDirectory = File(root, "build"),
          abi = AbiTarget.ARM64_V8A,
        )

      assertTrue(result.success)
      assertEquals(2, result.completedCommands)
      assertEquals("/toolchain/bin/clang", commands[0].executable.absolutePath)
      assertEquals("/toolchain/bin/clang++", commands[1].executable.absolutePath)
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun compilationStopsOnFirstFailure() {
    val root = Files.createTempDirectory("native-fail").toFile()
    try {
      val c = File(root, "native.c").apply { writeText("int c() { return 0; }") }
      val cpp = File(root, "native.cpp").apply { writeText("int cpp() { return 0; }") }

      var calls = 0
      val executor =
        NativeCompilationExecutor(
          toolchain = toolchain,
          androidApiLevel = 28,
          commandExecutor = { _, _ ->
            calls++
            NativeProcessResult(if (calls == 1) 1 else 0, "compiler error", 1)
          },
        )

      val result =
        executor.compile(
          sourceRoot = root,
          sourceSet =
            NativeSourceSet(
              cSources = listOf(c.toPath()),
              cppSources = listOf(cpp.toPath()),
            ),
          buildDirectory = File(root, "build"),
          abi = AbiTarget.ARM64_V8A,
        )

      assertFalse(result.success)
      assertEquals(0, result.completedCommands)
      assertEquals(1, calls)
      assertTrue(result.processResult?.output?.contains("compiler error") == true)
    } finally {
      root.deleteRecursively()
    }
  }
}
