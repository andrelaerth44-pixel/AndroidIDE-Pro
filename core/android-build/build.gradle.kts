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
  implementation(libs.composite.javac)
  testImplementation(libs.tests.junit)
}

description = "Native Android APK build pipeline"
