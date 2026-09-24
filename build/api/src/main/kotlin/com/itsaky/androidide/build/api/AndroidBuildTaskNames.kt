/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.api

/**
 * Stable lifecycle identifiers for the native Android APK pipeline.
 *
 * The names are based on the 17 stages observed in a real CodeAssist Hello World build. They are
 * identifiers only; registering a name here does not imply that the corresponding task is already
 * executable in AndroidIDE Pro.
 */
object AndroidBuildTaskNames {

  const val GENERATE_SOURCES = "generateSources"
  const val MERGE_RESOURCES = "mergeResources"
  const val CHECK_AAR_METADATA = "checkAarMetadata"
  const val MERGE_NATIVE_LIBS = "mergeNativeLibs"
  const val MERGE_JAVA_RESOURCE = "mergeJavaResource"
  const val AAPT2_COMPILE = "aapt2Compile"
  const val PROCESS_MANIFEST = "processManifest"
  const val INJECT_APP_LOG_PROVIDER = "injectAppLogProvider"
  const val AAPT2_LINK = "aapt2Link"
  const val GENERATE_R_FILE = "generateRFile"
  const val COMPILE_JAVA = "compileJava"
  const val DEX_BUILDER = "dexBuilder"
  const val MERGE_PROJECT_DEX = "mergeProjectDex"
  const val MERGE_EXT_DEX = "mergeExtDex"
  const val PACKAGE_APK = "packageApk"
  const val SIGN = "sign"
  const val ASSEMBLE = "assemble"

  /**
   * Observed debug APK lifecycle order, without the module prefix and variant suffix.
   */
  val DEBUG_APK_ORDER: List<String> = listOf(
    GENERATE_SOURCES,
    MERGE_RESOURCES,
    CHECK_AAR_METADATA,
    MERGE_NATIVE_LIBS,
    MERGE_JAVA_RESOURCE,
    AAPT2_COMPILE,
    PROCESS_MANIFEST,
    INJECT_APP_LOG_PROVIDER,
    AAPT2_LINK,
    GENERATE_R_FILE,
    COMPILE_JAVA,
    DEX_BUILDER,
    MERGE_PROJECT_DEX,
    MERGE_EXT_DEX,
    PACKAGE_APK,
    SIGN,
    ASSEMBLE,
  )

  fun forVariant(task: String, variant: String): String {
    require(task.isNotBlank()) { "Task name cannot be blank" }
    require(variant.isNotBlank()) { "Variant cannot be blank" }
    return task + variant.replaceFirstChar(Char::uppercase)
  }

  fun forModule(modulePath: String, task: String, variant: String): String {
    require(modulePath.isNotBlank()) { "Module path cannot be blank" }
    return ":" + modulePath.trimStart(':') + ":" + forVariant(task, variant)
  }
}
