plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  api(projects.build.api)
  api(projects.tooling.model)
  testImplementation(libs.tests.junit)
}
