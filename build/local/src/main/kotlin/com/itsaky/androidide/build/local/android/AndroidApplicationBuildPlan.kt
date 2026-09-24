/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import com.itsaky.androidide.build.api.AndroidBuildTaskNames
import com.itsaky.androidide.build.api.BuildGraph
import com.itsaky.androidide.build.local.java.JavaCompileTask
import java.nio.file.Path

/**
 * Partial native Android application graph.
 *
 * This is intentionally smaller than the final APK graph. It wires the first real resource + Java stages
 * together so the engine can validate a Hello World without involving Gradle.
 */
data class AndroidApplicationBuildInputs(
  val variant: String,
  val resDirs: List<Path>,
  val javaSourceRoots: List<Path>,
  val manifest: Path,
  val androidJar: Path,
  val packageName: String,
  val minSdk: Int,
  val targetSdk: Int,
  val buildDir: Path,
  val javacBootClasspath: List<Path> = emptyList(),
  val javaSourceLevel: String = "17",
  val javaTargetLevel: String = javaSourceLevel,
)

class AndroidApplicationBuildPlan(
  private val input: AndroidApplicationBuildInputs,
  private val aapt2: Aapt2Tool,
) {

  fun graph(): BuildGraph {
    val variant = input.variant.replaceFirstChar(Char::uppercase)
    val mergedRes = input.buildDir.resolve("intermediates/merged-res/${input.variant}")
    val compiledRes = input.buildDir.resolve("intermediates/aapt2/compile/${input.variant}")
    val generatedR = input.buildDir.resolve("generated/source/r/${input.variant}")
    val resourcesAp = input.buildDir.resolve("intermediates/resources/${input.variant}/resources.ap_")
    val javaOutput = input.buildDir.resolve("intermediates/javac/${input.variant}")

    val mergeId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.MERGE_RESOURCES, input.variant)
    val compileId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.AAPT2_COMPILE, input.variant)
    val linkId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.AAPT2_LINK, input.variant)
    val javaId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.COMPILE_JAVA, input.variant)

    val merge = MergeResourcesTask(
      id = mergeId,
      resDirs = input.resDirs,
      outputDir = mergedRes,
    )
    val compile = Aapt2CompileTask(
      id = compileId,
      mergedResources = mergedRes,
      outputDir = compiledRes,
      aapt2 = aapt2,
      dependencies = setOf(mergeId),
    )
    val link = Aapt2LinkTask(
      id = linkId,
      compiledResources = compiledRes,
      manifest = input.manifest,
      androidJar = input.androidJar,
      packageName = input.packageName,
      minSdk = input.minSdk,
      targetSdk = input.targetSdk,
      generatedRDir = generatedR,
      resourcesAp = resourcesAp,
      aapt2 = aapt2,
      dependencies = setOf(compileId),
    )
    val java = JavaCompileTask(
      id = javaId,
      sourceRoots = input.javaSourceRoots + generatedR,
      classpath = emptyList(),
      outputDir = javaOutput,
      bootClasspath = input.javacBootClasspath,
      sourceLevel = input.javaSourceLevel,
      targetLevel = input.javaTargetLevel,
      dependencies = setOf(linkId),
    )

    return BuildGraph(listOf(merge, compile, link, java))
  }
}
