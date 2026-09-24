package com.itsaky.androidide.build.api

import java.nio.file.Path

data class BuildRequest(
  val moduleName: String,
  val variant: String = "debug"
)

data class BuildResult(
  val success: Boolean,
  val outputApk: Path? = null,
  val message: String = "",
  val applicationId: String? = null
)

data class TaskResult(
  val success: Boolean,
  val message: String = ""
)

interface BuildTask {
  val id: String
  val inputs: List<Path>
  val outputs: List<Path>
  fun execute(context: BuildContext): TaskResult
}

interface BuildContext {
  val cacheRoot: Path?
    get() = null

  fun log(message: String)
  fun fail(message: String): Nothing {
    throw BuildFailureException(message)
  }
}

class BuildFailureException(
  message: String,
  cause: Throwable? = null
) : RuntimeException(message, cause)

interface BuildSystem {
  fun assemble(request: BuildRequest): BuildResult
}


data class KotlinCompileContext(
  val moduleId: String,
  val classpath: List<Path> = emptyList(),
  val properties: Map<String, String> = emptyMap()
)

data class KotlinCompilerPluginOptions(
  val classpath: List<Path> = emptyList(),
  val options: Map<String, String> = emptyMap()
)

interface KotlinCompilerPlugin {
  val id: String

  fun appliesTo(context: KotlinCompileContext): Boolean

  fun options(context: KotlinCompileContext): KotlinCompilerPluginOptions
}
