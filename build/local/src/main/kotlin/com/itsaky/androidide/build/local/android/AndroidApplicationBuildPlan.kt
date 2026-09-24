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
import com.itsaky.androidide.build.api.Artifact
import com.itsaky.androidide.build.api.BuildGraph
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult
import com.itsaky.androidide.build.local.java.JavaCompileTask
import java.nio.file.Files
import java.nio.file.Path

data class ApkSigningInputs(
  val keystore: Path,
  val storePassword: String,
  val keyAlias: String,
  val keyPassword: String,
)

/**
 * Native Android application DAG.
 *
 * The graph follows the same major lifecycle as the reference pipeline:
 * resources -> AAPT2 -> R/Java -> dexBuilder -> dex merge -> package -> optional sign -> assemble.
 */
data class AndroidApplicationBuildInputs(
  val variant: String,
  val resDirs: List<Path>,
  val javaSourceRoots: List<Path>,
  val javaClasspath: List<Path> = emptyList(),
  val manifest: Path,
  val androidJar: Path,
  val packageName: String,
  val minSdk: Int,
  val targetSdk: Int,
  val buildDir: Path,
  val javacBootClasspath: List<Path> = emptyList(),
  val javaSourceLevel: String = "17",
  val javaTargetLevel: String = javaSourceLevel,
  val dexer: Dexer? = null,
  val dexThreads: Int = 1,
  val signing: ApkSigningInputs? = null,
  val extraResourcePackages: List<String> = emptyList(),
)

class AndroidApplicationBuildPlan(
  private val input: AndroidApplicationBuildInputs,
  private val aapt2: Aapt2Tool,
  private val apkSigner: ApkSigner? = null,
) {

  fun graph(): BuildGraph {
    val variant = input.variant.replaceFirstChar(Char::uppercase)
    val mergedRes = input.buildDir.resolve("intermediates/merged-res/${input.variant}")
    val compiledRes = input.buildDir.resolve("intermediates/aapt2/compile/${input.variant}")
    val generatedR = input.buildDir.resolve("generated/source/r/${input.variant}")
    val resourcesAp = input.buildDir.resolve("intermediates/resources/${input.variant}/resources.ap_")
    val javaOutput = input.buildDir.resolve("intermediates/javac/${input.variant}")
    val dexArchives = input.buildDir.resolve("intermediates/dex/${input.variant}/archives")
    val mergedDex = input.buildDir.resolve("intermediates/dex/${input.variant}/merged")
    val unsignedApk = input.buildDir.resolve("outputs/apk/${input.variant}/app-unsigned.apk")
    val signedApk = input.buildDir.resolve("outputs/apk/${input.variant}/app.apk")

    val mergeId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.MERGE_RESOURCES, input.variant)
    val compileId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.AAPT2_COMPILE, input.variant)
    val linkId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.AAPT2_LINK, input.variant)
    val javaId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.COMPILE_JAVA, input.variant)
    val dexBuilderId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.DEX_BUILDER, input.variant)
    val dexMergeId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.MERGE_PROJECT_DEX, input.variant)
    val packageId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.PACKAGE_APK, input.variant)
    val signId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.SIGN, input.variant)
    val assembleId = AndroidBuildTaskNames.forVariant(AndroidBuildTaskNames.ASSEMBLE, input.variant)

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
      extraPackages = input.extraResourcePackages,
      dependencies = setOf(compileId),
    )
    val java = JavaCompileTask(
      id = javaId,
      sourceRoots = input.javaSourceRoots + generatedR,
      classpath = input.javaClasspath,
      outputDir = javaOutput,
      bootClasspath = input.javacBootClasspath,
      sourceLevel = input.javaSourceLevel,
      targetLevel = input.javaTargetLevel,
      dependencies = setOf(linkId),
    )

    val taskList = ArrayList<BuildTask>()
    taskList += merge
    taskList += compile
    taskList += link
    taskList += java

    val dexer = input.dexer
    if (dexer != null) {
      val dexBuilder = DexBuilderTask(
        id = dexBuilderId,
        classInputs = listOf(javaOutput),
        classpath = emptyList(),
        androidJar = input.androidJar,
        minSdk = input.minSdk,
        outputDir = dexArchives,
        dexer = dexer,
        threads = input.dexThreads,
        dependencies = setOf(javaId),
      )
      val dexMerge = DexMergeTask(
        id = dexMergeId,
        dexInputs = listOf(dexArchives),
        androidJar = input.androidJar,
        minSdk = input.minSdk,
        outputDir = mergedDex,
        dexer = dexer,
        threads = input.dexThreads,
        dependencies = setOf(dexBuilderId),
      )
      val packageApk = PackageApkTask(
        id = packageId,
        request = ApkPackageRequest(
          resourcesAp = resourcesAp,
          dexDirs = listOf(mergedDex),
          outputApk = unsignedApk,
        ),
        dependencies = setOf(linkId, dexMergeId),
      )
      taskList += dexBuilder
      taskList += dexMerge
      taskList += packageApk

      if (apkSigner != null && input.signing != null) {
        val sign = SignApkTask(
          id = signId,
          request = ApkSigningRequest(
            inputApk = unsignedApk,
            signedApk = signedApk,
            keystore = input.signing.keystore,
            storePassword = input.signing.storePassword,
            keyAlias = input.signing.keyAlias,
            keyPassword = input.signing.keyPassword,
          ),
          signer = apkSigner,
          dependencies = setOf(packageId),
        )
        taskList += sign
        taskList += AssembleApkTask(assembleId, signedApk, setOf(signId))
      }
    }

    return BuildGraph(taskList)
  }
}

/** Lifecycle task marking the signed APK as the final assemble artifact. */
class AssembleApkTask(
  override val id: String,
  private val signedApk: Path,
  override val dependencies: Set<String>,
) : BuildTask {
  override val inputs = setOf(Artifact(signedApk))
  override val outputs = setOf(Artifact(signedApk))

  override fun execute(context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return TaskResult(id, TaskResult.State.CANCELLED, message = "Assemble cancelled")
    }
    return if (Files.isRegularFile(signedApk)) {
      TaskResult(id, TaskResult.State.SUCCESS, outputs = outputs, message = "APK assembled")
    } else {
      TaskResult(id, TaskResult.State.FAILED, message = "Signed APK was not produced")
    }
  }
}
