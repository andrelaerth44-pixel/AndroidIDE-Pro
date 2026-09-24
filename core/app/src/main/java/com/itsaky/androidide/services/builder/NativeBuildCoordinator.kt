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
import com.itsaky.androidide.build.api.BuildDiagnosticSink
import com.itsaky.androidide.build.api.BuildModule
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.BuildResult
import com.itsaky.androidide.build.api.BuildSystem
import com.itsaky.androidide.build.local.AndroidApplicationInputsProvider
import com.itsaky.androidide.build.local.LightweightBuildSystem
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.projects.internal.ProjectManagerImpl
import com.itsaky.androidide.tooling.api.messages.result.BuildCancellationRequestResult
import com.itsaky.androidide.tooling.api.messages.result.BuildInfo
import com.itsaky.androidide.tooling.api.messages.result.TaskExecutionResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

internal class NativeBuildCoordinator(
  private val isBuildInProgress: () -> Boolean,
  private val setBuildInProgress: (Boolean) -> Unit,
  private val eventListener: () -> GradleBuildService.EventListener?,
) {

  private val nativeRunning = AtomicBoolean(false)
  private val cancellationRequested = AtomicBoolean(false)

  fun tryExecute(tasks: Array<out String>): CompletableFuture<TaskExecutionResult>? {
    val requested = tasks.singleOrNull() ?: return null
    val shortTask = requested.substringAfterLast(':')
    if (!shortTask.startsWith("assemble")) return null

    val manager = ProjectManagerImpl.getInstance()
    val workspace = manager.getWorkspace() ?: return null
    val module = WorkspaceNativeAndroidAdapter.application(workspace) ?: return null
    val toolchain = NativeAndroidToolchainFactory.create(module) ?: return null
    val project = WorkspaceNativeAndroidAdapter.buildProject(workspace, module)

    val variant = shortTask.removePrefix("assemble")
      .ifBlank { module.getSelectedVariant()?.name ?: "debug" }
      .replaceFirstChar(Char::lowercaseChar)
    if (module.getVariant(variant) == null) return null

    val request = BuildRequest(
      requestedTasks = setOf(shortTask),
      variant = variant,
      parameters = mapOf("android.variant" to variant),
    )
    val cacheDir = workspace.getProjectDir().toPath().resolve(".androidide/build-cache")
    val toolchainsDir = workspace.getProjectDir().toPath().resolve(".androidide/toolchains")
    val context = BuildContext(
      projectDir = workspace.getProjectDir().toPath(),
      cacheDir = cacheDir,
      toolchainsDir = toolchainsDir,
      diagnostics = BuildDiagnosticSink { diagnostic ->
        eventListener()?.onOutput("[${diagnostic.severity}] ${diagnostic.message}")
      },
      cancellation = com.itsaky.androidide.build.api.CancellationToken { cancellationRequested.get() },
    )

    val provider = WorkspaceNativeAndroidAdapter.inputsProvider(module)
    val nativeBackend = LightweightBuildSystem(
      androidToolchain = toolchain,
      androidInputsProvider = provider,
    )
    if (!nativeBackend.supports(project, request, context)) return null

    if (isBuildInProgress()) {
      return failedFuture(BuildInProgressException())
    }

    setBuildInProgress(true)
    nativeRunning.set(true)
    cancellationRequested.set(false)
    eventListener()?.prepareBuild(BuildInfo(listOf(requested)))

    val future = CompletableFuture.supplyAsync {
      val graph = nativeBackend.createBuildGraph(project, request, context)
      val result = com.itsaky.androidide.build.api.SequentialBuildExecutor().execute(graph, context)
      when (result.state) {
        BuildResult.State.SUCCESS -> {
          eventListener()?.onBuildSuccessful(listOf(requested))
          TaskExecutionResult.SUCCESS
        }
        BuildResult.State.CANCELLED -> {
          eventListener()?.onBuildFailed(listOf(requested))
          TaskExecutionResult(false, TaskExecutionResult.Failure.BUILD_CANCELLED)
        }
        BuildResult.State.FAILED -> {
          eventListener()?.onBuildFailed(listOf(requested))
          TaskExecutionResult(false, TaskExecutionResult.Failure.BUILD_FAILED)
        }
      }
    }

    future.whenComplete { _, _ ->
      nativeRunning.set(false)
      cancellationRequested.set(false)
      setBuildInProgress(false)
    }
    return future
  }

  fun cancel(): BuildCancellationRequestResult {
    if (!nativeRunning.get()) {
      return BuildCancellationRequestResult(
        wasEnqueued = false,
        failureReason = BuildCancellationRequestResult.Reason.NO_RUNNING_BUILD,
      )
    }
    cancellationRequested.set(true)
    return BuildCancellationRequestResult(wasEnqueued = true)
  }

  private fun <T> failedFuture(error: Throwable): CompletableFuture<T> =
    CompletableFuture<T>().also { it.completeExceptionally(error) }
}
