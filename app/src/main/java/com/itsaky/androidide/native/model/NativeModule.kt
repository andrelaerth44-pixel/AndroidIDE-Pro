package com.itsaky.androidide.native.model

data class NativeModule(
    val moduleName: String,
    val targets: List<NativeTarget>,
)
