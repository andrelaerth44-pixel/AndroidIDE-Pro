/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.services.builder

import com.itsaky.androidide.projects.android.AndroidModule
import com.itsaky.androidide.utils.Environment
import com.itsaky.androidide.build.local.android.ApkSigningInputs
import com.itsaky.androidide.build.local.android.NativeAndroidToolchain
import java.io.File
import java.util.Comparator

internal object NativeAndroidToolchainFactory {

  fun create(module: AndroidModule): NativeAndroidToolchain? {
    val androidJar = module.bootClassPaths
      .firstOrNull { it.name == "android.jar" && it.isFile }
      ?: return null

    val buildTools = resolveBuildTools() ?: return null
    val zipalign = File(buildTools, "zipalign")
    val apksigner = File(buildTools, "apksigner")
    if (!zipalign.isFile || !apksigner.isFile) return null

    val debugSigning = ensureDebugSigning() ?: return null
    return NativeAndroidToolchain(
      aapt2 = Environment.AAPT2.toPath(),
      androidJar = androidJar.toPath(),
      zipalign = zipalign.toPath(),
      apksigner = apksigner.toPath(),
      debugSigning = debugSigning,
    ).takeIf { it.canSignDebug() }
  }

  private fun resolveBuildTools(): File? {
    val root = File(Environment.ANDROID_HOME, "build-tools")
    if (!root.isDirectory) return null
    return root.listFiles()
      ?.filter { it.isDirectory }
      ?.maxWithOrNull(Comparator { a, b -> compareVersions(a.name, b.name) })
  }

  private fun compareVersions(first: String, second: String): Int {
    val a = first.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
    val b = second.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
    val size = maxOf(a.size, b.size)
    for (i in 0 until size) {
      val left = a.getOrElse(i) { 0 }
      val right = b.getOrElse(i) { 0 }
      if (left != right) return left.compareTo(right)
    }
    return first.compareTo(second)
  }

  private fun ensureDebugSigning(): ApkSigningInputs? {
    val home = Environment.HOME ?: return null
    val keyDir = File(home, ".android")
    if (!keyDir.exists() && !keyDir.mkdirs()) return null
    val keystore = File(keyDir, "debug.keystore")
    if (!keystore.isFile) {
      val keytool = File(Environment.JAVA_HOME, "bin/keytool")
      if (!keytool.isFile) return null

      val command = listOf(
        keytool.absolutePath,
        "-genkeypair",
        "-v",
        "-keystore", keystore.absolutePath,
        "-storepass", DEBUG_PASSWORD,
        "-alias", DEBUG_ALIAS,
        "-keypass", DEBUG_PASSWORD,
        "-dname", "CN=Android Debug,O=Android,C=US",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
      )
      val process = runCatching { ProcessBuilder(command).redirectErrorStream(true).start() }.getOrNull() ?: return null
      if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS) || process.exitValue() != 0) {
        process.destroyForcibly()
        return null
      }
    }

    return ApkSigningInputs(
      keystore = keystore.toPath(),
      storePassword = DEBUG_PASSWORD,
      keyAlias = DEBUG_ALIAS,
      keyPassword = DEBUG_PASSWORD,
    )
  }

  private const val DEBUG_ALIAS = "androiddebugkey"
  private const val DEBUG_PASSWORD = "android"
}
