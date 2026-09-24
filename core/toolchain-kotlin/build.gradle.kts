plugins {
  id("com.android.application")
}

val composeCompilerToolchain by configurations.creating

android {
  namespace = "com.itsaky.androidide.toolchain.kotlin"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.itsaky.androidide.toolchain.kotlin"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.9.24.1"
    multiDexEnabled = true
  }

  buildFeatures {
    buildConfig = false
  }

  buildTypes {
    getByName("release") {
      signingConfig = signingConfigs.getByName("debug")
      isMinifyEnabled = false
    }
  }
}

dependencies {
  implementation(libs.kotlin.compiler.embeddable)
  composeCompilerToolchain(libs.compose.compiler.hosted)
}

val generatedAssetsDir = layout.buildDirectory.dir("generated/toolchain/assets")

val packageComposeCompiler = tasks.register<Copy>("packageComposeCompiler") {
  from(composeCompilerToolchain)
  into(generatedAssetsDir)
  rename { "compose-compiler-hosted.jar" }
}

android.sourceSets["main"].assets.srcDir(generatedAssetsDir)

tasks.named("preBuild") {
  dependsOn(packageComposeCompiler)
}


description = "AndroidIDE Pro first-party Kotlin compiler runtime"
