/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.services.builder

import com.itsaky.androidide.build.api.BuildModule
import com.itsaky.androidide.build.api.BuildModuleType
import com.itsaky.androidide.build.api.BuildProject
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.local.AndroidApplicationInputsProvider
import com.itsaky.androidide.build.local.android.AndroidApplicationBuildInputs
import com.itsaky.androidide.build.local.android.NativeAndroidToolchain
import com.itsaky.androidide.projects.IWorkspace
import com.itsaky.androidide.projects.android.AndroidModule
import java.io.File
import java.nio.file.Path

internal object WorkspaceNativeAndroidAdapter {

  fun application(workspace: IWorkspace): AndroidModule? {
    val apps = workspace.androidProjects().filter { it.isApplication }.toList()
    if (apps.size != 1) return null
    val module = apps.single()
    if (module.getCompileModuleProjects().isNotEmpty()) return null
    if (module.libraries.isNotEmpty()) return null
    if (workspace.getSubProjects().count { it is com.itsaky.androidide.projects.ModuleProject } != 1) return null
    return module
  }

  fun buildProject(workspace: IWorkspace, module: AndroidModule): BuildProject =
    BuildProject(
      rootDir = workspace.getProjectDir().toPath(),
      modules = listOf(
        BuildModule(
          id = module.path,
          projectDir = module.projectDir.toPath(),
          type = if (module.isApplication) BuildModuleType.ANDROID_APPLICATION else BuildModuleType.ANDROID_LIBRARY,
        )
      ),
    )

  fun inputsProvider(module: AndroidModule): AndroidApplicationInputsProvider =
    AndroidApplicationInputsProvider { _, request, toolchain ->
      createInputs(module, request, toolchain)
    }

  private fun createInputs(
    module: AndroidModule,
    request: BuildRequest,
    toolchain: NativeAndroidToolchain,
  ): AndroidApplicationBuildInputs {
    val variantName = resolveVariant(module, request)
    val variant = module.getVariant(variantName)
      ?: error("Native backend could not resolve variant '$variantName' for ${module.path}")
    val artifact = variant.mainArtifact
    val sourceProvider = module.mainSourceSet?.sourceProvider
      ?: error("Native backend requires a main source set for ${module.path}")
    val manifest = sourceProvider.manifestFile
    require(manifest.isFile) { "Native backend requires an AndroidManifest.xml for ${module.path}" }

    val sourceRoots = buildList {
      addAll(sourceProvider.javaDirectories ?: emptyList())
      addAll(artifact.generatedSourceFolders)
    }.filter(File::isDirectory).distinct().map(File::toPath)

    val resourceRoots = buildList {
      addAll(sourceProvider.resDirectories ?: emptyList())
      addAll(artifact.generatedResourceFolders)
    }.filter(File::isDirectory).distinct().map(File::toPath)

    val javaClasspath = module.getCompileClasspaths()
      .filter(File::isFile)
      .distinct()
      .map(File::toPath)

    val bootClasspath = module.bootClassPaths
      .filter(File::isFile)
      .map(File::toPath)
    val androidJar = bootClasspath.firstOrNull { it.fileName.toString() == "android.jar" }
      ?: toolchain.androidJar

    val packageName = module.namespace ?: artifact.applicationId
      ?: error("Native backend requires namespace/applicationId for ${module.path}")

    return AndroidApplicationBuildInputs(
      variant = variantName,
      resDirs = resourceRoots,
      javaSourceRoots = sourceRoots,
      javaClasspath = javaClasspath,
      manifest = manifest.toPath(),
      androidJar = androidJar,
      packageName = packageName,
      minSdk = artifact.minSdkVersion,
      targetSdk = artifact.targetSdkVersionOverride.takeIf { it > 0 } ?: artifact.minSdkVersion,
      buildDir = module.buildDir.toPath(),
      javacBootClasspath = bootClasspath.ifEmpty { listOf(androidJar) },
      javaSourceLevel = request.parameters["java.source"] ?: "17",
      javaTargetLevel = request.parameters["java.target"] ?: request.parameters["java.source"] ?: "17",
      signing = toolchain.debugSigning,
      extraResourcePackages = emptyList(),
    )
  }

  private fun resolveVariant(module: AndroidModule, request: BuildRequest): String {
    request.parameters["android.variant"]?.takeIf(String::isNotBlank)?.let { return it }
    request.variant?.takeIf(String::isNotBlank)?.let { return it }
    request.requestedTasks.firstOrNull()?.substringAfterLast(':')?.removePrefix("assemble")
      ?.takeIf(String::isNotBlank)?.let { suffix ->
        return suffix.replaceFirstChar(Char::lowercaseChar)
      }
    return module.getSelectedVariant()?.name ?: "debug"
  }
}
