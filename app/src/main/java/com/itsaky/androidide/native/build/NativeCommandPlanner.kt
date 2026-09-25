package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.NativeSourceSet
import com.itsaky.androidide.toolchain.NativeToolchain
import java.io.File
import java.nio.file.Path

data class NativeCompilePlan(
  val cCommands: List<NativeCommandSpec>,
  val cppCommands: List<NativeCommandSpec>,
)

object NativeCommandPlanner {

  fun planCompilation(
    sourceRoot: File,
    sourceSet: NativeSourceSet,
    buildDirectory: File,
    abi: AbiTarget,
    factory: NativeCommandFactory,
  ): NativeCompilePlan {
    val objectDirectory = File(buildDirectory, "obj/$abi")
    val includeDirectories = listOf(sourceRoot)

    val cCommands =
      sourceSet.cSources.map { source ->
        factory.compileC(
          abi = abi,
          source = source.toFile(),
          output = objectFile(objectDirectory, source, sourceRoot),
          includeDirectories = includeDirectories,
        )
      }

    val cppCommands =
      sourceSet.cppSources.map { source ->
        factory.compileCpp(
          abi = abi,
          source = source.toFile(),
          output = objectFile(objectDirectory, source, sourceRoot),
          includeDirectories = includeDirectories,
        )
      }

    return NativeCompilePlan(
      cCommands = cCommands,
      cppCommands = cppCommands,
    )
  }

  private fun objectFile(
    objectDirectory: File,
    source: Path,
    sourceRoot: File,
  ): File {
    val relative =
      sourceRoot.toPath()
        .relativize(source)
        .toString()

    return File(objectDirectory, "$relative.o")
  }
}
