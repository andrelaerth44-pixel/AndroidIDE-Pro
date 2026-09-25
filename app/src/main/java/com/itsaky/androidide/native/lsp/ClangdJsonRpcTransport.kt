package com.itsaky.androidide.native.lsp

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/** Minimal LSP JSON-RPC transport used by the long-running clangd process. */
class ClangdJsonRpcTransport(
  private val input: InputStream,
  private val output: OutputStream,
) {

  @Synchronized
  fun sendJson(json: String) {
    val payload = json.toByteArray(StandardCharsets.UTF_8)
    val header =
      (
        "Content-Length: " + payload.size + "\r\n" +
          "Content-Type: application/vscode-jsonrpc; charset=utf-8\r\n" +
          "\r\n"
      ).toByteArray(StandardCharsets.US_ASCII)
    output.write(header)
    output.write(payload)
    output.flush()
  }

  fun readJson(): String? {
    val contentLength = readContentLength() ?: return null
    val payload = ByteArray(contentLength)
    var offset = 0
    while (offset < contentLength) {
      val count = input.read(payload, offset, contentLength - offset)
      if (count < 0) {
        throw EOFException("Unexpected end of clangd JSON-RPC payload")
      }
      offset += count
    }
    return payload.toString(StandardCharsets.UTF_8)
  }

  private fun readContentLength(): Int? {
    val headers = ByteArrayOutputStream()
    var matched = 0
    val delimiter = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())

    while (true) {
      val value = input.read()
      if (value < 0) {
        return if (headers.size() == 0) null else throw EOFException(
          "Unexpected end of clangd JSON-RPC headers"
        )
      }
      val byte = value.toByte()
      headers.write(value)
      if (byte == delimiter[matched]) {
        matched++
        if (matched == delimiter.size) {
          break
        }
      } else {
        matched = if (byte == delimiter[0]) 1 else 0
      }
    }

    val headerText = headers.toByteArray().toString(StandardCharsets.US_ASCII)
    val length =
      headerText.lineSequence()
        .map(String::trim)
        .firstNotNullOfOrNull { line ->
          if (line.startsWith("Content-Length:", ignoreCase = true)) {
            line.substringAfter(':').trim().toIntOrNull()
          } else {
            null
          }
        }
        ?: throw IllegalStateException("clangd response did not contain Content-Length")

    require(length >= 0) { "clangd Content-Length must be non-negative" }
    return length
  }
}
