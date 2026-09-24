package com.itsaky.androidide.build.android

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.walk

class CompileNativeTask(
  private val module: AndroidModule
) : BuildTask {

  override val id = "compileNativeDebug"

  private val output: Path
    get() = module.nativeLibDir.resolve("arm64-v8a/libappnative.so")

  private val marker: Path
    get() = module.nativeLibDir.resolve(".native-stamp")

  private val environment: Map<String, String>
    get() {
      val toolchain = module.sdk.nativeToolchain
        ?: return emptyMap()
      return mapOf(
        "LD_LIBRARY_PATH" to toolchain.runtimeLibraryDir.toString()
      )
    }

  override val inputs: List<Path>
    get() = buildList {
      add(module.nativeSourceDir)
      module.sdk.nativeToolchain?.let { toolchain ->
        add(toolchain.compiler)
        add(toolchain.cppCompiler)
        add(toolchain.linker)
        add(toolchain.sysroot)
        add(toolchain.resourceDir)
        add(toolchain.runtimeLibraryDir)
        toolchain.runtimeSharedLibrary?.let(::add)
        toolchain.includeDirs.forEach(::add)
        toolchain.nativeAppGlueDir?.let(::add)
      }
    }

  override val outputs: List<Path>
    get() = buildList {
      add(marker)
      if (hasSources()) add(output)
    }

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.nativeLibDir.createDirectories()

    val sources = sources()
    if (sources.isEmpty()) {
      output.deleteIfExists()
      Files.writeString(
        marker,
        "abi=arm64-v8a\nstatus=no-native-sources\n"
      )
      return@runCatching TaskResult(true, "No C/C++ sources")
    }

    val toolchain = module.sdk.nativeToolchain
      ?: return@runCatching TaskResult(
        false,
        "C/C++ sources require the built-in Android LLVM Core Toolchain Pack. " +
          "Install the arm64-v8a LLVM pack and retry."
      )

    check(Files.isRegularFile(toolchain.compiler)) {
      "Android LLVM compiler is missing: " + toolchain.compiler
    }
    check(Files.isRegularFile(toolchain.linker)) {
      "Android LLVM linker is missing: " + toolchain.linker
    }
    check(Files.isDirectory(toolchain.sysroot)) {
      "Android LLVM sysroot is missing: " + toolchain.sysroot
    }
    check(Files.isDirectory(toolchain.resourceDir)) {
      "Android LLVM resource directory is missing: " + toolchain.resourceDir
    }

    val objectDir = module.nativeLibDir.resolve("obj")
    objectDir.createDirectories()

    val objects = sources.map { source ->
      val object = objectDir.resolve(objectName(source))
      val compilerArgs = buildList {
        if (isCpp(source)) add("--driver-mode=g++")
        add("--target=aarch64-linux-android" + module.minSdk)
        add("--sysroot")
        add(toolchain.sysroot.toString())
        add("-resource-dir")
        add(toolchain.resourceDir.toString())
        add("-I")
        add(module.nativeSourceDir.toString())
        toolchain.includeDirs.forEach {
          add("-I")
          add(it.toString())
        }
        toolchain.nativeAppGlueDir?.let {
          add("-I")
          add(it.toString())
        }
        add("-fPIC")
        add("-O2")
        add("-fdata-sections")
        add("-ffunction-sections")
        add("-fstack-protector-strong")
        add("-DANDROID")
        add(
          "-std=" + if (source.extension == "c") {
            module.cLanguageStandard
          } else {
            module.cppLanguageStandard
          }
        )
        add("-c")
        add(source.toString())
        add("-o")
        add(object.toString())
      }

      ProcessTools.run(
        executable = compilerFor(toolchain, isCpp(source)),
        args = compilerArgs,
        environment = environment,
        logger = context::log
      )

      object
    }

    output.parent.createDirectories()

    val hasCpp = sources.any(::isCpp)
    val linkArgs = buildList {
      if (hasCpp) add("--driver-mode=g++")
      add("--target=aarch64-linux-android" + module.minSdk)
      add("--sysroot")
      add(toolchain.sysroot.toString())
      add("-resource-dir")
      add(toolchain.resourceDir.toString())
      add("--ld-path=" + toolchain.linker)
      if (hasCpp) {
        add("-stdlib=libc++")
        add("-L")
        add(toolchain.runtimeLibraryDir.toString())
        add("-lc++_shared")
      }
      add("-shared")
      add("-Wl,-z,max-page-size=16384")
      add("-Wl,-z,common-page-size=16384")
      add("-Wl,--gc-sections")
      add("-Wl,-soname,libappnative.so")
      objects.forEach { add(it.toString()) }
      add("-o")
      add(output.toString())
    }

    ProcessTools.run(
      executable = compilerFor(toolchain, hasCpp),
      args = linkArgs,
      environment = environment,
      logger = context::log
    )

    if (hasCpp) {
      val runtime = toolchain.runtimeSharedLibrary
        ?: return@runCatching TaskResult(
          false,
          "C++ compilation requires libc++_shared.so in the Core LLVM Toolchain Pack."
        )
      check(Files.isRegularFile(runtime)) {
        "C++ runtime library is missing: " + runtime
      }
    }

    Files.writeString(
      marker,
      buildString {
        appendLine("abi=arm64-v8a")
        appendLine("toolchain=" + toolchain.version)
        appendLine("cStandard=" + module.cLanguageStandard)
        appendLine("cppStandard=" + module.cppLanguageStandard)
        appendLine("sources=" + sources.size)
        appendLine("output=" + output)
      }
    )

    TaskResult(true, "Built " + sources.size + " C/C++ source file(s)")
  }.getOrElse {
    TaskResult(false, it.message ?: "C/C++ compilation failed")
  }

  private fun sources(): List<Path> =
    if (Files.exists(module.nativeSourceDir)) {
      module.nativeSourceDir.walk()
        .filter { it.isRegularFile() }
        .filter { it.extension in setOf("c", "cc", "cpp", "cxx") }
        .sortedBy { it.toString() }
        .toList()
    } else {
      emptyList()
    }

  private fun hasSources(): Boolean = sources().isNotEmpty()

  private fun isCpp(source: Path): Boolean =
    source.extension in setOf("cc", "cpp", "cxx")

  private fun objectName(source: Path): String {
    val relative = module.nativeSourceDir.relativize(source).toString()
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(relative.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
      .take(24)
    return digest + ".o"
  }
}
