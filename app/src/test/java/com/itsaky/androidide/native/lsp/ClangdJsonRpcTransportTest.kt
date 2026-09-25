package com.itsaky.androidide.native.lsp

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class ClangdJsonRpcTransportTest {

  @Test
  fun sendsUtf8JsonRpcFrame() {
    val output = ByteArrayOutputStream()
    val transport =
      ClangdJsonRpcTransport(
        input = ByteArrayInputStream(ByteArray(0)),
        output = output,
      )

    transport.sendJson("{\"method\":\"initialize\",\"params\":{\"name\":\"AndroidIDE Pro\"}}")

    val frame = output.toString(Charsets.UTF_8.name())
    assertEquals(
      "Content-Length: 59\r\nContent-Type: application/vscode-jsonrpc; charset=utf-8\r\n\r\n{" +
        "\"method\":\"initialize\",\"params\":{\"name\":\"AndroidIDE Pro\"}}",
      frame,
    )
  }

  @Test
  fun readsContentLengthFramedJson() {
    val json = "{\"id\":1,\"result\":{\"capabilities\":{}}}"
    val bytes =
      (
        "Content-Length: " + json.toByteArray(Charsets.UTF_8).size +
          "\r\n\r\n" +
          json
      ).toByteArray(Charsets.UTF_8)

    val transport =
      ClangdJsonRpcTransport(
        input = ByteArrayInputStream(bytes),
        output = ByteArrayOutputStream(),
      )

    assertEquals(json, transport.readJson())
    assertEquals(null, transport.readJson())
  }
}
