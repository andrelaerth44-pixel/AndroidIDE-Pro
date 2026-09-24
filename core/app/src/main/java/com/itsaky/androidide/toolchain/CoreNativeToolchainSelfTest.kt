package com.itsaky.androidide.toolchain

import com.itsaky.androidide.build.android.ProcessTools
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

data class NativeToolchainSelfTestResult(
  val success: Boolean,
  val message: String
)

class CoreNativeToolchainSelfTest {

  fun run(toolchain: com.itsaky.androidide.build.android.AndroidNativeToolchain): NativeToolchainSelfTestResult {
    val root = Files.createTempDirectory("androidide-llvm-self-test-" + UUID.randomUUID())
    return try {
      val cSource = root.resolve("hello.c")
      val cppSource = root.resolve("hello.cpp")
      val cObject = root.resolve("hello-c.o")
      val cppObject = root.resolve("hello-cpp.o")
      val output = root.resolve("libandroidide_selftest.so")

      Files.writeString(
        cSource,
        "int androidide_c_selftest(void) { return 40; }\n"
      )
      Files.writeString(
        cppSource,
        "extern \"C\" int androidide_cpp_selftest(void) { return 2; }\n"
      )

      val env = mapOf(
        "LD_LIBRARY_PATH" to toolchain.runtimeLibraryDir.toString()
      )

      compile(
        toolchain = toolchain,
        source = cSource,
        output = cObject,
        standard = "c17",
        cpp = false,
        environment = env
      )

      compile(
        toolchain = toolchain,
        source = cppSource,
        output = cppObject,
        standard = "c++20",
        cpp = true,
        environment = env
      )

      ProcessTools.run(
        executable = toolchain.compiler,
        args = listOf(
          "--driver-mode=g++",
          "--target=aarch64-linux-android26",
          "--sysroot", toolchain.sysroot.toString(),
          "-resource-dir", toolchain.resourceDir.toString(),
          "--ld-path=" + toolchain.linker,
          "-stdlib=libc++",
          "-L", toolchain.runtimeLibraryDir.toString(),
          "-lc++_shared",
          "-shared",
          "-Wl,-z,max-page-size=16384",
          "-Wl,-z,common-page-size=16384",
          "-Wl,--gc-sections",
          cObject.toString(),
          cppObject.toString(),
          "-o", output.toString()
        ),
        environment = env
      )

      check(Files.isRegularFile(output)) {
        "LLVM self-test did not produce a shared library"
      }

      NativeToolchainSelfTestResult(
        success = true,
        message = "LLVM C17/C++20 compile + LLD link self-test passed."
      )
    } catch (t: Throwable) {
      NativeToolchainSelfTestResult(
        success = false,
        message = t.message ?: "LLVM self-test failed"
      )
    } finally {
      root.toFile().deleteRecursively()
    }
  }

  private fun compile(
    toolchain: com.itsaky.androidide.build.android.AndroidNativeToolchain,
    source: Path,
    output: Path,
    standard: String,
    cpp: Boolean,
    environment: Map<String, String>
  ) {
    ProcessTools.run(
      executable = toolchain.compiler,
      args = buildList {
        if (cpp) add("--driver-mode=g++")
        add("--target=aarch64-linux-android26")
        add("--sysroot")
        add(toolchain.sysroot.toString())
        add("-resource-dir")
        add(toolchain.resourceDir.toString())
        add("-fPIC")
        add("-O2")
        add("-std=" + standard)
        add("-c")
        add(source.toString())
        add("-o")
        add(output.toString())
      },
      environment = environment
    )
  }
}
