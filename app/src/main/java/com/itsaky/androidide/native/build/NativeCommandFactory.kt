package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File

class NativeCommandFactory(
  private val toolchain: NativeToolchain,
  private val androidApiLevel: Int,
) {

  fun compileC(
    abi: AbiTarget,
    source: File,
    output: File,
    includeDirectories: List<File> = emptyList(),
  ): NativeCommandSpec =
    compile(
      toolId = NativeToolId.CLANG,
      abi = abi,
      source = source,
      output = output,
      languageStandard = "c17",
      includeDirectories = includeDirectories,
      extraArguments = emptyList(),
    )

  fun compileCpp(
    abi: AbiTarget,
    source: File,
    output: File,
    includeDirectories: List<File> = emptyList(),
  ): NativeCommandSpec =
    compile(
      toolId = NativeToolId.CLANGXX,
      abi = abi,
      source = source,
      output = output,
      languageStandard = "c++20",
      includeDirectories = includeDirectories,
      extraArguments = listOf("-stdlib=libc++"),
    )

  fun archiveObjects(
    objects: List<File>,
    output: File,
  ): NativeCommandSpec {
    val archiver =
      requireNotNull(toolchain.tool(NativeToolId.LLVM_AR)?.path) {
        "Native tool 'LLVM_AR' is not available"
      }

    return NativeCommandSpec(
      executable = archiver,
      arguments =
        buildList {
          add("rcs")
          add(output.absolutePath)
          objects.forEach { add(it.absolutePath) }
        },
      workingDirectory = output.parentFile,
    )
  }

  fun linkShared(
    abi: AbiTarget,
    objects: List<File>,
    output: File,
  ): NativeCommandSpec {
    val linker =
      requireNotNull(toolchain.tool(NativeToolId.CLANGXX)?.path) {
        "Native tool 'CLANGXX' is not available"
      }

    require(androidApiLevel >= 21) {
      "Android API level must be >= 21"
    }

    return NativeCommandSpec(
      executable = linker,
      arguments =
        buildList {
          add("--target=" + abi.androidTriplePrefix + androidApiLevel)
          toolchain.sysroot?.let {
            add("--sysroot=" + it.absolutePath)
          }
          add("-stdlib=libc++")
          if (toolchain.libcxxShared?.isFile == true) {
            add("-lc++_shared")
          }
          add("-shared")
          add("-fuse-ld=lld")
          objects.forEach { add(it.absolutePath) }
          add("-o")
          add(output.absolutePath)
        },
      workingDirectory = output.parentFile,
    )
  }

  private fun compile(
    toolId: NativeToolId,
    abi: AbiTarget,
    source: File,
    output: File,
    languageStandard: String,
    includeDirectories: List<File>,
    extraArguments: List<String>,
  ): NativeCommandSpec {
    val compiler =
      requireNotNull(toolchain.tool(toolId)?.path) {
        "Native tool '" + toolId.name + "' is not available"
      }

    require(androidApiLevel >= 21) {
      "Android API level must be >= 21"
    }

    val arguments = buildList {
      add("--target=" + abi.androidTriplePrefix + androidApiLevel)
      toolchain.sysroot?.let {
        add("--sysroot=" + it.absolutePath)
      }
      add("-std=" + languageStandard)
      add("-fPIC")
      extraArguments.forEach(::add)
      includeDirectories.forEach {
        add("-I" + it.absolutePath)
      }
      add("-c")
      add(source.absolutePath)
      add("-o")
      add(output.absolutePath)
    }

    return NativeCommandSpec(
      executable = compiler,
      arguments = arguments,
      workingDirectory = source.parentFile,
    )
  }
}
