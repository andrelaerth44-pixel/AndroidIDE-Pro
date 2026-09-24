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
  val apiVersion: Int,
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


class PluginRegistry {
  private val plugins = linkedMapOf<String, IdePlugin>()

  fun register(plugin: IdePlugin): Boolean {
    val id = plugin.descriptor.id.value
    if (plugins.containsKey(id)) return false
    plugins[id] = plugin
    return true
  }

  fun unregister(pluginId: PluginId): IdePlugin? =
    plugins.remove(pluginId.value)

  fun find(pluginId: PluginId): IdePlugin? =
    plugins[pluginId.value]

  fun all(): List<IdePlugin> =
    plugins.values.toList()
}
