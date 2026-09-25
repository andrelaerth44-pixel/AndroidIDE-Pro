package com.itsaky.androidide.native.lsp

import com.itsaky.androidide.lsp.models.CompletionItemKind
import com.itsaky.androidide.lsp.models.InsertTextFormat
import java.util.stream.Collectors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ClangdCompletionMapperTest {

  @Test
  fun mapsCompletionListToIdeItems() {
    val json =
      """
      {
        "jsonrpc":"2.0",
        "id":4,
        "result":{
          "isIncomplete":false,
          "items":[
            {
              "label":"printf",
              "kind":3,
              "detail":"int(const char*, ...)",
              "insertText":"printf",
              "insertTextFormat":1,
              "sortText":"0001"
            },
            {
              "label":"Widget",
              "kind":7,
              "detail":"class Widget",
              "textEdit":{"newText":"Widget"},
              "insertTextFormat":2
            }
          ]
        }
      }
      """.trimIndent()

    val result = ClangdCompletionMapper.mapResponse(json)
    assertNotNull(result)
    assertEquals(2, result?.items?.size)

    val printf = result!!.items[0]
    assertEquals("printf", printf.label)
    assertEquals(CompletionItemKind.FUNCTION, printf.completionKind)
    assertEquals(InsertTextFormat.PLAIN_TEXT, printf.insertTextFormat)
    assertEquals("int(const char*, ...)", printf.detail)

    val widget = result.items[1]
    assertEquals(CompletionItemKind.CLASS, widget.completionKind)
    assertEquals(InsertTextFormat.SNIPPET, widget.insertTextFormat)
    assertEquals("Widget", widget.insertText)
  }

  @Test
  fun mapsNullResultToEmptyCompletion() {
    val result =
      ClangdCompletionMapper.mapResponse(
        "{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":null}"
      )

    assertNotNull(result)
    assertEquals(0, result?.items?.size)
  }
}
