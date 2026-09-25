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
    )

  private fun compile(
    toolId: NativeToolId,
    abi: AbiTarget,
    source: File,
    output: File,
    languageStandard: String,
    includeDirectories: List<File>,
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
