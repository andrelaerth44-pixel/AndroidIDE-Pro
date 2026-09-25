package com.itsaky.androidide.native.lsp

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.itsaky.androidide.lsp.models.CompletionItem
import com.itsaky.androidide.lsp.models.CompletionItemKind
import com.itsaky.androidide.lsp.models.CompletionResult
import com.itsaky.androidide.lsp.models.InsertTextFormat
import com.itsaky.androidide.lsp.models.MatchLevel

object ClangdCompletionMapper {

  fun mapResponse(json: String): CompletionResult? {
    val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
    val result = root.get("result") ?: return null
    if (result.isJsonNull) return CompletionResult.EMPTY

    val items =
      if (result.isJsonObject) {
        result.asJsonObject.getAsJsonArray("items") ?: JsonArray()
      } else if (result.isJsonArray) {
        result.asJsonArray
      } else {
        return null
      }

    val mapped = items.mapNotNull {
      runCatching { mapItem(it.asJsonObject) }.getOrNull()
    }

    return CompletionResult(mapped)
  }

  private fun mapItem(value: JsonObject): CompletionItem {
    val label = value.get("label")?.asString.orEmpty()
    val detail = value.get("detail")?.asString.orEmpty()
    val insertText =
      value.get("insertText")?.asString
        ?: value.getAsJsonObject("textEdit")?.get("newText")?.asString
        ?: label
    val sortText = value.get("sortText")?.asString
    val format =
      if (value.get("insertTextFormat")?.asInt == 2) {
        InsertTextFormat.SNIPPET
      } else {
        InsertTextFormat.PLAIN_TEXT
      }

    return CompletionItem(
      label = label,
      detail = detail,
      insertText = insertText,
      insertTextFormat = format,
      sortText = sortText,
      command = null,
      completionKind = mapKind(value.get("kind")?.asInt),
      matchLevel = MatchLevel.NO_MATCH,
      additionalTextEdits = null,
      data = null,
    )
  }

  private fun mapKind(value: Int?): CompletionItemKind =
    when (value) {
      2 -> CompletionItemKind.METHOD
      3 -> CompletionItemKind.FUNCTION
      4 -> CompletionItemKind.CONSTRUCTOR
      5 -> CompletionItemKind.FIELD
      6 -> CompletionItemKind.VARIABLE
      7 -> CompletionItemKind.CLASS
      8 -> CompletionItemKind.INTERFACE
      9 -> CompletionItemKind.MODULE
      10 -> CompletionItemKind.PROPERTY
      11, 12 -> CompletionItemKind.VALUE
      13 -> CompletionItemKind.ENUM
      14 -> CompletionItemKind.KEYWORD
      15 -> CompletionItemKind.SNIPPET
      16 -> CompletionItemKind.VALUE
      17, 18, 19 -> CompletionItemKind.VALUE
      20 -> CompletionItemKind.ENUM_MEMBER
      21 -> CompletionItemKind.VARIABLE
      22 -> CompletionItemKind.CLASS
      23, 24 -> CompletionItemKind.VALUE
      25 -> CompletionItemKind.TYPE_PARAMETER
      else -> CompletionItemKind.NONE
    }
}
