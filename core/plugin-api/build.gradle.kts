plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

testImplementation(libs.tests.junit)

description = "AndroidIDE Pro plugin contracts"
