/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.services.builder

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.BuildModule
import com.itsaky.androidide.build.api.BuildModuleType
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.build.api.CancellationToken
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult
import com.itsaky.androidide.build.gradle.GradleTaskExecutor
import com.itsaky.androidide.tooling.api.messages.InitializeProjectParams
import com.itsaky.androidide.tooling.api.messages.result.BuildCancellationRequestResult
import com.itsaky.androidide.tooling.api.messages.result.InitializeResult
import com.itsaky.androidide.tooling.api.messages.result.TaskExecutionResult
import com.itsaky.androidide.tooling.api.models.ToolingServerMetadata
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleBuildServiceTaskExecutorTest {

  private val context = TaskContext(
    projectDir = Paths.get("/project"),
    cacheDir = Paths.get("/cache"),
    toolchainsDir = Paths.get("/toolchains"),
    environment = emptyMap(),
    cancellation = CancellationToken.NONE,
    diagnostics = com.itsaky.androidide.build.api.BuildDiagnosticSink.NONE,
  )

  @Test
  fun successfulBuildServiceTaskBecomesSuccessfulTaskResult() {
    val service = mockk<BuildService>()
    every { service.executeTasks(":app:assembleDebug") } returns CompletableFuture.completedFuture(
      TaskExecutionResult.SUCCESS
    )

    val result = GradleBuildServiceTaskExecutor(service).execute(
      ":app:assembleDebug",
      context,
    )

    assertEquals(TaskResult.State.SUCCESS, result.state)
    assertTrue(result.durationMillis >= 0)
  }

  @Test
  fun failedBuildServiceTaskBecomesDiagnostic() {
    val service = mockk<BuildService>()
    every { service.executeTasks("assemble") } returns CompletableFuture.completedFuture(
      TaskExecutionResult(false, TaskExecutionResult.Failure.BUILD_FAILED)
    )

    val result = GradleBuildServiceTaskExecutor(service).execute("assemble", context)

    assertEquals(TaskResult.State.FAILED, result.state)
    assertEquals(
      com.itsaky.androidide.build.api.BuildDiagnostic.Kind.GENERAL,
      result.diagnostics.single().kind,
    )
  }

  @Test
  fun cancellationIsRequestedBeforeTaskStarts() {
    val service = mockk<BuildService>()
    val cancelledContext = context.copy(
      cancellation = CancellationToken { true },
    )

    val result = GradleBuildServiceTaskExecutor(service).execute("assemble", cancelledContext)

    assertEquals(TaskResult.State.CANCELLED, result.state)
    every { service.executeTasks(any()) } returns CompletableFuture.completedFuture(TaskExecutionResult.SUCCESS)
  }
}
