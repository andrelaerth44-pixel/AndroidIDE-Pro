package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.BuildVariant
import com.itsaky.androidide.native.model.NativeLibraryType
import com.itsaky.androidide.native.model.NativeModule
import com.itsaky.androidide.native.model.NativeSourceSet
import com.itsaky.androidide.native.model.NativeTarget
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeBuildGraphTest {

  @Test
  fun graphOrdersDependenciesBeforeDependents() {
    val scan =
      NativeBuildTask(
        id = "scan",
        kind = NativeBuildTask.Kind.SCAN_SOURCES,
      )
    val compile =
      NativeBuildTask(
        id = "compile",
        kind = NativeBuildTask.Kind.COMPILE_CPP,
        dependencies = listOf(scan.id),
      )
    val link =
      NativeBuildTask(
        id = "link",
        kind = NativeBuildTask.Kind.LINK_NATIVE,
        dependencies = listOf(compile.id),
      )

    val order = NativeBuildGraph(listOf(link, compile, scan))
      .topologicalOrder()
      .map { it.id }

    assertEquals(listOf("scan", "compile", "link"), order)
  }

  @Test
  fun graphRejectsDependencyCycles() {
    val first =
      NativeBuildTask(
        id = "first",
        kind = NativeBuildTask.Kind.SCAN_SOURCES,
        dependencies = listOf("second"),
      )
    val second =
      NativeBuildTask(
        id = "second",
        kind = NativeBuildTask.Kind.COMPILE_CPP,
        dependencies = listOf("first"),
      )

    assertThrows(IllegalStateException::class.java) {
      NativeBuildGraph(listOf(first, second))
    }
  }


  @Test
  fun staticPipelineAddsArchiveStage() {
    val module =
      NativeModule(
        moduleName = "static",
        targets =
          listOf(
            NativeTarget(
              name = "static",
              abi = AbiTarget.ARM64_V8A,
              variant = BuildVariant.RELEASE,
              libraryType = NativeLibraryType.STATIC,
              sourceSet = NativeSourceSet(),
            )
          ),
      )

    val graph =
      NativePipeline.createGraph(
        NativeBuildRequest(
          module = module,
          abi = AbiTarget.ARM64_V8A,
          variant = BuildVariant.RELEASE,
        )
      )

    val kinds = graph.topologicalOrder().map { it.kind }
    assertTrue(kinds.contains(NativeBuildTask.Kind.ARCHIVE_OBJECTS))
    assertEquals(NativeBuildTask.Kind.LINK_NATIVE, kinds[kinds.lastIndex - 1])
  }

  @Test
  fun pipelineCreatesDependencyAwareNativeStages() {
    val module =
      NativeModule(
        moduleName = "app",
        targets =
          listOf(
            NativeTarget(
              name = "app",
              abi = AbiTarget.ARM64_V8A,
              variant = BuildVariant.DEBUG,
              libraryType = NativeLibraryType.SHARED,
              sourceSet =
                NativeSourceSet(
                  cSources = listOf(Path.of("src/main/c/native.c")),
                  cppSources = listOf(Path.of("src/main/cpp/native.cpp")),
                ),
            )
          ),
      )

    val graph =
      NativePipeline.createGraph(
        NativeBuildRequest(
          module = module,
          abi = AbiTarget.ARM64_V8A,
          variant = BuildVariant.DEBUG,
        )
      )

    val order = graph.topologicalOrder().map { it.kind }

    assertEquals(
      listOf(
        NativeBuildTask.Kind.SCAN_SOURCES,
        NativeBuildTask.Kind.GENERATE_JNI_HEADERS,
        NativeBuildTask.Kind.COMPILE_C,
        NativeBuildTask.Kind.COMPILE_CPP,
        NativeBuildTask.Kind.LINK_NATIVE,
        NativeBuildTask.Kind.PACKAGE_NATIVE_LIBS,
      ),
      order,
    )

    val ready = graph.readyTasks(emptySet()).map { it.kind }
    assertEquals(listOf(NativeBuildTask.Kind.SCAN_SOURCES), ready)
  }
}
