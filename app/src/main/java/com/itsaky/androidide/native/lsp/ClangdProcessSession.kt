package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.native.build.NativeCommandSpec
import com.itsaky.androidide.native.build.NativeProcessController
import com.itsaky.androidide.utils.Environment
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader

class ClangdProcessSession(
  private val command: NativeCommandSpec,
  private val environment: Map<String, String> = Environment.getEnvironment(),
  private val processController: NativeProcessController = NativeProcessController(),
  private val onStderrLine: (String) -> Unit = {},
) : Closeable {

  private var process: Process? = null
  private var transport: ClangdJsonRpcTransport? = null

  val isRunning: Boolean
    get() = process?.isAlive == true

  @Synchronized
  fun start() {
    check(process == null || process?.isAlive != true) { "clangd session is already running" }

    processController.reset()
    val builder = ProcessBuilder(command.asCommandLine())
    command.workingDirectory?.let { builder.directory(it) }
    builder.environment().putAll(environment)

    val started = builder.start()
    process = started
    processController.attach(started)
    transport = ClangdJsonRpcTransport(
      input = started.inputStream,
      output = started.outputStream,
    )

    Thread({
      BufferedReader(InputStreamReader(started.errorStream)).useLines { lines ->
        lines.forEach(onStderrLine)
      }
    }, "androidide-clangd-stderr").apply {
      isDaemon = true
      start()
    }
  }

  fun send(json: String) {
    check(isRunning) { "clangd session is not running" }
    checkNotNull(transport).sendJson(json)
  }

  fun read(): String? {
    check(isRunning) { "clangd session is not running" }
    return checkNotNull(transport).readJson()
  }

  fun initialize(workspaceRoot: File, requestId: Int = 1) {
    send(ClangdProtocolMessageFactory.initialize(requestId, workspaceRoot))
    send(ClangdProtocolMessageFactory.initialized())
  }

  @Synchronized
  fun stop() {
    val current = process ?: return
    processController.cancel()
    runCatching { current.waitFor() }
    processController.clear(current)
    process = null
    transport = null
  }

  override fun close() {
    stop()
  }
}
