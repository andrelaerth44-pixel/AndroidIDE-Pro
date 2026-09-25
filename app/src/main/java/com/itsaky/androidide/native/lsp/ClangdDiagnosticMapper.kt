package com.itsaky.androidide.native.lsp

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.itsaky.androidide.lsp.models.DiagnosticItem
import com.itsaky.androidide.lsp.models.DiagnosticResult
import com.itsaky.androidide.lsp.models.DiagnosticSeverity
import com.itsaky.androidide.models.Position
import com.itsaky.androidide.models.Range
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

object ClangdDiagnosticMapper {

  fun mapPublishDiagnostics(json: String): DiagnosticResult? {
    val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
    if (root.get("method")?.asString != "textDocument/publishDiagnostics") {
      return null
    }

    val params = root.getAsJsonObject("params") ?: return null
    val uri = params.get("uri")?.asString ?: return null
    val file = runCatching { Paths.get(URI(uri)) }.getOrNull() ?: return null
    val diagnostics = params.getAsJsonArray("diagnostics") ?: return DiagnosticResult(file, emptyList())

    return DiagnosticResult(
      file = file,
      diagnostics = diagnostics.mapNotNull { element ->
        mapDiagnostic(element.asJsonObject)
      },
    )
  }

  private fun mapDiagnostic(value: JsonObject): DiagnosticItem? {
    val range = value.getAsJsonObject("range") ?: return null
    val start = range.getAsJsonObject("start") ?: return null
    val end = range.getAsJsonObject("end") ?: return null

    return DiagnosticItem(
      message = value.get("message")?.asString.orEmpty(),
      code = value.get("code")?.let(::codeText).orEmpty(),
      range = Range(
        start = Position(
          start.get("line")?.asInt ?: 0,
          start.get("character")?.asInt ?: 0,
        ),
        end = Position(
          end.get("line")?.asInt ?: 0,
          end.get("character")?.asInt ?: 0,
        ),
      ),
      source = value.get("source")?.asString ?: "clangd",
      severity = mapSeverity(value.get("severity")?.asInt),
    )
  }

  private fun codeText(value: JsonElement): String =
    when {
      value.isJsonPrimitive && value.asJsonPrimitive.isString -> value.asString
      value.isJsonPrimitive -> value.toString()
      else -> value.toString()
    }

  private fun mapSeverity(value: Int?): DiagnosticSeverity =
    when (value) {
      1 -> DiagnosticSeverity.ERROR
      2 -> DiagnosticSeverity.WARNING
      3 -> DiagnosticSeverity.INFO
      4 -> DiagnosticSeverity.HINT
      else -> DiagnosticSeverity.ERROR
    }
}
