package com.itsaky.androidide.native.model

import com.google.gson.GsonBuilder
import java.io.File

data class NativeProjectConfig(
  val libraryType: NativeLibraryType = NativeLibraryType.SHARED,
)

object NativeProjectConfigStore {

  private const val FILE_NAME = "native.json"

  private val gson =
    GsonBuilder()
      .setPrettyPrinting()
      .create()

  fun load(moduleRoot: File): NativeProjectConfig? {
    val file = File(moduleRoot, ".androidide/$FILE_NAME")
    if (!file.isFile) {
      return null
    }

    return runCatching {
      gson.fromJson(file.readText(), NativeProjectConfig::class.java)
    }.getOrNull()
  }

  fun writeDefault(
    moduleRoot: File,
    libraryType: NativeLibraryType = NativeLibraryType.SHARED,
  ): File {
    val directory = File(moduleRoot, ".androidide")
    directory.mkdirs()

    val file = File(directory, FILE_NAME)
    file.writeText(
      gson.toJson(
        NativeProjectConfig(libraryType = libraryType)
      )
    )
    return file
  }
}
