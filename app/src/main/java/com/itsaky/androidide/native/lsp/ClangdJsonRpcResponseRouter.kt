package com.itsaky.androidide.native.lsp

import com.google.gson.JsonParser
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ClangdJsonRpcResponseRouter(
  private val onNotification: (String) -> Unit = {},
) {

  private val pending = ConcurrentHashMap<Int, CompletableFuture<String>>()

  fun register(requestId: Int): CompletableFuture<String> =
    CompletableFuture<String>().also { future ->
      check(pending.putIfAbsent(requestId, future) == null) {
        "A response is already pending for request id $requestId"
      }
    }

  fun route(json: String) {
    val objectMessage = JsonParser.parseString(json).asJsonObject
    val id = objectMessage.get("id")
    if (id != null && id.isJsonPrimitive && id.asJsonPrimitive.isNumber) {
      val future = pending.remove(id.asInt)
      if (future != null) {
        future.complete(json)
        return
      }
    }
    onNotification(json)
  }

  fun failAll(error: Throwable) {
    pending.values.forEach { it.completeExceptionally(error) }
    pending.clear()
  }

  fun pendingCount(): Int = pending.size
}
