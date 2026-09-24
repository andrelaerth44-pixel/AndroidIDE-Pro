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

/**
 * Native C/C++ compiler stage.
 *
 * Supported project shapes:
 *   - pure C application
 *   - pure C++ application
 *   - C + C++ application
 *   - Java/Kotlin + C/C++ hybrid application
 *
 * Hybrid applications load the generated shared library from Java/Kotlin
 * with System.loadLibrary() and expose JNI functions normally.
 *
 * Pure-native applications can declare android.app.NativeActivity. The
 * default library name is "main" for NativeActivity and "appnative" for
 * hybrid/background native code. Both can be overridden by the project
 * native configuration or the manifest's android.app.lib_name metadata.
 */
class CompileNativeTask(
  private val module: AndroidModule
) : BuildTask {

  override val id = "compileNativeDebug"

  private val output: Path
    get() = module.nativeLibDir
      .resolve("arm64-v8a")
      .resolve("lib" + module.nativeLibraryName + ".so")

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
      module.localNativeLibDir?.let(::add)
      module.localNativeIncludeDir?.let(::add)
      add(module.generatedJniHeadersDir)
      add(
        module.rootDir
          .resolve("src/main/cpp/androidide-native.properties")
      )

      module.nativeBuildConfiguration.includeDirs.forEach(::add)
      module.nativeBuildConfiguration.libraryDirs.forEach(::add)
      module.nativeBuildConfiguration.staticLibraries.forEach(::add)

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

        if (requiresNativeActivityGlue() &&
          toolchain.nativeAppGlueDir != null &&
          module.nativeActivityFunctionName == "ANativeActivity_onCreate"
        ) {
          add(
            toolchain.nativeAppGlueDir
              .resolve("android_native_app_glue.c")
          )
          add(
            toolchain.nativeAppGlueDir
              .resolve("android_native_app_glue.h")
          )
        }
      }
    }

  override val outputs: List<Path>
    get() = buildList {
      add(marker)

      if (hasSourcesOrNativeActivity()) {
        add(output)
      }
    }

  override fun execute(context: BuildContext): TaskResult = runCatching {
    module.nativeLibDir.createDirectories()

    val toolchain = module.sdk.nativeToolchain
      ?: return@runCatching TaskResult(
        false,
        "C/C++ sources require the built-in Android LLVM Core Toolchain Pack. " +
          "Install the arm64-v8a LLVM pack and retry."
      )

    val sources = sources(toolchain)

    if (sources.isEmpty()) {
      output.deleteIfExists()
      Files.writeString(
        marker,
        "abi=arm64-v8a\n" +
          "status=no-native-sources\n"
      )
      return@runCatching TaskResult(true, "No C/C++ sources")
    }

    validateToolchain(toolchain, sources)

    val objectDir = module.nativeLibDir.resolve("obj")
    objectDir.createDirectories()

    val objects = sources.map { source ->
      val object = objectDir.resolve(objectName(source))

      val compilerArgs = buildList {
        if (isCpp(source)) {
          add("--driver-mode=g++")
        }

        add("--target=aarch64-linux-android" + module.minSdk)
        add("--sysroot")
        add(toolchain.sysroot.toString())
        add("-resource-dir")
        add(toolchain.resourceDir.toString())

        module.localNativeIncludeDir?.let {
          add("-I")
          add(it.toString())
        }

        if (Files.isDirectory(module.generatedJniHeadersDir)) {
          add("-I")
          add(module.generatedJniHeadersDir.toString())
        }

        module.nativeBuildConfiguration.includeDirs.forEach {
          add("-I")
          add(it.toString())
        }

        toolchain.includeDirs.forEach {
          add("-I")
          add(it.toString())
        }

        if (requiresNativeActivityGlue() &&
          module.nativeActivityFunctionName == "ANativeActivity_onCreate"
        ) {
          toolchain.nativeAppGlueDir?.let {
            add("-I")
            add(it.toString())
          }
        }

        add("-fPIC")
        add("-fexceptions")
        add("-frtti")
        add("-fdata-sections")
        add("-ffunction-sections")
        add("-fstack-protector-strong")
        add("-DANDROID")

        if (isCpp(source)) {
          addAll(module.nativeBuildConfiguration.cppFlags)
        } else {
          addAll(module.nativeBuildConfiguration.cFlags)
        }

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

    val hasCpp = sources.any(::isCpp)
    val nativeActivity = requiresNativeActivityGlue()

    output.parent.createDirectories()

    val linkArgs = buildList {
      if (hasCpp) {
        add("--driver-mode=g++")
      }

      add("--target=aarch64-linux-android" + module.minSdk)
      add("--sysroot")
      add(toolchain.sysroot.toString())
      add("-resource-dir")
      add(toolchain.resourceDir.toString())
      add("--ld-path=" + toolchain.linker)

      module.nativeBuildConfiguration.libraryDirs.forEach {
        add("-L")
        add(it.toString())
      }

      if (hasCpp) {
        add("-stdlib=libc++")
        add("-L")
        add(toolchain.runtimeLibraryDir.toString())
      }

      // Objects come before static/shared libraries so LLD can resolve
      // symbols from the application into the libraries in one left-to-right pass.
      objects.forEach(::add)

      if (hasCpp) {
        add("-lc++_shared")
      }

      if (requiresNativeActivityGlue()) {
        add("-landroid")
        add("-llog")

        // Prevent --gc-sections from discarding the NativeActivity entry.
        add("-Wl,-u," + module.nativeActivityFunctionName)
      }

      module.nativeBuildConfiguration.staticLibraries.forEach {
        add(it.toString())
      }

      module.nativeBuildConfiguration.linkLibraries.forEach {
        add("-l" + it)
      }

      addAll(module.nativeBuildConfiguration.linkerFlags)

      add("-shared")
      add("-Wl,-z,max-page-size=16384")
      add("-Wl,-z,common-page-size=16384")
      add("-Wl,--gc-sections")
      add("-Wl,-soname,lib" + module.nativeLibraryName + ".so")
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
        appendLine("library=" + module.nativeLibraryName)
        appendLine("cStandard=" + module.cLanguageStandard)
        appendLine("cppStandard=" + module.cppLanguageStandard)
        appendLine("nativeActivity=" + nativeActivity)
        appendLine("nativeActivityFunction=" + module.nativeActivityFunctionName)
        appendLine("sources=" + sources.size)
        appendLine("output=" + output)
      }
    )

    TaskResult(
      true,
      buildString {
        append("Built ")
        append(sources.size)
        append(" C/C++ source file(s) into ")
        append(output)

        if (nativeActivity) {
          append(" with NativeActivity support")
        }
      }
    )
  }.getOrElse {
    TaskResult(
      false,
      it.message ?: "C/C++ compilation failed"
    )
  }

  private fun validateToolchain(
    toolchain: AndroidNativeToolchain,
    sources: List<Path>
  ) {
    check(Files.isRegularFile(toolchain.compiler)) {
      "Android LLVM compiler is missing: " + toolchain.compiler
    }

    check(Files.isRegularFile(toolchain.cppCompiler)) {
      "Android LLVM C++ compiler is missing: " + toolchain.cppCompiler
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

    if (requiresNativeActivityGlue() &&
      module.nativeActivityFunctionName == "ANativeActivity_onCreate"
    ) {
      val glue = toolchain.nativeAppGlueDir
        ?: error(
          "This NativeActivity project requires android_native_app_glue " +
            "from the Core LLVM Toolchain Pack."
        )

      check(
        Files.isRegularFile(
          glue.resolve("android_native_app_glue.c")
        )
      ) {
        "android_native_app_glue.c is missing from the Core LLVM Toolchain Pack."
      }

      check(
        Files.isRegularFile(
          glue.resolve("android_native_app_glue.h")
        )
      ) {
        "android_native_app_glue.h is missing from the Core LLVM Toolchain Pack."
      }

      check(
        sources.none {
          it.fileName.toString() == "android_native_app_glue.c"
        }
      ) {
        "Do not add a second android_native_app_glue.c to a NativeActivity project."
      }
    }
  }

  private fun sources(
    toolchain: AndroidNativeToolchain
  ): List<Path> {
    val result = buildList {
      if (Files.exists(module.nativeSourceDir)) {
        module.nativeSourceDir.walk()
          .filter { it.isRegularFile() }
          .filter {
            it.extension in setOf(
              "c",
              "cc",
              "cpp",
              "cxx",
              "cppm",
              "ixx"
            )
          }
          .sortedBy { it.toString() }
          .forEach(::add)
      }

      if (requiresNativeActivityGlue() &&
        module.nativeActivityFunctionName == "ANativeActivity_onCreate"
      ) {
        val glue = toolchain.nativeAppGlueDir
          ?.resolve("android_native_app_glue.c")

        if (glue != null &&
          Files.isRegularFile(glue) &&
          result.none {
            it.fileName.toString() == "android_native_app_glue.c"
          }
        ) {
          add(glue)
        }
      }
    }

    return result.distinct()
  }

  private fun hasSourcesOrNativeActivity(): Boolean =
    hasNativeSourceFiles() || requiresNativeActivityGlue()

  private fun hasNativeSourceFiles(): Boolean =
    Files.exists(module.nativeSourceDir) &&
      module.nativeSourceDir.walk().any {
        it.isRegularFile() &&
          it.extension in setOf(
            "c",
            "cc",
            "cpp",
            "cxx",
            "cppm",
            "ixx"
          )
      }

  private fun requiresNativeActivityGlue(): Boolean =
    module.manifestContainsNativeActivity()

  private fun compilerFor(
    toolchain: AndroidNativeToolchain,
    cpp: Boolean
  ): Path =
    if (cpp) toolchain.cppCompiler else toolchain.compiler

  private fun isCpp(source: Path): Boolean =
    source.extension in setOf(
      "cc",
      "cpp",
      "cxx",
      "cppm",
      "ixx"
    )

  private fun objectName(source: Path): String {
    val pathKey = runCatching {
      if (source.startsWith(module.nativeSourceDir)) {
        module.nativeSourceDir.relativize(source).toString()
      } else {
        source.toString()
      }
    }.getOrDefault(source.toString())

    val digest = MessageDigest.getInstance("SHA-256")
      .digest(pathKey.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
      .take(24)

    return digest + ".o"
  }
}
