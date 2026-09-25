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
    val nativeToolchain = NativeToolchainLocator.locate()

    val components = mutableListOf(
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
    )

    nativeToolchain.tools.forEach { tool ->
      components +=
        nativeComponent(
          id = tool.id.name.lowercase(),
          name = tool.displayName,
          description = nativeDescription(tool.id),
          file = tool.path,
          root = nativeToolchain.root,
        )
    }

    return ToolchainSnapshot(
      abi = abi,
      components = components,
    )
  }

  private fun nativeDescription(id: NativeToolId): String =
    when (id) {
      NativeToolId.CLANG -> "C compiler used by the AndroidIDE Pro native pipeline"
      NativeToolId.CLANGXX -> "C++ compiler used by the AndroidIDE Pro native pipeline"
      NativeToolId.CLANGD -> "C/C++ language server for editor diagnostics and completion"
      NativeToolId.LLD -> "LLVM linker used for native library linking"
      NativeToolId.LLDB -> "Native debugger planned for the debugging phase"
      NativeToolId.LIBCXX -> "C++ standard library headers for native compilation"
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
      status =
        if (file?.exists() == true) {
          ToolchainComponentStatus.READY
        } else {
          ToolchainComponentStatus.MISSING
        },
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
      status =
        when {
          file?.exists() == true -> ToolchainComponentStatus.READY
          root?.exists() == true -> ToolchainComponentStatus.MISSING
          else -> ToolchainComponentStatus.PLANNED
        },
      path =
        when {
          file?.exists() == true -> file.absolutePath
          root?.exists() == true -> root.absolutePath
          else -> null
        },
    )

  private fun safeFile(block: () -> File?): File? =
    runCatching { block() }.getOrNull()
}
