package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.toolchain.NativeTool
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeCommandFactoryTest {

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
  fun compileCUsesAndroidTargetAndC17() {
    val command =
      NativeCommandFactory(toolchain, androidApiLevel = 28)
        .compileC(
          abi = AbiTarget.ARM64_V8A,
          source = File("/project/native.c"),
          output = File("/project/native.o"),
        )

    assertEquals("/toolchain/bin/clang", command.executable.absolutePath)
    assertEquals(
      listOf(
        "--target=aarch64-linux-android28",
        "--sysroot=/toolchain/sysroot",
        "-std=c17",
        "-fPIC",
        "-c",
        "/project/native.c",
        "-o",
        "/project/native.o",
      ),
      command.arguments,
    )
  }

  @Test
  fun archiveObjectsUsesLlvmAr() {
    val factory =
      NativeCommandFactory(
        toolchain.copy(
          tools =
            toolchain.tools +
              NativeTool(
                id = NativeToolId.LLVM_AR,
                displayName = "LLVM ar",
                path = File("/toolchain/bin/llvm-ar"),
              )
        ),
        androidApiLevel = 28,
      )

    val command =
      factory.archiveObjects(
        objects = listOf(File("/build/a.o"), File("/build/b.o")),
        output = File("/build/libnative.a"),
      )

    assertEquals("/toolchain/bin/llvm-ar", command.executable.absolutePath)
    assertEquals(
      listOf("rcs", "/build/libnative.a", "/build/a.o", "/build/b.o"),
      command.arguments,
    )
  }

  @Test
  fun linkSharedUsesAndroidTargetAndLld() {
    val command =
      NativeCommandFactory(toolchain, androidApiLevel = 28)
        .linkShared(
          abi = AbiTarget.ARM64_V8A,
          objects = listOf(File("/build/native.o")),
          output = File("/build/libnative.so"),
        )

    assertEquals("/toolchain/bin/clang++", command.executable.absolutePath)
    assertTrue(command.arguments.contains("--target=aarch64-linux-android28"))
    assertTrue(command.arguments.contains("-stdlib=libc++"))
    assertTrue(command.arguments.contains("-fuse-ld=lld"))
    assertTrue(command.arguments.contains("/build/native.o"))
  }

  @Test
  fun compileCppUsesCxx20() {
    val command =
      NativeCommandFactory(toolchain, androidApiLevel = 28)
        .compileCpp(
          abi = AbiTarget.ARM64_V8A,
          source = File("/project/native.cpp"),
          output = File("/project/native.o"),
        )

    assertEquals("/toolchain/bin/clang++", command.executable.absolutePath)
    assertTrue(command.arguments.contains("-std=c++20"))
  }
}
