package com.itsaky.androidide.native.model

import java.nio.file.Path

data class NativeSourceSet(
    val cSources: List<Path> = emptyList(),
    val cppSources: List<Path> = emptyList(),
    val headers: List<Path> = emptyList(),
)
