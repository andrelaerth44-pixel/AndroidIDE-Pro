/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

fun interface BuildExecutor {
  fun execute(
    graph: BuildGraph,
    context: BuildContext,
  ): BuildResult
}

data class BuildResult(
  val state: State,
  val taskResults: List<TaskResult> = emptyList(),
  val diagnostics: List<BuildDiagnostic> = emptyList(),
  val message: String? = null,
) {
  enum class State {
    SUCCESS,
    FAILED,
    CANCELLED,
  }
}
