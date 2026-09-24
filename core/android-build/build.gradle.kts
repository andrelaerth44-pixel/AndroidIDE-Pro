plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

kotlin {
  jvmToolchain(17)
}

val kotlinToolchain by configurations.creating

dependencies {
  api(projects.core.buildApi)
  implementation(projects.core.buildEngine)
  implementation(projects.core.languageSupport)
  implementation(libs.kotlin.compiler.embeddable)
  implementation(libs.composite.javac)
  kotlinToolchain(libs.kotlin.compiler.embeddable)
}

tasks.register<Sync>("packageKotlinToolchain") {
  from(kotlinToolchain)
  into(layout.buildDirectory.dir("toolchains/kotlin"))
}

description = "Native Android APK build pipeline"
