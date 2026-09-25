package com.itsaky.androidide.toolchain

import com.itsaky.androidide.utils.Environment
import java.io.File

object NativeToolchainLocator {

  fun locate(): NativeToolchain {
    val expectedRoot =
      runCatching {
        Environment.ANDROIDIDE_HOME?.let { File(it, "toolchains/llvm") }
      }.getOrNull()

    val prefixBin =
      runCatching {
        Environment.PREFIX?.let { File(it, "bin") }
      }.getOrNull()

    val roots =
      listOfNotNull(
        expectedRoot?.takeIf { it.exists() },
        prefixBin,
      )

    val tools =
      listOf(
        nativeTool(NativeToolId.CLANG, "Clang", "clang", roots),
        nativeTool(NativeToolId.CLANGXX, "Clang++", "clang++", roots),
        nativeTool(NativeToolId.CLANGD, "Clangd", "clangd", roots),
        nativeTool(NativeToolId.LLD, "LLD", "ld.lld", roots),
        nativeTool(NativeToolId.LLDB, "LLDB", "lldb", roots),
        nativeTool(NativeToolId.LLVM_AR, "LLVM ar", "llvm-ar", roots),
      )

    val sysroot =
      listOfNotNull(
        expectedRoot?.let { File(it, "sysroot") },
        expectedRoot?.let { File(it, "lib/clang") },
      ).firstOrNull { it.exists() }

    val libcxx =
      listOfNotNull(
        sysroot?.let { File(it, "usr/include/c++/v1") },
        expectedRoot?.let { File(it, "include/c++/v1") },
      ).firstOrNull { it.exists() }

    return NativeToolchain(
      root = expectedRoot?.takeIf { it.exists() },
      sysroot = sysroot,
      tools = tools + NativeTool(
        id = NativeToolId.LIBCXX,
        displayName = "libc++",
        path = libcxx,
      ),
    )
  }

  private fun nativeTool(
    id: NativeToolId,
    displayName: String,
    executable: String,
    roots: List<File>,
  ): NativeTool =
    NativeTool(
      id = id,
      displayName = displayName,
      path = findExecutable(executable, roots),
    )

  private fun findExecutable(
    executable: String,
    roots: List<File>,
  ): File? =
    roots
      .asSequence()
      .map { root ->
        if (root.name == "bin") File(root, executable)
        else File(root, "bin/$executable")
      }
      .firstOrNull { it.isFile && it.canExecute() }
}
