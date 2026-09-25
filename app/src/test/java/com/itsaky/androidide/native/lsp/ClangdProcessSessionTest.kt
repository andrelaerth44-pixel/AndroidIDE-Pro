package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.native.build.NativeCommandSpec
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClangdProcessSessionTest {

  @Test
  fun initialStateIsStopped() {
    val session =
      ClangdProcessSession(
        command = NativeCommandSpec(File("/does/not/exist"), emptyList()),
      )

    assertFalse(session.isRunning)
    session.close()
    assertFalse(session.isRunning)
  }
}
