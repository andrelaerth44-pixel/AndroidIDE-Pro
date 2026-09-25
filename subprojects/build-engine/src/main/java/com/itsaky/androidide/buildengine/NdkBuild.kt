package com.itsaky.androidide.buildengine

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Direct Android LLVM backend. No CMake and no ndk-build. */
data class NdkBuildConfiguration(
  val sourceDirs: List<String> = listOf("src/main/cpp"),
  val libraryName: String = "native-lib",
  val abis: List<String> = listOf("arm64-v8a"),
  val minSdk: Int = 26,
  val cppStandard: String = "c++17",
  val cStandard: String = "c17",
  val stl: String = "c++_shared",
  val optimization: String = "-O2",
  val compilerFlags: List<String> = emptyList(),
  val linkLibraries: List<String> = listOf("log"),
  val nativeActivity: Boolean = false,
)

data class NdkToolResult(
  val success: Boolean,
  val output: String,
  val exitCode: Int = -1,
)

class NdkToolchain(
  private val root: File,
  private val clang: File = File(root, "bin/clang"),
  private val clangxx: File = File(root, "bin/clang++"),
  private val lld: File = File(root, "bin/ld.lld"),
  private val llvmAr: File = File(root, "bin/llvm-ar"),
) {
  private val sysroot = File(root, "sysroot")
  private val resourceDir: File?
    get() =
      File(root, "lib/clang").listFiles()
        ?.filter(File::isDirectory)
        ?.maxByOrNull { it.name }

  val isAvailable: Boolean
    get() =
      clang.isFile && clangxx.isFile && lld.isFile && llvmAr.isFile &&
        sysroot.isDirectory && resourceDir?.isDirectory == true

  fun version(): NdkToolResult = runTool(listOf(clang.absolutePath, "--version"), 15)

  fun compile(
    source: File,
    output: File,
    abi: String,
    cpp: Boolean,
    configuration: NdkBuildConfiguration,
  ): NdkToolResult {
    if (!isAvailable) return unavailable()
    val target = targetTriple(abi) + configuration.minSdk
    output.parentFile?.mkdirs()
    val args = mutableListOf<String>()
    args += if (cpp) clangxx.absolutePath else clang.absolutePath
    args += "--target=" + target
    args += listOf(
      "--sysroot", sysroot.absolutePath,
      "-resource-dir", resourceDir!!.absolutePath,
      configuration.optimization,
      "-fPIC",
      "-c", source.absolutePath,
      "-o", output.absolutePath,
    )
    args += if (cpp) "-std=" + configuration.cppStandard else "-std=" + configuration.cStandard
    args += configuration.compilerFlags
    if (configuration.nativeActivity) {
      val glue = File(root, "native_app_glue")
      if (glue.isDirectory) args += listOf("-I", glue.absolutePath)
    }
    return runTool(args, 120)
  }

  fun linkShared(
    objects: List<File>,
    output: File,
    abi: String,
    configuration: NdkBuildConfiguration,
  ): NdkToolResult {
    if (!isAvailable) return unavailable()
    if (objects.isEmpty()) return NdkToolResult(false, "No native object files were produced")
    val target = targetTriple(abi) + configuration.minSdk
    output.parentFile?.mkdirs()
    val args = mutableListOf<String>()
    args += clangxx.absolutePath
    args += "--target=" + target
    args += listOf(
      "--sysroot", sysroot.absolutePath,
      "-resource-dir", resourceDir!!.absolutePath,
      "-shared",
      "-fPIC",
      "-fuse-ld=lld",
      "-Wl,-z,max-page-size=16384",
      "-Wl,-z,common-page-size=16384",
      "-Wl,-soname,lib" + configuration.libraryName + ".so",
    )
    args += "-stdlib=libc++"
    if (configuration.stl == "c++_shared") args += "-lc++_shared"
    objects.forEach { args += it.absolutePath }
    configuration.linkLibraries.forEach { args += "-l" + it }
    args += listOf("-o", output.absolutePath)
    return runTool(args, 120)
  }

  fun copySharedStl(destination: File): Boolean {
    if (!isAvailable) return false
    val source =
      root.walkTopDown().firstOrNull { it.isFile && it.name == "libc++_shared.so" }
        ?: return false
    destination.parentFile?.mkdirs()
    source.copyTo(destination, overwrite = true)
    return destination.isFile
  }

  private fun targetTriple(abi: String): String =
    when (abi) {
      "arm64-v8a" -> "aarch64-linux-android"
      "armeabi-v7a" -> "armv7a-linux-androideabi"
      "x86" -> "i686-linux-android"
      "x86_64" -> "x86_64-linux-android"
      else -> error("Unsupported Android ABI: " + abi)
    }

  private fun unavailable(): NdkToolResult =
    NdkToolResult(false, "LLVM NDK toolchain is unavailable at " + root.absolutePath)

  private fun runTool(command: List<String>, timeoutSeconds: Long): NdkToolResult {
    return try {
      val process =
        ProcessBuilder(command)
          .redirectErrorStream(true)
          .apply {
            environment()["LD_LIBRARY_PATH"] = File(root, "lib").absolutePath
          }
          .start()
      val output = process.inputStream.bufferedReader().use { it.readText() }
      if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return NdkToolResult(false, "Timed out after " + timeoutSeconds + "s\n" + output)
      }
      NdkToolResult(process.exitValue() == 0, output.trim(), process.exitValue())
    } catch (error: Throwable) {
      NdkToolResult(false, error.message ?: error.toString())
    }
  }

  companion object {
    fun fromEnvironment(environment: Map<String, String>): NdkToolchain? {
      val rootPath =
        environment["ANDROIDIDE_NDK_TOOLCHAIN"]
          ?: environment["NDK_TOOLCHAIN_ROOT"]
          ?: environment["ANDROIDIDE_HOME"]?.let { File(it, "toolchains/llvm").absolutePath }
          ?: return null
      val root = File(rootPath)
      return NdkToolchain(
        root = root,
        clang = File(root, "bin/clang"),
        clangxx = File(root, "bin/clang++"),
        lld = File(root, "bin/ld.lld"),
        llvmAr = File(root, "bin/llvm-ar"),
      )
    }
  }
}

