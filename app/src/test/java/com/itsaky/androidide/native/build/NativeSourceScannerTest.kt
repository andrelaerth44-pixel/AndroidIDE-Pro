package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.toolchain.NativeTool
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSourceScannerTest {

  @Test
  fun scannerFindsAndSortsNativeSources() {
    val root = Files.createTempDirectory("native-scan").toFile()
    try {
      File(root, "z.cpp").writeText("int z() { return 0; }")
      File(root, "a.c").writeText("int a() { return 0; }")
      File(root, "include/native.h").apply {
        parentFile.mkdirs()
        writeText("#pragma once")
      }
      File(root, "src/native.hpp").apply {
        parentFile.mkdirs()
        writeText("#pragma once")
      }

      val sourceSet = NativeSourceScanner.scan(root)

      assertEquals(listOf(File(root, "a.c").toPath()), sourceSet.cSources)
      assertEquals(listOf(File(root, "z.cpp").toPath()), sourceSet.cppSources)
      assertEquals(
        listOf(
          File(root, "include/native.h").toPath(),
          File(root, "src/native.hpp").toPath(),
        ),
        sourceSet.headers,
      )
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun plannerPreservesRelativeSourcePathInObjectOutput() {
    val root = Files.createTempDirectory("native-plan").toFile()
    try {
      val source = File(root, "src/native.cpp").apply {
        parentFile.mkdirs()
        writeText("int main() { return 0; }")
      }

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

      val sourceSet =
        com.itsaky.androidide.native.model.NativeSourceSet(
          cppSources = listOf(source.toPath()),
        )

      val plan =
        NativeCommandPlanner.planCompilation(
          sourceRoot = root,
          sourceSet = sourceSet,
          buildDirectory = File("/build"),
          abi = AbiTarget.ARM64_V8A,
          factory = NativeCommandFactory(toolchain, 28),
        )

      assertEquals(1, plan.cppCommands.size)
      assertTrue(
        plan.cppCommands.single().arguments.contains(
          File("/build/obj/ARM64_V8A/src/native.cpp.o").absolutePath
        )
      )
    } finally {
      root.deleteRecursively()
    }
  }
}
