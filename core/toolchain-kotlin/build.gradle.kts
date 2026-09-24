plugins {
  id("com.android.application")
}

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
}

description = "AndroidIDE Pro first-party Kotlin compiler runtime"
