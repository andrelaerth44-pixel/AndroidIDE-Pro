package com.itsaky.androidide.native.build

import com.itsaky.androidide.utils.Environment
import java.io.File

object JniHeaderGenerator {

  fun plan(
    moduleRoot: File,
    buildDirectory: File,
  ): NativeCommandSpec? {
    val javaRoot = File(moduleRoot, "src/main/java")
    if (!javaRoot.isDirectory) {
      return null
    }

    val javaSources =
      javaRoot.walkTopDown()
        .filter { it.isFile && it.extension == "java" }
        .filter { declaresNativeMethod(it) }
        .toList()

    if (javaSources.isEmpty()) {
      return null
    }

    val javac = runCatching { Environment.JAVA }.getOrNull() ?: return null
    if (!javac.isFile) {
      return null
    }

    val headerDirectory = File(buildDirectory, "jni/headers")
    val classesDirectory = File(buildDirectory, "jni/classes")
    headerDirectory.mkdirs()
    classesDirectory.mkdirs()

    val classpath =
      listOfNotNull(
        runCatching { Environment.ANDROID_JAR }.getOrNull()?.takeIf { it.isFile },
      ).joinToString(File.pathSeparator)

    return NativeCommandSpec(
      executable = javac.parentFile?.let { File(it, "javac") } ?: javac,
      arguments =
        buildList {
          add("-h")
          add(headerDirectory.absolutePath)
          add("-d")
          add(classesDirectory.absolutePath)
          add("-proc:none")
          add("-source")
          add("11")
          add("-target")
          add("11")
          if (classpath.isNotBlank()) {
            add("-classpath")
            add(classpath)
          }
          javaSources.sortedBy { it.absolutePath }.forEach {
            add(it.absolutePath)
          }
        },
      workingDirectory = moduleRoot,
    )
  }

  private fun declaresNativeMethod(file: File): Boolean {
    val text = runCatching { file.readText() }.getOrDefault("")
    return Regex("""\\bnative\\s+[\\w<>\\[\\], ?]+\\s+\\w+\\s*\\(""").containsMatchIn(text)
  }
}
