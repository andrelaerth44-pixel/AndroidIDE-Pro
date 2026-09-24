plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  api(projects.core.buildApi)
  testImplementation(libs.tests.junit)
}

description = "Gradle-free build task engine"
