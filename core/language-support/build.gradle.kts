plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

kotlin {
  jvmToolchain(17)
}

description = "Built-in language support for AndroidIDE Pro"

dependencies {
  testImplementation(libs.tests.junit)
}
