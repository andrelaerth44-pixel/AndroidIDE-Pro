package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.NativeBuildRequest
import com.itsaky.androidide.native.model.NativeLibraryType

object NativePipeline {

  fun createGraph(request: NativeBuildRequest): NativeBuildGraph {
    val prefix =
      "${request.module.moduleName}:${request.abi}:${request.variant}"

    val scan =
      task(
        prefix = prefix,
        kind = NativeBuildTask.Kind.SCAN_SOURCES,
      )

    val headers =
      task(
        prefix = prefix,
        kind = NativeBuildTask.Kind.GENERATE_JNI_HEADERS,
        dependencies = listOf(scan.id),
      )

    val compileC =
      task(
        prefix = prefix,
        kind = NativeBuildTask.Kind.COMPILE_C,
        dependencies = listOf(scan.id),
      )

    val compileCpp =
      task(
        prefix = prefix,
        kind = NativeBuildTask.Kind.COMPILE_CPP,
        dependencies = listOf(scan.id, headers.id),
      )

    val archive =
      task(
        prefix = prefix,
        kind = NativeBuildTask.Kind.ARCHIVE_OBJECTS,
        dependencies = listOf(compileC.id, compileCpp.id),
      )

    val link =
      task(
        prefix = prefix,
        kind = NativeBuildTask.Kind.LINK_NATIVE,
        dependencies = listOf(archive.id),
        description =
          if (request.module.targets.any { it.libraryType == NativeLibraryType.STATIC }) {
            "Link native output"
          } else {
            "Link shared native library"
          },
      )

    val packageLibraries =
      task(
        prefix = prefix,
        kind = NativeBuildTask.Kind.PACKAGE_NATIVE_LIBS,
        dependencies = listOf(link.id),
      )

    return NativeBuildGraph(
      listOf(
        scan,
        headers,
        compileC,
        compileCpp,
        archive,
        link,
        packageLibraries,
      )
    )
  }

  private fun task(
    prefix: String,
    kind: NativeBuildTask.Kind,
    dependencies: List<String> = emptyList(),
    description: String = kind.displayName,
  ): NativeBuildTask =
    NativeBuildTask(
      id = "$prefix:${kind.name.lowercase()}",
      kind = kind,
      dependencies = dependencies,
      description = description,
    )
}
