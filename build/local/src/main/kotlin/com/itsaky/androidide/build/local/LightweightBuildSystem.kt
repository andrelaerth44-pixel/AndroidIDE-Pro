/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local

import com.itsaky.androidide.build.api.BuildContext
import com.itsaky.androidide.build.api.BuildGraph
import com.itsaky.androidide.build.api.BuildModuleType
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.api.BuildSystem
import com.itsaky.androidide.build.api.TaskDescriptor
import com.itsaky.androidide.build.local.android.AndroidApplicationBuildPlan
import com.itsaky.androidide.build.local.android.ConventionalAndroidApplicationAdapter
import com.itsaky.androidide.build.local.android.ApkSignerSubprocess
import com.itsaky.androidide.build.local.android.D8InProcessDexer
import com.itsaky.androidide.build.local.android.NativeAndroidToolchain

/**
 * Native build backend for operations already covered by the lightweight engine.
 *
 * Clean works without any toolchain. Android assemble works only for the controlled conventional Android
 * layout when an explicit native toolchain is available. Unsupported projects are left to the Gradle adapter.
 */
class LightweightBuildSystem(
  private val androidToolchain: NativeAndroidToolchain? = null,
) : BuildSystem {

  override val id: String = "local"

  override fun supports(moduleType: BuildModuleType): Boolean = true

  override fun supports(
    project: BuildProject,
    request: BuildRequest,
    context: BuildContext?,
  ): Boolean {
    if (request.clean || request.requestedTasks.isEmpty() && request.clean) return true
    if (request.requestedTasks == setOf("clean")) return true

    val task = request.requestedTasks.singleOrNull() ?: return false
    if (!task.startsWith("assemble") &&
      !task.startsWith("packageApk") &&
      !task.startsWith("dexBuilder") &&
      !task.startsWith("mergeProjectDex") &&
      !task.startsWith("compileJava") &&
      !task.startsWith("aapt2") &&
      !task.startsWith("mergeResources")
    ) return false

    val module = project.modules.singleOrNull { it.type == BuildModuleType.ANDROID_APPLICATION } ?: return false
    if (androidToolchain?.canCompile() != true) return false
    if (task.startsWith("assemble")) return androidToolchain.canSignDebug()
    return module.projectDir.resolve("src/main/AndroidManifest.xml").toFile().isFile
  }

  override fun createBuildGraph(
    project: BuildProject,
    request: BuildRequest,
    context: BuildContext,
  ): BuildGraph {
    if (request.clean || request.requestedTasks.contains("clean")) {
      return BuildGraph(listOf(CleanProjectTask(project)))
    }

    val toolchain = requireNotNull(androidToolchain) { "Native Android toolchain is not configured" }
    val module = project.modules.singleOrNull { it.type == BuildModuleType.ANDROID_APPLICATION }
      ?: error("Native Android backend currently requires exactly one Android application module")
    val inputs = ConventionalAndroidApplicationAdapter.createInputs(module, request, toolchain)
    val signer = if (toolchain.canSignDebug()) {
      ApkSignerSubprocess(
        zipalign = requireNotNull(toolchain.zipalign),
        apksigner = requireNotNull(toolchain.apksigner),
      )
    } else null

    return AndroidApplicationBuildPlan(
      input = inputs.copy(
        dexer = D8InProcessDexer(),
        signing = toolchain.debugSigning,
      ),
      aapt2 = com.itsaky.androidide.build.local.android.Aapt2Subprocess(toolchain.aapt2),
      apkSigner = signer,
    ).graph()
  }

  override fun tasks(project: BuildProject): Collection<TaskDescriptor> = listOf(
    TaskDescriptor("clean", "Remove generated build outputs without starting Gradle"),
    TaskDescriptor("mergeResources", "Merge Android resource source sets"),
    TaskDescriptor("aapt2Compile", "Compile merged Android resources"),
    TaskDescriptor("aapt2Link", "Link Android resources and generate R"),
    TaskDescriptor("compileJava", "Compile Java without Gradle"),
    TaskDescriptor("dexBuilder", "Archive compiled classes into DEX"),
    TaskDescriptor("mergeProjectDex", "Merge DEX archives"),
    TaskDescriptor("packageApk", "Package an unsigned APK"),
    TaskDescriptor("sign", "Align and sign an APK"),
    TaskDescriptor("assemble", "Produce the final APK"),
  )
}
