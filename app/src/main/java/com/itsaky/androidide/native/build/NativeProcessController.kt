package com.itsaky.androidide.native.build

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class NativeProcessController {

  private val processRef = AtomicReference<Process?>()
  private val cancelRequested = AtomicBoolean(false)

  fun reset() {
    cancelRequested.set(false)
    processRef.set(null)
  }

  fun attach(process: Process) {
    processRef.set(process)
    if (cancelRequested.get()) {
      destroy(process)
    }
  }

  fun clear(process: Process) {
    processRef.compareAndSet(process, null)
  }

  fun cancel() {
    cancelRequested.set(true)
    processRef.get()?.let(::destroy)
  }

  fun isRunning(): Boolean =
    processRef.get()?.isAlive == true

  private fun destroy(process: Process) {
    process.destroy()
    if (process.isAlive) {
      process.destroyForcibly()
    }
  }
}
