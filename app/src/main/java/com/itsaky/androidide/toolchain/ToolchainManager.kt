package com.itsaky.androidide.toolchain

import android.os.Build
import com.itsaky.androidide.utils.Environment
import java.io.File

enum class ToolchainComponentStatus {
  READY,
  MISSING,
  PLANNED,
}

data class ToolchainComponent(
  val id: String,
  val name: String,
  val description: String,
  val status: ToolchainComponentStatus,
  val path: String? = null,
)

data class ToolchainSnapshot(
  val abi: String,
  val components: List<ToolchainComponent>,
)

object ToolchainManager {

  fun inspect(): ToolchainSnapshot {
    val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
    val javaHome = safeFile { Environment.JAVA_HOME }
    val androidHome = safeFile { Environment.ANDROID_HOME }
    val aapt2 = safeFile { Environment.AAPT2 }
    val androidJar = safeFile { Environment.ANDROID_JAR }
    val toolchainRoot =
      safeFile { Environment.ANDROIDIDE_HOME }
        ?.let { File(it, "toolchains") }

    val llvmRoot = toolchainRoot?.let { File(it, "llvm") }
    val clang = llvmRoot?.let { File(it, "bin/clang") }
    val clangd = llvmRoot?.let { File(it, "bin/clangd") }
    val lldb = llvmRoot?.let { File(it, "bin/lldb") }

    return ToolchainSnapshot(
      abi = abi,
      components =
        listOf(
          component(
            id = "jdk",
            name = "JDK",
            description = "Java runtime and compiler environment",
            file = javaHome,
          ),
          component(
            id = "android-sdk",
            name = "Android SDK",
            description = "SDK root used by the current AndroidIDE installation",
            file = androidHome,
          ),
          component(
            id = "aapt2",
            name = "AAPT2",
            description = "Android resource compiler",
            file = aapt2,
          ),
          component(
            id = "android-jar",
            name = "Android API",
            description = "Boot classpath Android API jar used by language/build tooling",
            file = androidJar,
          ),
          nativeComponent(
            id = "llvm",
            name = "LLVM / Clang",
            description = "Planned native C/C++ compiler toolchain",
            file = clang,
            root = llvmRoot,
          ),
          nativeComponent(
            id = "clangd",
            name = "Clangd",
            description = "Planned C/C++ language server",
            file = clangd,
            root = llvmRoot,
          ),
          nativeComponent(
            id = "lldb",
            name = "LLDB",
            description = "Planned native debugger",
            file = lldb,
            root = llvmRoot,
          ),
        ),
    )
  }

  private fun component(
    id: String,
    name: String,
    description: String,
    file: File?,
  ): ToolchainComponent =
    ToolchainComponent(
      id = id,
      name = name,
      description = description,
      status = if (file?.exists() == true) ToolchainComponentStatus.READY else ToolchainComponentStatus.MISSING,
      path = file?.absolutePath,
    )

  private fun nativeComponent(
    id: String,
    name: String,
    description: String,
    file: File?,
    root: File?,
  ): ToolchainComponent =
    ToolchainComponent(
      id = id,
      name = name,
      description = description,
      status = if (file?.exists() == true) ToolchainComponentStatus.READY else ToolchainComponentStatus.PLANNED,
      path = if (file?.exists() == true) file.absolutePath else root?.absolutePath,
    )

  private fun safeFile(block: () -> File?): File? =
    runCatching { block() }.getOrNull()
}
