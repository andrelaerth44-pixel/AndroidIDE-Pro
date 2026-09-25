package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.native.build.NativeCommandSpec
import com.itsaky.androidide.native.build.NativeProcessController
import com.itsaky.androidide.utils.Environment
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class ClangdProcessSession(
  private val command: NativeCommandSpec,
  private val environment: Map<String, String> = Environment.getEnvironment(),
  private val processController: NativeProcessController = NativeProcessController(),
  private val onStderrLine: (String) -> Unit = {},
  onNotification: (String) -> Unit = {},
) : Closeable {

  private var process: Process? = null
  private var transport: ClangdJsonRpcTransport? = null
  private var readerThread: Thread? = null
  private val nextRequestId = AtomicInteger(1)
  @Volatile
  private var notificationHandler: (String) -> Unit = onNotification

  private val responseRouter =
    ClangdJsonRpcResponseRouter { notificationHandler(it) }

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

    readerThread =
      Thread({
        try {
          while (started.isAlive) {
            val message = transport?.readJson() ?: break
            responseRouter.route(message)
          }
        } catch (error: Throwable) {
          responseRouter.failAll(error)
        }
      }, "androidide-clangd-reader").apply {
        isDaemon = true
        start()
      }

    Thread({
      BufferedReader(InputStreamReader(started.errorStream)).useLines { lines ->
        lines.forEach(onStderrLine)
      }
    }, "androidide-clangd-stderr").apply {
      isDaemon = true
      start()
    }
  }

  fun setNotificationHandler(handler: (String) -> Unit) {
    notificationHandler = handler
  }

  fun send(json: String) {
    check(isRunning) { "clangd session is not running" }
    checkNotNull(transport).sendJson(json)
  }

  fun sendRequest(
    json: String,
    requestId: Int = nextRequestId.getAndIncrement(),
  ): CompletableFuture<String> {
    check(isRunning) { "clangd session is not running" }
    val future = responseRouter.register(requestId)
    try {
      send(json)
    } catch (error: Throwable) {
      responseRouter.fail(requestId, error)
      throw error
    }
    return future
  }

  fun initialize(
    workspaceRoot: File,
    requestId: Int = nextRequestId.getAndIncrement(),
  ): CompletableFuture<String> =
    sendRequest(
      json = ClangdProtocolMessageFactory.initialize(requestId, workspaceRoot),
      requestId = requestId,
    )

  fun initialized() {
    send(ClangdProtocolMessageFactory.initialized())
  }

  @Synchronized
  fun stop() {
    val current = process ?: return
    processController.cancel()
    runCatching { current.waitFor() }
    responseRouter.failAll(IllegalStateException("clangd session stopped"))
    processController.clear(current)
    process = null
    transport = null
    readerThread = null
  }

  override fun close() {
    stop()
  }
}
