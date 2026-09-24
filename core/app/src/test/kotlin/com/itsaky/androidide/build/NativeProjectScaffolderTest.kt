package com.itsaky.androidide.build

import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeProjectScaffolderTest {

  @Test
  fun createsPureCppNativeActivityProject() {
    val root = Files.createTempDirectory("androidide-pure-cpp")
    val sdk = Files.createTempDirectory("androidide-sdk")

    NativeProjectScaffolder.create(
      NativeProjectScaffoldRequest(
        rootDir = root,
        packageName = "com.example.nativecpp",
        appName = "Native C++",
        sdkRoot = sdk,
        compileSdk = 35,
        kind = NativeProjectKind.PURE_CPP
      )
    )

    assertTrue(Files.isRegularFile(
      root.resolve("androidide-project.properties")
    ))
    assertTrue(Files.isRegularFile(
      root.resolve("src/main/AndroidManifest.xml")
    ))
    assertTrue(Files.isRegularFile(
      root.resolve("src/main/cpp/main.cpp")
    ))
    assertTrue(
      Files.readString(root.resolve("src/main/AndroidManifest.xml"))
        .contains("android.app.NativeActivity")
    )
  }

  @Test
  fun createsKotlinCppHybridProjectWithJavaBridge() {
    val root = Files.createTempDirectory("androidide-kotlin-cpp")
    val sdk = Files.createTempDirectory("androidide-sdk")

    NativeProjectScaffolder.create(
      NativeProjectScaffoldRequest(
        rootDir = root,
        packageName = "com.example.hybrid",
        appName = "Hybrid",
        sdkRoot = sdk,
        compileSdk = 35,
        kind = NativeProjectKind.KOTLIN_CPP
      )
    )

    assertTrue(Files.isRegularFile(
      root.resolve("src/main/kotlin/com/example/hybrid/MainActivity.kt")
    ))
    assertTrue(Files.isRegularFile(
      root.resolve("src/main/java/com/example/hybrid/NativeBridge.java")
    ))
    assertTrue(Files.isRegularFile(
      root.resolve("src/main/cpp/main.cpp")
    ))

    val cpp = Files.readString(root.resolve("src/main/cpp/main.cpp"))
    assertTrue(cpp.contains("NativeBridge_nativeValue"))
  }
}
