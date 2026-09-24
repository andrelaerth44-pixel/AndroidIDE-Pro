plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  api(projects.core.buildApi)
}

description = "Gradle-free build task engine"
