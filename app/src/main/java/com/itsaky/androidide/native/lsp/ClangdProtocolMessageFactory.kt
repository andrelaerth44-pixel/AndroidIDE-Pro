package com.itsaky.androidide.native.lsp

import com.google.gson.JsonArray
import com.google.gson.JsonObject

object ClangdProtocolMessageFactory {

  fun initialize(id: Int, workspaceRoot: java.io.File): String {
    val rootUri = workspaceRoot.toURI().toString()
    val params = JsonObject().apply {
      addProperty("processId", android.os.Process.myPid())
      addProperty("rootUri", rootUri)
      add("capabilities", JsonObject())
      add("initializationOptions", JsonObject())
      addProperty("trace", "off")
    }
    return request(id, "initialize", params)
  }

  fun initialized(): String = notification("initialized", JsonObject())

  fun didOpen(file: java.io.File, languageId: String, version: Int, text: String): String {
    val textDocument = JsonObject().apply {
      addProperty("uri", file.toURI().toString())
      addProperty("languageId", languageId)
      addProperty("version", version)
      addProperty("text", text)
    }
    return notification(
      "textDocument/didOpen",
      JsonObject().apply { add("textDocument", textDocument) },
    )
  }

  fun didChange(file: java.io.File, version: Int, text: String): String {
    val changes = JsonArray().apply {
      add(
        JsonObject().apply {
          addProperty("text", text)
        }
      )
    }
    val textDocument = JsonObject().apply {
      addProperty("uri", file.toURI().toString())
      addProperty("version", version)
    }
    return notification(
      "textDocument/didChange",
      JsonObject().apply {
        add("textDocument", textDocument)
        add("contentChanges", changes)
      },
    )
  }

  fun didClose(file: java.io.File): String =
    notification(
      "textDocument/didClose",
      JsonObject().apply {
        add("textDocument", JsonObject().apply {
          addProperty("uri", file.toURI().toString())
        })
      },
    )


  fun definition(id: Int, file: java.io.File, line: Int, character: Int): String =
    positionRequest(id, "textDocument/definition", file, line, character)

  fun references(
    id: Int,
    file: java.io.File,
    line: Int,
    character: Int,
    includeDeclaration: Boolean,
  ): String =
    JsonObject().apply {
      addProperty("jsonrpc", "2.0")
      addProperty("id", id)
      addProperty("method", "textDocument/references")
      add("params", JsonObject().apply {
        add("textDocument", JsonObject().apply {
          addProperty("uri", file.toURI().toString())
        })
        add("position", JsonObject().apply {
          addProperty("line", line)
          addProperty("character", character)
        })
        add("context", JsonObject().apply {
          addProperty("includeDeclaration", includeDeclaration)
        })
      })
    }.toString()

  private fun positionRequest(
    id: Int,
    method: String,
    file: java.io.File,
    line: Int,
    character: Int,
  ): String =
    request(
      id,
      method,
      JsonObject().apply {
        add("textDocument", JsonObject().apply {
          addProperty("uri", file.toURI().toString())
        })
        add("position", JsonObject().apply {
          addProperty("line", line)
          addProperty("character", character)
        })
      },
    )
  fun completion(id: Int, file: java.io.File, line: Int, character: Int): String {
    val params = JsonObject().apply {
      add("textDocument", JsonObject().apply {
        addProperty("uri", file.toURI().toString())
      })
      add("position", JsonObject().apply {
        addProperty("line", line)
        addProperty("character", character)
      })
    }
    return request(id, "textDocument/completion", params)
  }

  fun shutdown(id: Int): String = request(id, "shutdown", null)

  fun exit(): String = notification("exit", null)

  private fun request(id: Int, method: String, params: com.google.gson.JsonElement?): String =
    JsonObject().apply {
      addProperty("jsonrpc", "2.0")
      addProperty("id", id)
      addProperty("method", method)
      if (params != null) add("params", params)
    }.toString()

  private fun notification(method: String, params: com.google.gson.JsonElement?): String =
    JsonObject().apply {
      addProperty("jsonrpc", "2.0")
      addProperty("method", method)
      if (params != null) add("params", params)
    }.toString()
}
