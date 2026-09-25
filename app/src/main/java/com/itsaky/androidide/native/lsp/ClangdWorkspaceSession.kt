package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.lsp.models.CompletionResult
import com.itsaky.androidide.lsp.models.DiagnosticResult
import java.io.Closeable
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

/** High-level native language workspace backed by one persistent clangd process. */
class ClangdWorkspaceSession(
  private val processSession: ClangdProcessSession,
  private val onDiagnostics: (DiagnosticResult) -> Unit = {},
  private val onNotification: (String) -> Unit = {},
) : Closeable {

  private val nextRequestId = AtomicInteger(2)

  init {
    processSession.setNotificationHandler(::handleNotification)
  }

  fun start(
    workspaceRoot: File,
    initializeRequestId: Int = 1,
  ): CompletableFuture<String> {
    processSession.start()
    return processSession.initialize(workspaceRoot, initializeRequestId).also { response ->
      response.thenRun { processSession.initialized() }
    }
  }

  fun openFile(file: File, version: Int, text: String) {
    processSession.send(
      ClangdProtocolMessageFactory.didOpen(
        file = file,
        languageId = languageId(file),
        version = version,
        text = text,
      )
    )
  }

  fun changeFile(file: File, version: Int, text: String) {
    processSession.send(
      ClangdProtocolMessageFactory.didChange(file, version, text)
    )
  }

  fun closeFile(file: File) {
    processSession.send(ClangdProtocolMessageFactory.didClose(file))
  }

  fun completion(
    file: File,
    line: Int,
    character: Int,
  ): CompletableFuture<CompletionResult?> {
    val requestId = nextRequestId.getAndIncrement()
    return processSession
      .sendRequest(
        json = ClangdProtocolMessageFactory.completion(
          id = requestId,
          file = file,
          line = line,
          character = character,
        ),
        requestId = requestId,
      )
      .thenApply(ClangdCompletionMapper::mapResponse)
  }

  fun handleNotification(json: String) {
    ClangdDiagnosticMapper.mapPublishDiagnostics(json)?.let {
      onDiagnostics(it)
      return
    }
    onNotification(json)
  }

  fun stop() {
    if (!processSession.isRunning) return

    runCatching {
      val requestId = nextRequestId.getAndIncrement()
      val response =
        processSession.sendRequest(
          json = ClangdProtocolMessageFactory.shutdown(requestId),
          requestId = requestId,
        )
      response.whenComplete { _, _ ->
        runCatching {
          processSession.send(ClangdProtocolMessageFactory.exit())
        }
        processSession.stop()
      }
    }.onFailure {
      runCatching { processSession.send(ClangdProtocolMessageFactory.exit()) }
      processSession.stop()
    }
  }

  override fun close() {
    stop()
  }

  private fun languageId(file: File): String =
    when (file.extension.lowercase()) {
      "c" -> "c"
      "m" -> "objective-c"
      "mm" -> "objective-cpp"
      "cc", "cpp", "cxx", "hpp", "hh", "hxx" -> "cpp"
      "h" -> "c"
      else -> "cpp"
    }
}
