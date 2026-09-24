package com.itsaky.androidide.build.engine

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFingerprintTest {

  @Test
  fun contentChangeInvalidatesFingerprint() {
    val root = Files.createTempDirectory("androidide-fingerprint-test")
    val input = root.resolve("input.txt")
    val output = root.resolve("output.bin")
    val cache = root.resolve("cache")

    Files.writeString(input, "one")
    Files.writeString(output, "built")

    val task = TestTask(input, output)

    assertFalse(TaskFingerprints.isUpToDate(task, cache))

    TaskFingerprints.write(task, cache)

    assertTrue(TaskFingerprints.isUpToDate(task, cache))

    Files.writeString(input, "two")

    assertFalse(TaskFingerprints.isUpToDate(task, cache))
  }

  private class TestTask(
    private val input: java.nio.file.Path,
    private val output: java.nio.file.Path
  ) : BuildTask {
    override val id: String = "testFingerprint"
    override val inputs = listOf(input)
    override val outputs = listOf(output)

    override fun execute(context: BuildContext): TaskResult =
      TaskResult(true)
  }
}
