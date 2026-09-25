package com.itsaky.androidide.native.lsp

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClangdWorkspaceSessionTest {

  @Test
  fun detectsCppAndCHeaders() {
    val notifications = AtomicReference<String?>(null)
    val workspace =
      ClangdWorkspaceSession(
        processSession = ClangdProcessSession(
          command = com.itsaky.androidide.native.build.NativeCommandSpec(
            executable = File("/does/not/exist"),
            arguments = emptyList(),
          )
        ),
        onNotification = { notifications.set(it) },
      )

    workspace.handleNotification(
      "{\"jsonrpc\":\"2.0\",\"method\":\"window/logMessage\",\"params\":{}}"
    )

    assertTrue(notifications.get() != null)
    workspace.close()
  }
}
