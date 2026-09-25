package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.NativeBuildRequest
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

interface NativeBuildService {

  val isBuildInProgress: Boolean

  fun execute(
    request: NativeBuildRequest,
    moduleRoot: File,
    buildDirectory: File? = null,
    onTaskState: (NativeBuildTask, NativeBuildTaskState) -> Unit = { _, _ -> },
    onOutput: (String) -> Unit = {},
  ): CompletableFuture<NativeBuildResult>

  fun cancelCurrentBuild(): Boolean
}

class DefaultNativeBuildService(
  private val executor: NativeBuildExecutor,
  private val worker: ExecutorService = Executors.newSingleThreadExecutor(),
) : NativeBuildService {

  private val controller = NativeProcessController()
  private val futureRef = AtomicReference<CompletableFuture<NativeBuildResult>?>()

  override val isBuildInProgress: Boolean
    get() = futureRef.get()?.isDone == false

  override fun execute(
    request: NativeBuildRequest,
    moduleRoot: File,
    buildDirectory: File?,
    onTaskState: (NativeBuildTask, NativeBuildTaskState) -> Unit,
    onOutput: (String) -> Unit,
  ): CompletableFuture<NativeBuildResult> {
    check(!isBuildInProgress) {
      "A native build is already in progress"
    }

    controller.reset()

    val future =
      CompletableFuture.supplyAsync(
        {
          executor.execute(
            request = request,
            moduleRoot = moduleRoot,
            buildDirectory = buildDirectory
              ?: File(
                moduleRoot,
                ".androidide/native/" +
                  request.variant.name.lowercase() + "/" +
                  request.abi.name.lowercase(),
              ),
            onTaskState = onTaskState,
            onOutput = onOutput,
            processController = controller,
          )
        },
        worker,
      )

    futureRef.set(future)
    future.whenComplete { _, _ ->
      futureRef.compareAndSet(future, null)
    }
    return future
  }

  override fun cancelCurrentBuild(): Boolean {
    val future = futureRef.get() ?: return false
    if (future.isDone) {
      return false
    }

    controller.cancel()
    future.cancel(true)
    futureRef.compareAndSet(future, null)
    return true
  }
}
