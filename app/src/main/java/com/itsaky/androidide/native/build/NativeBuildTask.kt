package com.itsaky.androidide.native.build

/**
 * A single deterministic stage in the native build graph.
 *
 * Tasks are data-only at this layer. Toolchain execution is intentionally kept
 * out of the graph so the same graph can later be executed by Clang, tests,
 * or a future local build executor.
 */
data class NativeBuildTask(
  val id: String,
  val kind: Kind,
  val dependencies: List<String> = emptyList(),
  val description: String = kind.displayName,
) {

  enum class Kind(val displayName: String) {
    SCAN_SOURCES("Scan native sources"),
    GENERATE_JNI_HEADERS("Generate JNI headers"),
    COMPILE_C("Compile C"),
    COMPILE_CPP("Compile C++"),
    ARCHIVE_OBJECTS("Archive objects"),
    LINK_NATIVE("Link native library"),
    PACKAGE_NATIVE_LIBS("Package native libraries"),
  }
}
