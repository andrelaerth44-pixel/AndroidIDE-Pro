plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

description = "AndroidIDE Pro core toolchain contracts"

dependencies {
  testImplementation(libs.tests.junit)
}
