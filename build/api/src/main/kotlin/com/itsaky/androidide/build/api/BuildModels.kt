/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

import java.nio.file.Path

enum class BuildModuleType {
  UNKNOWN,
  ANDROID_APPLICATION,
  ANDROID_LIBRARY,
  JAVA_LIBRARY,
  KOTLIN_LIBRARY,
  NATIVE
}

data class BuildProject(
  val rootDir: Path,
  val modules: List<BuildModule>,
)

data class BuildModule(
  val id: String,
  val projectDir: Path,
  val type: BuildModuleType,
)

data class BuildRequest(
  val requestedTasks: Set<TaskId> = emptySet(),
  val variant: String? = null,
  val clean: Boolean = false,
  val offline: Boolean = false,
  val parameters: Map<String, String> = emptyMap(),
)

data class BuildContext(
  val projectDir: Path,
  val cacheDir: Path,
  val toolchainsDir: Path,
  val environment: Map<String, String> = emptyMap(),
  val cancellation: CancellationToken = CancellationToken.NONE,
  val diagnostics: BuildDiagnosticSink = BuildDiagnosticSink.NONE,
)

data class TaskContext(
  val projectDir: Path,
  val cacheDir: Path,
  val toolchainsDir: Path,
  val environment: Map<String, String>,
  val cancellation: CancellationToken,
  val diagnostics: BuildDiagnosticSink,
)

typealias TaskId = String

data class Artifact(
  val path: Path,
)

fun interface CancellationToken {
  fun isCancellationRequested(): Boolean

  companion object {
    val NONE: CancellationToken = CancellationToken { false }
  }
}

fun interface BuildDiagnosticSink {
  fun report(diagnostic: BuildDiagnostic)

  companion object {
    val NONE: BuildDiagnosticSink = BuildDiagnosticSink {}
  }
}

data class BuildDiagnostic(
  val severity: Severity,
  val kind: Kind,
  val message: String,
  val source: Path? = null,
  val line: Int? = null,
  val column: Int? = null,
  val code: String? = null,
  val task: TaskId? = null,
  val detail: String? = null,
) {
  enum class Severity {
    INFO,
    WARNING,
    ERROR,
  }

  enum class Kind {
    GENERAL,
    COMPILER,
    RESOURCE,
    MANIFEST,
    LINKER,
    PACKAGING,
    DEPENDENCY,
    CONFIGURATION,
    INTERNAL,
  }
}
