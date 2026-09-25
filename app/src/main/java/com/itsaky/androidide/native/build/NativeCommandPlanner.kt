package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.NativeSourceSet
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
  ): NativeCompilePlan =
    NativeCompilePlan(
      cCommands = planC(sourceRoot, sourceSet, buildDirectory, abi, factory),
      cppCommands = planCpp(sourceRoot, sourceSet, buildDirectory, abi, factory),
    )

  fun planC(
    sourceRoot: File,
    sourceSet: NativeSourceSet,
    buildDirectory: File,
    abi: AbiTarget,
    factory: NativeCommandFactory,
  ): List<NativeCommandSpec> {
    val objectDirectory = File(buildDirectory, "obj/$abi")
    val includeDirectories =
      listOf(
        sourceRoot,
        File(buildDirectory, "jni/headers"),
      )

    return sourceSet.cSources.map { source ->
      factory.compileC(
        abi = abi,
        source = source.toFile(),
        output = objectFile(objectDirectory, source, sourceRoot),
        includeDirectories = includeDirectories,
      )
    }
  }

  fun planCpp(
    sourceRoot: File,
    sourceSet: NativeSourceSet,
    buildDirectory: File,
    abi: AbiTarget,
    factory: NativeCommandFactory,
  ): List<NativeCommandSpec> {
    val objectDirectory = File(buildDirectory, "obj/$abi")
    val includeDirectories =
      listOf(
        sourceRoot,
        File(buildDirectory, "jni/headers"),
      )

    return sourceSet.cppSources.map { source ->
      factory.compileCpp(
        abi = abi,
        source = source.toFile(),
        output = objectFile(objectDirectory, source, sourceRoot),
        includeDirectories = includeDirectories,
      )
    }
  }

  fun objectFiles(
    sourceRoot: File,
    sourceSet: NativeSourceSet,
    buildDirectory: File,
    abi: AbiTarget,
  ): List<File> =
    (sourceSet.cSources + sourceSet.cppSources)
      .map { objectFile(File(buildDirectory, "obj/$abi"), it, sourceRoot) }

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
