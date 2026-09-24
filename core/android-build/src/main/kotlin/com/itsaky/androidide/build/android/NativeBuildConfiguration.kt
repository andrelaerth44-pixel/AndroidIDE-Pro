package com.itsaky.androidide.build.android

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists

data class NativeBuildConfiguration(
  val libraryName: String? = null,
  val includeDirs: List<Path> = emptyList(),
  val libraryDirs: List<Path> = emptyList(),
  val linkLibraries: List<String> = emptyList(),
  val staticLibraries: List<Path> = emptyList(),
  val cFlags: List<String> = emptyList(),
  val cppFlags: List<String> = emptyList(),
  val linkerFlags: List<String> = emptyList()
) {

  companion object {
    private const val FILE_NAME = "androidide-native.properties"

    fun load(moduleRoot: Path): NativeBuildConfiguration {
      val file = moduleRoot
        .resolve("src/main/cpp")
        .resolve(FILE_NAME)

      if (!file.exists()) {
        return NativeBuildConfiguration()
      }

      val properties = Properties()
      Files.newInputStream(file).use(properties::load)

      val nativeRoot = file.parent

      fun csv(key: String): List<String> =
        properties.getProperty(key)
          ?.split(',')
          ?.map(String::trim)
          ?.filter(String::isNotEmpty)
          ?: emptyList()

      fun flags(key: String): List<String> =
        properties.getProperty(key)
          ?.trim()
          ?.takeIf(String::isNotEmpty)
          ?.split(Regex("\s+"))
          ?: emptyList()

      fun relativePaths(key: String): List<Path> =
        csv(key).map { value ->
          val candidate = Path.of(value)
          if (candidate.isAbsolute) {
            candidate
          } else {
            nativeRoot.resolve(value).normalize()
          }
        }

      return NativeBuildConfiguration(
        libraryName = properties.getProperty("libraryName")
          ?.trim()
          ?.takeIf(String::isNotEmpty),
        includeDirs = relativePaths("includeDirs"),
        libraryDirs = relativePaths("libraryDirs"),
        linkLibraries = csv("linkLibraries"),
        staticLibraries = relativePaths("staticLibraries"),
        cFlags = flags("cFlags"),
        cppFlags = flags("cppFlags"),
        linkerFlags = flags("linkerFlags")
      )
    }
  }
}
