package com.itsaky.androidide.toolchain

import android.content.Context

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

  companion object {
    const val KOTLIN_PACKAGE = "com.itsaky.androidide.toolchain.kotlin"
  }
}
