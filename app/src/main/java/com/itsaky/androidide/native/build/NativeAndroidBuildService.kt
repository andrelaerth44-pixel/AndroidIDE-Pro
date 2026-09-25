package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.BuildVariant
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

interface NativeAndroidBuildService {
  val isBuildInProgress: Boolean

  fun execute(
    moduleRoot: File,
    variant: BuildVariant = BuildVariant.DEBUG,
    abi: AbiTarget = AbiTarget.ARM64_V8A,
    onStage: (NativeAndroidBuildStage, String?) -> Unit = { _, _ -> },
    onOutput: (String) -> Unit = {},
  ): CompletableFuture<NativeAndroidBuildResult>

  fun cancelCurrentBuild(): Boolean
}

class DefaultNativeAndroidBuildService(
  private val executor: NativeAndroidBuildExecutor = NativeAndroidBuildExecutor(),
) : NativeAndroidBuildService {

  private val worker = Executors.newSingleThreadExecutor()
  private val building = AtomicBoolean(false)
  private var future: CompletableFuture<NativeAndroidBuildResult>? = null
  private val controller = NativeProcessController()

  override val isBuildInProgress: Boolean
    get() = building.get()

  @Synchronized
  override fun execute(
    moduleRoot: File,
    variant: BuildVariant,
    abi: AbiTarget,
    onStage: (NativeAndroidBuildStage, String?) -> Unit,
    onOutput: (String) -> Unit,
  ): CompletableFuture<NativeAndroidBuildResult> {
    check(building.compareAndSet(false, true)) {
      "A native Android build is already in progress"
    }

    controller.reset()
    val result = CompletableFuture<NativeAndroidBuildResult>()
    future = result

    worker.execute {
      try {
        val buildResult =
          executor.execute(
            moduleRoot = moduleRoot,
            variant = variant,
            abi = abi,
            controller = controller,
            onStage = onStage,
            onOutput = onOutput,
          )
        result.complete(buildResult)
      } catch (error: Throwable) {
        result.complete(
          NativeAndroidBuildResult(
            success = false,
            failedStage = NativeAndroidBuildStage.FAILED,
            message = error.message ?: error.javaClass.simpleName,
          )
        )
      } finally {
        building.set(false)
        future = null
      }
    }

    return result
  }

  @Synchronized
  override fun cancelCurrentBuild(): Boolean {
    if (!building.get()) return false
    controller.cancel()
    future?.cancel(true)
    return true
  }
}
