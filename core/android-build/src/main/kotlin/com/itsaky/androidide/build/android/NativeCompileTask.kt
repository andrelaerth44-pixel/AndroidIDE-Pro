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
 * It deliberately owns only the language/toolchain concerns:
 * source discovery, compilation, JNI/native-activity glue and linking.
 *
 * Java/Kotlin integration is achieved by the generated shared library. A
 * hybrid app simply loads libappnative through System.loadLibrary() and JNI.
 *
 * A pure native app can declare android.app.NativeActivity in its manifest.
 * In that case the official android_native_app_glue source is compiled into
 * the same shared library automatically.
 */
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
      module.localNativeLibDir?.let(::add)
      module.localNativeIncludeDir?.let(::add)

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
        if (requiresNativeActivityGlue(module) &&
          toolchain.nativeAppGlueDir != null
        ) {
          add(toolchain.nativeAppGlueDir.resolve("android_native_app_glue.c"))
          add(toolchain.nativeAppGlueDir.resolve("android_native_app_glue.h"))
        }
      }
    }

  override val outputs: List<Path>
    get() = buildList {
      add(marker)
      if (hasSources()) {
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
        "abi=arm64-v8a
status=no-native-sources
"
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

        toolchain.includeDirs.forEach {
          add("-I")
          add(it.toString())
        }

        toolchain.nativeAppGlueDir?.let { glue ->
          if (requiresNativeActivityGlue(module)) {
            add("-I")
            add(glue.toString())
          }
        }

        add("-fPIC")
        add("-fexceptions")
        add("-frtti")
        add("-O0")
        add("-g")
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
    val nativeActivity = requiresNativeActivityGlue(module)

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

      add("-shared")
      add("-Wl,-z,max-page-size=16384")
      add("-Wl,-z,common-page-size=16384")
      add("-Wl,--gc-sections")
      add("-Wl,-soname,libappnative.so")

      if (hasCpp) {
        add("-stdlib=libc++")
        add("-L")
        add(toolchain.runtimeLibraryDir.toString())
        add("-lc++_shared")
      }

      if (nativeActivity) {
        // NativeActivity + android_native_app_glue requires the Android
        // platform library at link time.
        add("-landroid")
        add("-llog")
      }

      objects.forEach(::add)
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
        appendLine("nativeActivity=" + nativeActivity)
        appendLine("sources=" + sources.size)
        appendLine("output=" + output)
      }
    )

    TaskResult(
      true,
      buildString {
        append("Built ")
        append(sources.size)
        append(" C/C++ source file(s)")

        if (nativeActivity) {
          append(" with NativeActivity glue")
        }

        append(" into ")
        append(output)
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

    if (requiresNativeActivityGlue(module)) {
      val glue = toolchain.nativeAppGlueDir
        ?: error(
          "This NativeActivity project requires android_native_app_glue " +
            "from the Core LLVM Toolchain Pack."
        )

      check(Files.isRegularFile(glue.resolve("android_native_app_glue.c"))) {
        "android_native_app_glue.c is missing from the Core LLVM Toolchain Pack."
      }

      check(Files.isRegularFile(glue.resolve("android_native_app_glue.h"))) {
        "android_native_app_glue.h is missing from the Core LLVM Toolchain Pack."
      }

      check(sources.none { it.fileName.toString() == "android_native_app_glue.c" } ) {
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
              "cxx"
            )
          }
          .sortedBy { it.toString() }
          .forEach(::add)
      }

      if (requiresNativeActivityGlue(module)) {
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

  private fun hasSources(): Boolean =
    Files.exists(module.nativeSourceDir) &&
      module.nativeSourceDir.walk().any {
        it.isRegularFile() &&
          it.extension in setOf(
            "c",
            "cc",
            "cpp",
            "cxx"
          )
      }

  private fun requiresNativeActivityGlue(
    module: AndroidModule
  ): Boolean =
    runCatching {
      Files.readString(module.manifest)
        .contains("android.app.NativeActivity")
    }.getOrDefault(false)

  private fun compilerFor(
    toolchain: AndroidNativeToolchain,
    cpp: Boolean
  ): Path =
    if (cpp) toolchain.cppCompiler else toolchain.compiler

  private fun isCpp(source: Path): Boolean =
    source.extension in setOf(
      "cc",
      "cpp",
      "cxx"
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
