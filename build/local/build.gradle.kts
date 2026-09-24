plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  api(projects.build.api)
  testImplementation(libs.tests.junit)
}
