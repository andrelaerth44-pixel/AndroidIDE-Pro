package com.itsaky.androidide.language

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeLanguageScannerTest {

  @Test
  fun detectsCSourcesAndHeaders() {
    val root = Files.createTempDirectory("androidide-c-scanner")
    Files.writeString(root.resolve("main.c"), "int main() { return 0; }")
    Files.writeString(root.resolve("main.h"), "#pragma once")
    Files.writeString(root.resolve("notes.txt"), "unsupported")

    val report = NativeLanguageScanner.scan(listOf(root))

    assertTrue(
      report.filesByLanguage[BuiltInLanguage.C].orEmpty().any {
        it.fileName.toString() == "main.c"
      }
    )
    assertTrue(
      report.filesByLanguage[BuiltInLanguage.C].orEmpty().any {
        it.fileName.toString() == "main.h"
      }
    )
    assertEquals(1, report.unknownFiles.size)
  }

  @Test
  fun detectsCppFamily() {
    val root = Files.createTempDirectory("androidide-cpp-scanner")
    listOf("a.cc", "b.cpp", "c.cxx", "d.hh", "e.hpp", "f.hxx").forEach {
      Files.writeString(root.resolve(it), "")
    }

    val report = NativeLanguageScanner.scan(listOf(root))

    assertEquals(
      6,
      report.filesByLanguage[BuiltInLanguage.CPP].orEmpty().size
    )
    assertTrue(report.unknownFiles.isEmpty())
  }
}
