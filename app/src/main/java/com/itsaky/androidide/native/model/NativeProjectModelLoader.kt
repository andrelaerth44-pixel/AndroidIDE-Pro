package com.itsaky.androidide.native.model

import com.itsaky.androidide.native.build.NativeSourceScanner
import java.io.File

object NativeProjectModelLoader {

  fun load(
    moduleRoot: File,
    abi: AbiTarget = AbiTarget.ARM64_V8A,
    variant: BuildVariant = BuildVariant.DEBUG,
  ): NativeModule? {
    require(moduleRoot.exists() && moduleRoot.isDirectory) {
      "Native module root does not exist: " + moduleRoot.absolutePath
    }

    val sourceRoot = File(moduleRoot, "src/main")
    if (!sourceRoot.isDirectory) {
      return null
    }

    val sourceSet = NativeSourceScanner.scan(sourceRoot)
    if (sourceSet.cSources.isEmpty() && sourceSet.cppSources.isEmpty()) {
      return null
    }

    val config = NativeProjectConfigStore.load(moduleRoot)
    val target =
      NativeTarget(
        name = moduleRoot.name,
        abi = abi,
        variant = variant,
        libraryType = NativeProjectConfigStore.load(moduleRoot)?.libraryType
          ?: NativeLibraryType.SHARED,
        sourceSet = sourceSet,
      )

    return NativeModule(
      moduleName = moduleRoot.name,
      targets = listOf(target),
      androidApiLevel = config?.androidApiLevel ?: 28,
    )
  }
}
