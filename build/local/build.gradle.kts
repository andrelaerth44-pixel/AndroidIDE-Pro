plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  api(projects.build.api)
  implementation(libs.composite.jdt)
  implementation(libs.android.r8)
  testImplementation(libs.tests.junit)
}
