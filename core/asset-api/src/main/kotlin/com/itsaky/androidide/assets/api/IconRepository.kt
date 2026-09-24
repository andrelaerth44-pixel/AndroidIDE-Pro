package com.itsaky.androidide.assets.api

data class IconQuery(
  val text: String,
  val limit: Int = 50,
  val tags: Set<String> = emptySet()
)

enum class AssetKind {
  ICON,
  ILLUSTRATION,
  LOTTIE,
  EMOJI,
  STICKER,
  SPLASH
}

enum class IconFormat {
  SVG,
  VECTOR_DRAWABLE,
  ANDROID_XML,
  PNG,
  WEBP,
  LOTTIE
}

data class IconAsset(
  val id: String,
  val name: String,
  val repositoryId: String,
  val kind: AssetKind = AssetKind.ICON,
  val formats: Set<IconFormat>,
  val tags: Set<String> = emptySet(),
  val licenseSpdx: String? = null,
  val licenseUrl: String? = null
)

interface IconRepository {
  val id: String
  val displayName: String

  suspend fun search(query: IconQuery): List<IconAsset>

  suspend fun load(asset: IconAsset, format: IconFormat): ByteArray

  fun supports(format: IconFormat): Boolean
}
