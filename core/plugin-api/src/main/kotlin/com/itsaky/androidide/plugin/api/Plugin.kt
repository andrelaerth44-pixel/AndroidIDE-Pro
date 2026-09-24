package com.itsaky.androidide.plugin.api

data class PluginId(
  val value: String
) {
  init {
    require(value.matches(Regex("[a-zA-Z0-9_.-]+")))
  }
}

data class PluginDescriptor(
  val id: PluginId,
  val version: String,
  val displayName: String,
  val capabilities: Set<String>,
  val permissions: Set<String>,
  val dependencies: Set<PluginId> = emptySet()
)

interface IdePlugin {
  val descriptor: PluginDescriptor

  fun onLoad(context: PluginContext)

  fun onUnload(context: PluginContext)
}

interface PluginContext {
  fun registerCapability(
    capability: String,
    provider: Any
  )
}
