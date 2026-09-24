/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.HashSet
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class ApkPackageRequest(
  val resourcesAp: Path,
  val dexDirs: List<Path>,
  val assetDirs: List<Path> = emptyList(),
  val jniLibDirs: List<Path> = emptyList(),
  val javaResourceJars: List<Path> = emptyList(),
  val outputApk: Path,
)

object ApkPackager {
  fun packageApk(request: ApkPackageRequest) {
    request.outputApk.parent?.let(Files::createDirectories)
    Files.newOutputStream(
      request.outputApk,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.WRITE,
    ).use { output ->
      ZipOutputStream(output).use { zip ->
        val written = HashSet<String>()
        copyZip(request.resourcesAp, zip, written)
        copyDex(request.dexDirs, zip, written)
        copyDirectories(request.assetDirs, "assets", zip, written)
        copyDirectories(request.jniLibDirs, "lib", zip, written)
        request.javaResourceJars.forEach { copyZip(it, zip, written, denyRoots = setOf("META-INF/")) }
      }
    }
  }

  private fun copyZip(path: Path, output: ZipOutputStream, written: MutableSet<String>, denyRoots: Set<String> = emptySet()) {
    ZipFile(path.toFile()).use { zip ->
      zip.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
        if (denyRoots.any(entry.name::startsWith) || !written.add(entry.name)) return@forEach
        val target = ZipEntry(entry.name)
        output.putNextEntry(target)
        zip.getInputStream(entry).use { it.copyTo(output) }
        output.closeEntry()
      }
    }
  }

  private fun copyDex(inputs: List<Path>, output: ZipOutputStream, written: MutableSet<String>) {
    var index = 1
    val dexFiles = inputs.flatMap { input ->
      when {
        Files.isRegularFile(input) && input.toString().endsWith(".dex") -> listOf(input)
        Files.isDirectory(input) -> Files.walk(input).use { stream ->
          stream.filter(Files::isRegularFile).filter { it.fileName.toString().endsWith(".dex") }.sorted().toList()
        }
        else -> emptyList()
      }
    }
    dexFiles.forEach { dex ->
      val name = if (index == 1) "classes.dex" else "classes$index.dex"
      index++
      if (!written.add(name)) return@forEach
      output.putNextEntry(ZipEntry(name))
      Files.newInputStream(dex).use { it.copyTo(output) }
      output.closeEntry()
    }
  }

  private fun copyDirectories(
    roots: List<Path>,
    prefix: String,
    output: ZipOutputStream,
    written: MutableSet<String>,
  ) {
    roots.filter(Files::isDirectory).forEach { root ->
      Files.walk(root).use { stream ->
        stream.filter(Files::isRegularFile).sorted().forEach { file ->
          val relative = root.relativize(file).toString().replace('\\', '/')
          val name = "$prefix/$relative"
          if (!written.add(name)) return@forEach
          output.putNextEntry(ZipEntry(name))
          Files.newInputStream(file).use { it.copyTo(output) }
          output.closeEntry()
        }
      }
    }
  }
}

/** Build task wrapper around [ApkPackager]. */
class PackageApkTask(
  override val id: String,
  private val request: ApkPackageRequest,
  override val dependencies: Set<String> = emptySet(),
) : com.itsaky.androidide.build.api.BuildTask {

  override val inputs: Set<com.itsaky.androidide.build.api.Artifact> =
    (listOf(request.resourcesAp) + request.dexDirs + request.assetDirs + request.jniLibDirs + request.javaResourceJars)
      .map(::com.itsaky.androidide.build.api.Artifact).toSet()

  override val outputs: Set<com.itsaky.androidide.build.api.Artifact> = setOf(
    com.itsaky.androidide.build.api.Artifact(request.outputApk),
  )

  override fun execute(context: com.itsaky.androidide.build.api.TaskContext): com.itsaky.androidide.build.api.TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return com.itsaky.androidide.build.api.TaskResult(id, com.itsaky.androidide.build.api.TaskResult.State.CANCELLED)
    }
    return runCatching {
      ApkPackager.packageApk(request)
      com.itsaky.androidide.build.api.TaskResult(
        taskId = id,
        state = com.itsaky.androidide.build.api.TaskResult.State.SUCCESS,
        outputs = outputs,
        message = "Unsigned APK packaged",
      )
    }.getOrElse { error ->
      val diagnostic = com.itsaky.androidide.build.api.BuildDiagnostic(
        severity = com.itsaky.androidide.build.api.BuildDiagnostic.Severity.ERROR,
        kind = com.itsaky.androidide.build.api.BuildDiagnostic.Kind.PACKAGING,
        message = error.message ?: error::class.java.simpleName,
        source = context.projectDir,
        task = id,
        detail = error.stackTraceToString(),
      )
      context.diagnostics.report(diagnostic)
      com.itsaky.androidide.build.api.TaskResult(
        taskId = id,
        state = com.itsaky.androidide.build.api.TaskResult.State.FAILED,
        diagnostics = listOf(diagnostic),
        message = "APK packaging failed",
      )
    }
  }
}
