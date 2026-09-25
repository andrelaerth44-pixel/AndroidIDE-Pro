package com.itsaky.androidide.native.lsp

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.itsaky.androidide.lsp.models.DefinitionResult
import com.itsaky.androidide.lsp.models.ReferenceResult
import com.itsaky.androidide.models.Location
import com.itsaky.androidide.models.Position
import com.itsaky.androidide.models.Range
import java.net.URI
import java.nio.file.Paths

object ClangdLocationMapper {

  fun mapDefinition(json: String): DefinitionResult? {
    val root = parse(json) ?: return null
    val result = root.get("result") ?: return DefinitionResult(emptyList())
    return DefinitionResult(mapLocations(result))
  }

  fun mapReferences(json: String): ReferenceResult? {
    val root = parse(json) ?: return null
    val result = root.get("result") ?: return ReferenceResult(emptyList())
    return ReferenceResult(mapLocations(result))
  }

  private fun parse(json: String) =
    runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()

  private fun mapLocations(value: JsonElement): List<Location> {
    if (!value.isJsonArray) {
      if (value.isJsonObject) return listOfNotNull(mapLocation(value.asJsonObject))
      return emptyList()
    }
    return value.asJsonArray.mapNotNull { element ->
      if (element.isJsonObject) mapLocation(element.asJsonObject) else null
    }
  }

  private fun mapLocation(value: com.google.gson.JsonObject): Location? {
    val uri =
      value.get("uri")?.asString
        ?: value.get("targetUri")?.asString
        ?: return null
    val range =
      value.getAsJsonObject("range")
        ?: value.getAsJsonObject("targetRange")
        ?: value.getAsJsonObject("targetSelectionRange")
        ?: return null
    val start = range.getAsJsonObject("start") ?: return null
    val end = range.getAsJsonObject("end") ?: return null

    val file = runCatching { Paths.get(URI(uri)) }.getOrNull() ?: return null
    return Location(
      file = file,
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
    )
  }
}
