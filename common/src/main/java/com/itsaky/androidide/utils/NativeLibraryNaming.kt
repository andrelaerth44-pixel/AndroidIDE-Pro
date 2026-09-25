package com.itsaky.androidide.utils

import java.util.Locale

/**
 * Canonical native shared-library name without the leading "lib" or ".so".
 *
 * Android's System.loadLibrary(), APK lib paths and the native linker all need
 * a stable identifier that is safe even when the project directory contains
 * spaces or punctuation.
 */
object NativeLibraryNaming {

  fun sanitize(value: String): String {
    val normalized =
      value.trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9_]+"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')

    val safe = normalized.ifBlank { "native" }
    return if (safe.first().isDigit()) "native_$safe" else safe
  }
}
