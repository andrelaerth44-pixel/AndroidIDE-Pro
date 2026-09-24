import java.security.MessageDigest
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression

plugins {
  id("com.android.application")
}

val toolchainOut = providers.gradleProperty("llvm.toolchain.dir")
  .map(::file)
  .orElse(layout.projectDirectory.dir("toolchain-out").asFile)

val toolchainAssets = toolchainOut.map { it.resolve("assets/toolchain") }
val generatedAssetsDir = layout.buildDirectory.dir("generated/toolchain/assets")

android {
  namespace = "com.itsaky.androidide.toolchain.llvm"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.itsaky.androidide.toolchain.llvm"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
  }

  buildFeatures {
    buildConfig = false
  }

  buildTypes {
    getByName("release") {
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  sourceSets["main"].assets.srcDir(generatedAssetsDir)

}

val packToolchainAssets = tasks.register<Zip>("packToolchainAssets") {
  from(toolchainAssets)
  archiveFileName.set("toolchain.zip")
  destinationDirectory.set(generatedAssetsDir)
  entryCompression = ZipEntryCompression.STORED
  doFirst {
    check(toolchainAssets.get().isDirectory) {
      "Missing LLVM toolchain assets at " + toolchainAssets.get()
    }
    check(toolchainLibs.get().resolve("arm64-v8a").isDirectory) {
      "Missing arm64-v8a LLVM native libraries at " + toolchainLibs.get()
    }
  }
}

val writeToolchainProperties = tasks.register("writeToolchainProperties") {
  dependsOn(packToolchainAssets)
  val output = generatedAssetsDir.map { it.file("toolchain.properties").asFile }
  outputs.file(output)
  doLast {
    val zip = generatedAssetsDir.get().file("toolchain.zip").asFile
    val digest = MessageDigest.getInstance("SHA-256")
    zip.inputStream().use { input ->
      val buffer = ByteArray(1024 * 64)
      while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
      }
    }
    val version = providers.gradleProperty("llvm.toolchain.version")
      .orElse("18.1.8-r27c")
      .get()
    val llvmMajor = providers.gradleProperty("llvm.major")
      .orElse("18")
      .get()
    output.get().writeText(
      buildString {
        appendLine("version=" + version)
        appendLine("llvmMajor=" + llvmMajor)
        appendLine("archiveSha256=" + digest.digest().joinToString("") { "%02x".format(it) })
        appendLine("hostAbi=arm64-v8a")
      }
    )
  }
}

tasks.named("preBuild") {
  dependsOn(writeToolchainProperties)
}
