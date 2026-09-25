package com.itsaky.androidide.native.build

object NativePipeline {
    val defaultStages = listOf(
        "scanSources",
        "generateJniHeaders",
        "compileC",
        "compileCpp",
        "linkNative",
        "packageNativeLibs",
    )
}
