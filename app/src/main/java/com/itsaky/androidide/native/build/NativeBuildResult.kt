package com.itsaky.androidide.native.build

data class NativeBuildResult(
    val success: Boolean,
    val executedTasks: List<String>,
)
