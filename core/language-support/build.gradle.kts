plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  api(projects.core.languageApi)
  testImplementation(libs.tests.junit)
}

description = "Built-in language support for AndroidIDE Pro"
