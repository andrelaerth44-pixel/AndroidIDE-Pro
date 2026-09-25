package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.NativeBuildRequest
import com.itsaky.androidide.native.model.NativeLibraryType

object NativePipeline {

  fun createGraph(request: NativeBuildRequest): NativeBuildGraph {
    val target =
      requireNotNull(
        request.module.targets.firstOrNull {
          it.abi == request.abi && it.variant == request.variant
        }
      ) {
        "Requested native target is not present in the module"
      }

    val prefix =
      request.module.moduleName + ":" + request.abi + ":" + request.variant

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

    val tasks = mutableListOf(
      scan,
      headers,
      compileC,
      compileCpp,
    )

    val finalTask =
      if (target.libraryType == NativeLibraryType.SHARED) {
        task(
          prefix = prefix,
          kind = NativeBuildTask.Kind.LINK_NATIVE,
          dependencies = listOf(compileC.id, compileCpp.id),
          description = "Link shared native library",
        )
      } else {
        task(
          prefix = prefix,
          kind = NativeBuildTask.Kind.ARCHIVE_OBJECTS,
          dependencies = listOf(compileC.id, compileCpp.id),
          description = "Archive static native library",
        )
      }

    tasks += finalTask

    val packageLibraries =
      task(
        prefix = prefix,
        kind = NativeBuildTask.Kind.PACKAGE_NATIVE_LIBS,
        dependencies = listOf(finalTask.id),
      )

    tasks += packageLibraries

    return NativeBuildGraph(tasks)
  }

  private fun task(
    prefix: String,
    kind: NativeBuildTask.Kind,
    dependencies: List<String> = emptyList(),
    description: String = kind.displayName,
  ): NativeBuildTask =
    NativeBuildTask(
      id = prefix + ":" + kind.name.lowercase(),
      kind = kind,
      dependencies = dependencies,
      description = description,
    )
}
