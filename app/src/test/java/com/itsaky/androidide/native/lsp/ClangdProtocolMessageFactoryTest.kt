package com.itsaky.androidide.native.lsp

import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClangdProtocolMessageFactoryTest {

  @Test
  fun initializeContainsWorkspaceAndCapabilities() {
    val json =
      JsonParser.parseString(
        ClangdProtocolMessageFactory.initialize(7, File("/workspace"))
      ).asJsonObject

    assertEquals("2.0", json.get("jsonrpc").asString)
    assertEquals(7, json.get("id").asInt)
    assertEquals("initialize", json.get("method").asString)
    assertEquals(
      "file:/workspace/",
      json.getAsJsonObject("params").get("rootUri").asString,
    )
    assertTrue(json.getAsJsonObject("params").has("capabilities"))
  }

  @Test
  fun didChangeUsesWholeDocumentReplacement() {
    val json =
      JsonParser.parseString(
        ClangdProtocolMessageFactory.didChange(
          File("/workspace/native.cpp"),
          3,
          "int main() { return 0; }",
        )
      ).asJsonObject

    assertEquals("textDocument/didChange", json.get("method").asString)
    assertEquals(3, json.getAsJsonObject("params")
      .getAsJsonObject("textDocument").get("version").asInt)
    assertEquals(1, json.getAsJsonArray("params")
      .getAsJsonArray("contentChanges").size())
    assertEquals(
      "int main() { return 0; }",
      params.getAsJsonArray("contentChanges")
        .get(0).asJsonObject.get("text").asString,
    )
  }

  @Test
  fun shutdownRequestOmitsParamsAndExitIsNotification() {
    val shutdown =
      JsonParser.parseString(
        ClangdProtocolMessageFactory.shutdown(11)
      ).asJsonObject

    assertEquals("shutdown", shutdown.get("method").asString)
    assertEquals(11, shutdown.get("id").asInt)
    assertTrue(!shutdown.has("params"))

    val exit =
      JsonParser.parseString(
        ClangdProtocolMessageFactory.exit()
      ).asJsonObject

    assertEquals("exit", exit.get("method").asString)
    assertTrue(!exit.has("id"))
  }

  @Test
  fun completionUsesZeroBasedPosition() {
    val json =
      JsonParser.parseString(
        ClangdProtocolMessageFactory.completion(
          9,
          File("/workspace/native.cpp"),
          4,
          12,
        )
      ).asJsonObject

    assertEquals(9, json.get("id").asInt)
    assertEquals("textDocument/completion", json.get("method").asString)
    val position = json.getAsJsonObject("params").getAsJsonObject("position")
    assertEquals(4, position.get("line").asInt)
    assertEquals(12, position.get("character").asInt)
  }
}
