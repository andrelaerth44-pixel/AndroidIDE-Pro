plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  api(projects.build.api)
  implementation(libs.composite.jdt)
  testImplementation(libs.tests.junit)
}
