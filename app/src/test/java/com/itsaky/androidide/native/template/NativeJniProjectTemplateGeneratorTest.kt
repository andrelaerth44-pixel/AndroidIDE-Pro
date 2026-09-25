package com.itsaky.androidide.native.template

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeJniProjectTemplateGeneratorTest {

  @Test
  fun generatesDependencyFreeJniProject() {
    val root = Files.createTempDirectory("jni-template").toFile()
    try {
      val generated =
        NativeJniProjectTemplateGenerator.generate(
          root,
          NativeJniProjectTemplateSpec(
            packageName = "com.example.hello",
            className = "NativeBridge",
            methodName = "stringFromNative",
            moduleName = "hello",
          ),
        )

      assertEquals(3, generated.size)

      val javaFile = Files.walk(root.toPath())
        .filter { it.fileName.toString() == "NativeBridge.java" }
        .findFirst()
        .get()
        .toFile()
      val cppFile = root.resolve("src/main/cpp/native_bridge.cpp")
      val configFile = root.resolve(".androidide/native.json")

      assertTrue(javaFile.isFile)
      assertTrue(cppFile.isFile)
      assertTrue(configFile.isFile)

      val java = javaFile.readText()
      val cpp = cppFile.readText()

      assertTrue(java.contains("public static native String stringFromNative()"))
      assertTrue(java.contains("System.loadLibrary(\"hello\")"))
      assertTrue(cpp.contains("Java_com_example_hello_NativeBridge_stringFromNative"))
      assertTrue(cpp.contains("#include <jni.h>"))
    } finally {
      root.deleteRecursively()
    }
  }
}
