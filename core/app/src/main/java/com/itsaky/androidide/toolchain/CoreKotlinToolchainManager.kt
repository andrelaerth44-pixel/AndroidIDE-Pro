package com.itsaky.androidide.toolchain

import android.content.Context
import java.nio.file.Files
import java.nio.file.Path

class CoreKotlinToolchainManager(
  private val context: Context
) {

  fun resolveClassLoader(): ClassLoader? {
    val packageContext = runCatching {
      context.createPackageContext(
        KOTLIN_PACKAGE,
        Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
      )
    }.getOrNull() ?: return null

    return packageContext.classLoader
  }

  fun isInstalled(): Boolean =
    resolveClassLoader() != null

  fun resolveComposeCompilerPlugin(): Path? {
    val packageContext = runCatching {
      context.createPackageContext(
        KOTLIN_PACKAGE,
        Context.CONTEXT_IGNORE_SECURITY
      )
    }.getOrNull() ?: return null

    val root = context.filesDir.toPath()
      .resolve("toolchains")
      .resolve("kotlin")
      .resolve(COMPOSE_COMPILER_VERSION)

    val target = root.resolve("compose-compiler-hosted.jar")
    if (Files.isRegularFile(target)) return target

    return runCatching {
      root.toFile().mkdirs()
      packageContext.assets.open("compose-compiler-hosted.jar").use { input ->
        Files.newOutputStream(target).use { output ->
          input.copyTo(output)
        }
      }
      target.takeIf { Files.isRegularFile(it) }
    }.getOrNull()
  }

  companion object {
    const val KOTLIN_PACKAGE = "com.itsaky.androidide.toolchain.kotlin"
    const val COMPOSE_COMPILER_VERSION = "1.5.14"
  }
}
