package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.BuildVariant
import com.itsaky.androidide.native.model.NativeLibraryType
import com.itsaky.androidide.native.model.NativeModule
import com.itsaky.androidide.native.model.NativeSourceSet
import com.itsaky.androidide.native.model.NativeTarget
import com.itsaky.androidide.toolchain.NativeTool
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeBuildExecutorTest {

  private val toolchain =
    NativeToolchain(
      root = File("/toolchain"),
      sysroot = File("/toolchain/sysroot"),
      tools =
        listOf(
          NativeTool(
            id = NativeToolId.CLANG,
            displayName = "Clang",
            path = File("/toolchain/bin/clang"),
          ),
          NativeTool(
            id = NativeToolId.CLANGXX,
            displayName = "Clang++",
            path = File("/toolchain/bin/clang++"),
          ),
        ),
    )

  @Test
  fun sharedBuildExecutesGraphAndProducesOutput() {
    val moduleRoot = Files.createTempDirectory("native-build").toFile()
    try {
      File(moduleRoot, "src/main/c/native.c").apply {
        parentFile.mkdirs()
        writeText("int native_c() { return 0; }")
      }
      File(moduleRoot, "src/main/cpp/native.cpp").apply {
        parentFile.mkdirs()
        writeText("int native_cpp() { return 0; }")
      }

      val module =
        NativeModule(
          moduleName = "app",
          targets =
            listOf(
              NativeTarget(
                name = "app",
                abi = AbiTarget.ARM64_V8A,
                variant = BuildVariant.DEBUG,
                libraryType = NativeLibraryType.SHARED,
                sourceSet =
                  NativeSourceSet(
                    cSources = listOf(File(moduleRoot, "src/main/c/native.c").toPath()),
                    cppSources = listOf(File(moduleRoot, "src/main/cpp/native.cpp").toPath()),
                  ),
              )
            ),
        )

      val states = mutableListOf<NativeBuildTaskState>()
      val executor =
        NativeBuildExecutor(
          toolchain = toolchain,
          androidApiLevel = 28,
          commandExecutor = { command, _, _ ->
            val outputIndex = command.arguments.indexOf("-o")
            if (outputIndex >= 0) {
              File(command.arguments[outputIndex + 1]).apply {
                parentFile?.mkdirs()
                writeText("fake native output")
              }
            }
            NativeProcessResult(0, "ok", 1)
          },
        )

      val result =
        executor.execute(
          request =
            NativeBuildRequest(
              module = module,
              abi = AbiTarget.ARM64_V8A,
              variant = BuildVariant.DEBUG,
            ),
          moduleRoot = moduleRoot,
          onTaskState = { _, state -> states += state },
        )

      assertTrue(result.success)
      assertEquals(
        listOf(
          "app:ARM64_V8A:DEBUG:scan_sources",
          "app:ARM64_V8A:DEBUG:generate_jni_headers",
          "app:ARM64_V8A:DEBUG:compile_c",
          "app:ARM64_V8A:DEBUG:compile_cpp",
          "app:ARM64_V8A:DEBUG:link_native",
          "app:ARM64_V8A:DEBUG:package_native_libs",
        ),
        result.executedTasks,
      )
      assertNotNull(result.outputFile)
      assertTrue(result.outputFile!!.isFile)
      assertEquals("arm64-v8a", result.outputFile!!.parentFile?.name)
      assertTrue(moduleRoot.resolve("compile_commands.json").isFile)
      assertTrue(moduleRoot.resolve("compile_commands.json").readText().contains("native.cpp"))
      assertTrue(states.contains(NativeBuildTaskState.FAILED).not())
    } finally {
      moduleRoot.deleteRecursively()
    }
  }
}
