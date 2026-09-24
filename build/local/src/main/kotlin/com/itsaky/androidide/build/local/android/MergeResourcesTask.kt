/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local.android

import com.itsaky.androidide.build.api.Artifact
import com.itsaky.androidide.build.api.BuildDiagnostic
import com.itsaky.androidide.build.api.BuildTask
import com.itsaky.androidide.build.api.TaskContext
import com.itsaky.androidide.build.api.TaskResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Merges Android resource roots in ascending priority.
 *
 * File resources overlay by relative resource path. `values*` files are merged by resource key so a higher
 * priority source overrides an earlier declaration without producing duplicate values resources.
 */
class MergeResourcesTask(
  override val id: String,
  private val resDirs: List<Path>,
  private val outputDir: Path,
  override val dependencies: Set<String> = emptySet(),
) : BuildTask {

  override val inputs: Set<Artifact> = resDirs.map(::Artifact).toSet()
  override val outputs: Set<Artifact> = setOf(Artifact(outputDir))

  override fun execute(context: TaskContext): TaskResult {
    if (context.cancellation.isCancellationRequested()) {
      return TaskResult(id, TaskResult.State.CANCELLED, message = "Resource merge cancelled")
    }

    return try {
      deleteTree(outputDir)
      Files.createDirectories(outputDir)

      val values = ValuesAccumulator()
      val owners = HashMap<String, Path>()
      resDirs.filter(Files::isDirectory).forEach { resDir ->
        Files.walk(resDir).use { stream ->
          stream.filter(Files::isRegularFile).sorted().forEach { source ->
            val relative = resDir.relativize(source)
            if (relative.nameCount == 0) return@forEach
            val folder = relative.getName(0).toString()
            if (folder.startsWith("values")) {
              if (!values.add(folder, source)) copyResource(source, outputDir.resolve(relative))
            } else {
              val identity = folder + "/" + resourceFileName(relative.fileName.toString())
              owners[identity]?.let { previous -> Files.deleteIfExists(previous) }
              val destination = outputDir.resolve(relative)
              copyResource(source, destination)
              owners[identity] = destination
            }
          }
        }
      }
      values.writeTo(outputDir)

      TaskResult(id, TaskResult.State.SUCCESS, outputs = outputs, message = "Resources merged")
    } catch (error: Throwable) {
      val diagnostic = BuildDiagnostic(
        severity = BuildDiagnostic.Severity.ERROR,
        kind = BuildDiagnostic.Kind.RESOURCE,
        message = error.message ?: error::class.java.simpleName,
        source = context.projectDir,
        task = id,
        detail = error.stackTraceToString(),
      )
      context.diagnostics.report(diagnostic)
      TaskResult(id, TaskResult.State.FAILED, diagnostics = listOf(diagnostic), message = "Resource merge failed")
    }
  }

  private fun copyResource(source: Path, destination: Path) {
    destination.parent?.let(Files::createDirectories)
    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
  }

  private fun deleteTree(path: Path) {
    if (!Files.exists(path)) return
    Files.walk(path).use { stream ->
      stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
  }

  private class ValuesAccumulator {
    private val byQualifier = LinkedHashMap<String, LinkedHashMap<String, org.w3c.dom.Element>>()
    private val namespaces = LinkedHashMap<String, String>()

    fun add(qualifier: String, file: Path): Boolean = runCatching {
      val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(file.toFile())
      val root = document.documentElement ?: return false
      if (root.tagName != "resources") return false
      val attrs = root.attributes
      for (i in 0 until attrs.length) {
        val attr = attrs.item(i)
        if (attr.nodeName.startsWith("xmlns")) namespaces.putIfAbsent(attr.nodeName, attr.nodeValue)
      }
      val bucket = byQualifier.getOrPut(qualifier) { LinkedHashMap() }
      val children = root.childNodes
      for (i in 0 until children.length) {
        val node = children.item(i)
        if (node !is org.w3c.dom.Element) continue
        val name = node.getAttribute("name")
        val type = node.getAttribute("type")
        val key = if (name.isEmpty()) "${node.tagName}#${bucket.size}" else "${node.tagName}|$type|$name"
        bucket[key] = node.cloneNode(true) as org.w3c.dom.Element
      }
      true
    }.getOrDefault(false)

    fun writeTo(outDir: Path) {
      if (byQualifier.isEmpty()) return
      for ((qualifier, entries) in byQualifier) {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = document.createElement("resources")
        namespaces.forEach { (name, value) -> root.setAttribute(name, value) }
        entries.values.forEach { root.appendChild(document.importNode(it, true)) }
        document.appendChild(root)
        val destination = outDir.resolve(qualifier).resolve("values.xml")
        Files.createDirectories(destination.parent)
        Files.write(destination, XmlDomWriter.toXml(document).toByteArray(Charsets.UTF_8))
      }
    }
  }

  private fun resourceFileName(name: String): String {
    val ninePatch = ".9.png"
    if (name.endsWith(ninePatch, ignoreCase = true)) return name.removeSuffix(ninePatch)
    return name.substringBeforeLast('.', name)
  }
}
