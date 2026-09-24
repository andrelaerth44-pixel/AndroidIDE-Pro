package com.itsaky.androidide.build.android

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.extension
import kotlin.io.path.outputStream
import kotlin.io.path.walk

private data class AndroidLlvmToolchain(
  val root: Path,
  val clang: Path,
  val clangxx: Path,
  val sysroot: Path
)

private object AndroidLlvmToolchainLocator {

  fun find(module: AndroidModule): AndroidLlvmToolchain? {
    val root = module.sdk.nativeToolchainRoot ?: return null
    val abiRoot = root.resolve("arm64-v8a")
    val bin = abiRoot.resolve("bin")
    val clang = bin.resolve("clang")
    val clangxx = bin.resolve("clang++")
    val sysroot = abiRoot.resolve("sysroot")

    if (!Files.isExecutable(clang) ||
      !Files.isExecutable(clangxx) ||
      !Files.isDirectory(sysroot)
    ) {
      return null
    }

    return AndroidLlvmToolchain(
      root = abiRoot,
      clang = clang,
      clangxx = clangxx,
      sysroot = sysroot
    )
  }
}

class CompileNativeTask(
  private val module: AndroidModule
) : BuildTask {

  override val id = "compileNativeDebug"

  private val marker: Path
    get() = module.nativeLibDir.resolve(".native-stamp")

  override val inputs: List<Path>
    get() = listOf(module.nativeSourceDir)

  override val outputs: List<Path>
    get() = listOf(marker)

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.nativeLibDir.deleteRecursively()
    module.nativeLibDir.createDirectories()

    val sources = if (Files.exists(module.nativeSourceDir)) {
      module.nativeSourceDir.walk()
        .filter { it.isRegularFile() }
        .filter { it.extension in setOf("c", "cc", "cpp", "cxx") }
        .sortedBy { it.toString() }
        .toList()
    } else {
      emptyList()
    }

    if (sources.isEmpty()) {
      Files.writeString(marker, "no native sources\n")
      return@runCatching TaskResult(true, "No C/C++ sources")
    }

    val toolchain = AndroidLlvmToolchainLocator.find(module)
      ?: return@runCatching TaskResult(
        false,
        "C/C++ sources are built by the built-in LLVM pipeline, but the " +
          "Android-hosted LLVM toolchain is not installed. Expected: " +
          (module.sdk.nativeToolchainRoot?.resolve("arm64-v8a") ?: "<toolchains/llvm>/arm64-v8a")
      )

    val objectDir = module.nativeLibDir.resolve("obj")
    objectDir.createDirectories()

    val objects = sources.mapIndexed { index, source ->
      val object = objectDir.resolve("obj$index.o")
      val compiler = if (source.extension == "c") {
        toolchain.clang
      } else {
        toolchain.clangxx
      }

      ProcessTools.run(
        compiler,
        listOf(
          "--target=aarch64-linux-android" + module.minSdk,
          "-B", toolchain.root.resolve("bin").toString(),
          "--sysroot", toolchain.sysroot.toString(),
          "-fPIC",
          "-O2",
          "-c",
          source.toString(),
          "-o", object.toString()
        ),
        logger = context::log
      )

      object
    }

    val output = module.nativeLibDir.resolve("arm64-v8a/libappnative.so")
    output.parent.createDirectories()

    ProcessTools.run(
      toolchain.clangxx,
      listOf(
        "--target=aarch64-linux-android" + module.minSdk,
        "-B", toolchain.root.resolve("bin").toString(),
        "--sysroot", toolchain.sysroot.toString(),
        "-fuse-ld=lld",
        "-shared",
        "-Wl,-soname,libappnative.so"
      ) + objects.map(Path::toString) + listOf(
        "-o", output.toString()
      ),
      logger = context::log
    )

    Files.writeString(
      marker,
      "abi=arm64-v8a\noutput=" + output + "\n"
    )

    TaskResult(true)
  }.getOrElse {
    TaskResult(false, it.message ?: "C/C++ compilation failed")
  }
}