class NdkBuildTask(
  private val moduleName: String,
  private val configuration: NdkBuildConfiguration,
  private val toolchain: NdkToolchain,
) : BuildTask {

  override val id = BuildTaskId("native." + moduleName)

  override fun inputs(context: BuildContext): Map<String, String> {
    val result = linkedMapOf<String, String>()
    result["configuration"] = configuration.toString()
    sourceFiles(context.projectDir, configuration).forEach { source ->
      result["source:" + source.relativeTo(context.projectDir).path] =
        source.length().toString() + ":" + source.lastModified()
    }
    return result
  }

  override fun execute(context: BuildContext): BuildTaskExecution {
    require(configuration.abis.isNotEmpty()) { "At least one Android ABI is required" }
    val sources = sourceFiles(context.projectDir, configuration)
    if (sources.isEmpty()) return BuildTaskExecution(message = "No C/C++ sources found")
    val version = toolchain.version()
    if (!version.success) error("NDK compiler unavailable: " + version.output)

    val buildRoot = File(context.buildDir, "ndk/" + moduleName)
    val jniRoot = File(context.projectDir, "src/main/jniLibs")
    val outputs = mutableListOf<File>()

    for (abi in configuration.abis) {
      val objectDir = File(buildRoot, abi + "/objects")
      objectDir.mkdirs()
      val objects = mutableListOf<File>()

      for (source in sources) {
        val object = File(objectDir, objectName(source))
        val compile = toolchain.compile(
          source = source,
          output = object,
          abi = abi,
          cpp = source.extension.lowercase() != "c",
          configuration = configuration,
        )
        if (!compile.success) {
          error("Native compile failed for " + source.relativeTo(context.projectDir).path + "\n" + compile.output)
        }
        objects += object
      }

      val output = File(jniRoot, abi + "/lib" + configuration.libraryName + ".so")
      val link = toolchain.linkShared(objects, output, abi, configuration)
      if (!link.success) {
        error("Native link failed for ABI " + abi + "\n" + link.output)
      }
      if (configuration.stl == "c++_shared" && !toolchain.copySharedStl(File(jniRoot, abi + "/libc++_shared.so"))) {
        error("libc++_shared.so was not found in the LLVM toolchain")
      }
      outputs += output
    }

    return BuildTaskExecution(
      message = "Built " + outputs.joinToString { it.name } +
        " with " + version.output.lineSequence().firstOrNull().orEmpty(),
      outputFingerprint = outputFingerprint(outputs),
    )
  }

  private fun sourceFiles(projectDir: File, configuration: NdkBuildConfiguration): List<File> =
    configuration.sourceDirs.asSequence()
      .map { File(projectDir, it) }
      .filter(File::isDirectory)
      .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension.lowercase() in SOURCE_EXTENSIONS }.asSequence() }
      .sortedBy(File::getAbsolutePath)
      .toList()

  private fun objectName(source: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(source.absolutePath.toByteArray())
      .take(8).joinToString("") { "%02x".format(it) }
    return source.name + "." + hash + ".o"
  }

  private fun outputFingerprint(outputs: List<File>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    outputs.sortedBy(File::getAbsolutePath).forEach { file ->
      digest.update(file.absolutePath.toByteArray())
      digest.update(0)
      digest.update(file.length().toString().toByteArray())
      digest.update(0)
      digest.update(file.lastModified().toString().toByteArray())
      digest.update(0)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private companion object {
    val SOURCE_EXTENSIONS = setOf("c", "cc", "cpp", "cxx", "c++")
  }
}

object NdkToolchainLocator {
  const val ENV_TOOLCHAIN = "ANDROIDIDE_NDK_TOOLCHAIN"
  const val ENV_LLVM_MAJOR = "ANDROIDIDE_NDK_LLVM_MAJOR"

  fun from(environment: Map<String, String>): NdkToolchain? =
    NdkToolchain.fromEnvironment(environment)
}
