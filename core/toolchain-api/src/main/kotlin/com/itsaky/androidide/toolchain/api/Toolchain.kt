package com.itsaky.androidide.toolchain.api

import java.nio.file.Path

@JvmInline
value class ToolchainId(val value: String)

enum class ToolchainKind {
  JAVA,
  KOTLIN,
  LLVM,
  RUST,
  GO,
  JAVASCRIPT
}

data class ToolchainManifest(
  val id: ToolchainId,
  val kind: ToolchainKind,
  val version: String,
  val hostAbi: String,
  val entryPoint: Path?,
  val files: List<Path>,
  val sha256: String
)

data class ToolchainRequest(
  val kind: ToolchainKind,
  val hostAbi: String,
  val version: String
)

interface ToolchainStore {
  fun find(request: ToolchainRequest): ToolchainManifest?

  fun install(
    request: ToolchainRequest,
    archive: Path
  ): ToolchainManifest

  fun remove(id: ToolchainId): Boolean
}

class BuiltInToolchainPaths(
  private val root: Path
) {
  fun root(): Path = root

  fun forKind(kind: ToolchainKind): Path =
    root.resolve(kind.name.lowercase())

  fun forRequest(request: ToolchainRequest): Path =
    forKind(request.kind)
      .resolve(request.hostAbi)
      .resolve(request.version)
}
