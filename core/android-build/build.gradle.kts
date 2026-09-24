plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  api(projects.core.buildApi)
  implementation(projects.core.buildEngine)
}

description = "Native Android APK build pipeline"
