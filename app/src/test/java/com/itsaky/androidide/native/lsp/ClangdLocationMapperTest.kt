package com.itsaky.androidide.native.lsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClangdLocationMapperTest {

  @Test
  fun mapsDefinitionLocation() {
    val json =
      """{"jsonrpc":"2.0","id":1,"result":[{"uri":"file:///tmp/native.cpp","range":{"start":{"line":2,"character":4},"end":{"line":2,"character":12}}}]}"""

    val result = checkNotNull(ClangdLocationMapper.mapDefinition(json))

    assertEquals(1, result.locations.size)
    assertEquals("native.cpp", result.locations.single().file.fileName.toString())
    assertEquals(2, result.locations.single().range.start.line)
    assertEquals(4, result.locations.single().range.start.column)
  }

  @Test
  fun mapsReferenceArrayAndHandlesNullResult() {
    val json =
      """{"jsonrpc":"2.0","id":2,"result":[{"uri":"file:///tmp/a.cpp","range":{"start":{"line":0,"character":0},"end":{"line":0,"character":3}}},{"uri":"file:///tmp/b.cpp","range":{"start":{"line":4,"character":2},"end":{"line":4,"character":5}}}]}"""

    val result = checkNotNull(ClangdLocationMapper.mapReferences(json))
    assertEquals(2, result.locations.size)

    val empty =
      checkNotNull(
        ClangdLocationMapper.mapReferences(
          """{"jsonrpc":"2.0","id":3,"result":null}"""
        )
      )
    assertTrue(empty.locations.isEmpty())
  }
}
