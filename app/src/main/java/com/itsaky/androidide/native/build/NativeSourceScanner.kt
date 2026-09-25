package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.NativeSourceSet
import java.io.File

object NativeSourceScanner {

  private val cExtensions = setOf("c", "h")
  private val cppExtensions = setOf("cc", "cpp", "cxx", "hh", "hpp")

  fun scan(sourceRoot: File): NativeSourceSet {
    require(sourceRoot.exists() && sourceRoot.isDirectory) {
      "Native source root does not exist: " + sourceRoot.absolutePath
    }

    val cSources = mutableListOf<java.nio.file.Path>()
    val cppSources = mutableListOf<java.nio.file.Path>()
    val headers = mutableListOf<java.nio.file.Path>()

    sourceRoot.walkTopDown()
      .filter { it.isFile }
      .forEach { file ->
        when (file.extension.lowercase()) {
          in cExtensions -> {
            if (file.extension.lowercase() == "h") {
              headers += file.toPath()
            } else {
              cSources += file.toPath()
            }
          }

          in cppExtensions -> {
            if (file.extension.lowercase() in setOf("hh", "hpp")) {
              headers += file.toPath()
            } else {
              cppSources += file.toPath()
            }
          }
        }
      }

    return NativeSourceSet(
      cSources = cSources.sortedBy { it.toString() },
      cppSources = cppSources.sortedBy { it.toString() },
      headers = headers.sortedBy { it.toString() },
    )
  }
}
