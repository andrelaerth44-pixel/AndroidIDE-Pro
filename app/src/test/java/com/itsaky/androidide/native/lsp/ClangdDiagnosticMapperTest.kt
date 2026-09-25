package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.lsp.models.DiagnosticSeverity
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ClangdDiagnosticMapperTest {

  @Test
  fun mapsPublishDiagnosticsIntoIdeModel() {
    val json =
      """
      {
        "jsonrpc":"2.0",
        "method":"textDocument/publishDiagnostics",
        "params":{
          "uri":"file:///workspace/native.cpp",
          "diagnostics":[
            {
              "range":{"start":{"line":2,"character":4},"end":{"line":2,"character":10}},
              "severity":2,
              "code":"-Wextra",
              "source":"clangd",
              "message":"unused variable"
            }
          ]
        }
      }
      """.trimIndent()

    val result = ClangdDiagnosticMapper.mapPublishDiagnostics(json)
    assertNotNull(result)
    assertEquals(Paths.get("/workspace/native.cpp"), result?.file)
    assertEquals(1, result?.diagnostics?.size)
    val diagnostic = result!!.diagnostics.single()
    assertEquals("unused variable", diagnostic.message)
    assertEquals("-Wextra", diagnostic.code)
    assertEquals(DiagnosticSeverity.WARNING, diagnostic.severity)
    assertEquals(2, diagnostic.range.start.line)
    assertEquals(4, diagnostic.range.start.column)
    assertEquals(10, diagnostic.range.end.column)
  }

  @Test
  fun ignoresOtherNotifications() {
    val result =
      ClangdDiagnosticMapper.mapPublishDiagnostics(
        "{\"jsonrpc\":\"2.0\",\"method\":\"initialized\"}"
      )

    assertNull(result)
  }
}
