plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  api(projects.core.buildApi)
  implementation(projects.core.buildEngine)
  implementation(projects.core.languageSupport)
  implementation(libs.kotlin.compiler.embeddable)
}

description = "Native Android APK build pipeline"
