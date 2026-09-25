package com.itsaky.androidide.native.build

import java.util.concurrent.atomic.AtomicReference

class NativeProcessController {

  private val processRef = AtomicReference<Process?>()

  fun attach(process: Process) {
    processRef.set(process)
  }

  fun clear(process: Process) {
    processRef.compareAndSet(process, null)
  }

  fun cancel() {
    processRef.get()?.let { process ->
      process.destroy()
      if (process.isAlive) {
        process.destroyForcibly()
      }
    }
  }

  fun isRunning(): Boolean =
    processRef.get()?.isAlive == true
}
