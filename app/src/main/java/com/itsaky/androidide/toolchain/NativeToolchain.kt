package com.itsaky.androidide.toolchain

import java.io.File

enum class NativeToolId {
  CLANG,
  CLANGXX,
  CLANGD,
  LLD,
  LLDB,
  LLVM_AR,
  LIBCXX,
}

data class NativeTool(
  val id: NativeToolId,
  val displayName: String,
  val path: File?,
)

data class NativeToolchain(
  val root: File?,
  val sysroot: File?,
  val tools: List<NativeTool>,
) {
  fun tool(id: NativeToolId): NativeTool? =
    tools.firstOrNull { it.id == id }
}
