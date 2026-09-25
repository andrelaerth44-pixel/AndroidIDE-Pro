package com.itsaky.androidide.native.lsp

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClangdJsonRpcResponseRouterTest {

  @Test
  fun routesResponsesByNumericId() {
    val notifications = mutableListOf<String>()
    val router = ClangdJsonRpcResponseRouter { notifications += it }
    val response = router.register(7)

    router.route("{\"jsonrpc\":\"2.0\",\"id\":7,\"result\":{}}")

    assertEquals(0, router.pendingCount())
    assertEquals("{\"jsonrpc\":\"2.0\",\"id\":7,\"result\":{}}", response.get(1, TimeUnit.SECONDS))
    assertTrue(notifications.isEmpty())
  }

  @Test
  fun routesNotificationsWithoutIds() {
    val notifications = mutableListOf<String>()
    val router = ClangdJsonRpcResponseRouter { notifications += it }

    router.route("{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/publishDiagnostics\",\"params\":{}}")

    assertEquals(1, notifications.size)
    assertEquals("textDocument/publishDiagnostics",
      com.google.gson.JsonParser.parseString(notifications.single()).asJsonObject.get("method").asString)
  }

  @Test
  fun failOnlyCompletesTheRequestedFuture() {
    val router = ClangdJsonRpcResponseRouter()
    val first = router.register(1)
    val second = router.register(2)

    router.fail(1, IllegalStateException("request failed"))

    assertTrue(first.isCompletedExceptionally)
    assertEquals(1, router.pendingCount())

    router.route("{"jsonrpc":"2.0","id":2,"result":{}}")
    assertEquals(0, router.pendingCount())
    assertTrue(second.isDone)
  }

  @Test
  fun failingRouterCompletesPendingRequestsExceptionally() {
    val router = ClangdJsonRpcResponseRouter()
    val response = router.register(3)
    val error = IllegalStateException("clangd stopped")

    router.failAll(error)

    assertTrue(response.isCompletedExceptionally)
    assertEquals(0, router.pendingCount())
  }
}
