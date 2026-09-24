/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

interface BuildTask {

  val id: TaskId

  val displayName: String
    get() = id

  val dependencies: Set<TaskId>
    get() = emptySet()

  val inputs: Set<Artifact>
    get() = emptySet()

  val outputs: Set<Artifact>
    get() = emptySet()

  fun execute(context: TaskContext): TaskResult
}

data class TaskResult(
  val taskId: TaskId,
  val state: State,
  val outputs: Set<Artifact> = emptySet(),
  val diagnostics: List<BuildDiagnostic> = emptyList(),
  val message: String? = null,
  val durationMillis: Long = 0L,
) {
  enum class State {
    SUCCESS,
    SKIPPED,
    FAILED,
    CANCELLED,
  }
}

data class TaskDescriptor(
  val id: TaskId,
  val displayName: String = id,
)
