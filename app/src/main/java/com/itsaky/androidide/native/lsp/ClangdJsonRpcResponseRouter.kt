package com.itsaky.androidide.native.lsp

import com.google.gson.JsonParser
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ClangdJsonRpcResponseRouter(
  onNotification: (String) -> Unit = {},
) {

  @Volatile
  private var notificationHandler: (String) -> Unit = onNotification

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
    notificationHandler(json)
  }

  fun setNotificationHandler(handler: (String) -> Unit) {
    notificationHandler = handler
  }

  fun fail(requestId: Int, error: Throwable) {
    pending.remove(requestId)?.completeExceptionally(error)
  }

  fun failAll(error: Throwable) {
    pending.values.forEach { it.completeExceptionally(error) }
    pending.clear()
  }

  fun pendingCount(): Int = pending.size
}
