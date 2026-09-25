package com.itsaky.androidide.native.model

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NativeProjectModelLoaderTest {

  @Test
  fun loaderBuildsModuleFromNativeSources() {
    val module = Files.createTempDirectory("native-module").toFile()
    try {
      File(module, "src/main/c").mkdirs()
      File(module, "src/main/c/native.c").writeText("int native_c() { return 0; }")
      File(module, "src/main/cpp").mkdirs()
      File(module, "src/main/cpp/native.cpp").writeText("int native_cpp() { return 0; }")

      val model =
        NativeProjectModelLoader.load(
          moduleRoot = module,
          abi = AbiTarget.ARM64_V8A,
          variant = BuildVariant.DEBUG,
        )

      assertNotNull(model)
      assertEquals(module.name, model?.moduleName)
      assertEquals(1, model?.targets?.size)
      assertEquals(1, model?.targets?.single()?.sourceSet?.cSources?.size)
      assertEquals(1, model?.targets?.single()?.sourceSet?.cppSources?.size)
    } finally {
      module.deleteRecursively()
    }
  }

  @Test
  fun loaderIgnoresModulesWithoutNativeSources() {
    val module = Files.createTempDirectory("empty-module").toFile()
    try {
      File(module, "src/main/kotlin").mkdirs()
      File(module, "src/main/kotlin/Main.kt").writeText("class Main")

      assertNull(NativeProjectModelLoader.load(module))
    } finally {
      module.deleteRecursively()
    }
  }
}
