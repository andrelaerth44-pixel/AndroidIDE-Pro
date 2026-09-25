package com.itsaky.androidide.native.model

data class NativeTarget(
    val name: String,
    val abi: AbiTarget,
    val variant: BuildVariant,
    val libraryType: NativeLibraryType,
    val sourceSet: NativeSourceSet,
)
