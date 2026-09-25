package com.itsaky.androidide.native.model

import com.itsaky.androidide.native.build.NativeSourceScanner
import com.itsaky.androidide.utils.NativeLibraryNaming
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
    val libraryName = NativeLibraryNaming.sanitize(moduleRoot.name)
    val target =
      NativeTarget(
        name = libraryName,
        abi = abi,
        variant = variant,
        libraryType = config?.libraryType ?: NativeLibraryType.SHARED,
        sourceSet = sourceSet,
      )

    return NativeModule(
      moduleName = libraryName,
      targets = listOf(target),
      androidApiLevel = config?.androidApiLevel ?: 28,
    )
  }
}
