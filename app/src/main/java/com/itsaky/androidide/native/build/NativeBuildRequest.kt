package com.itsaky.androidide.native.build

import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.BuildVariant
import com.itsaky.androidide.native.model.NativeModule

data class NativeBuildRequest(
  val module: NativeModule,
  val abi: AbiTarget,
  val variant: BuildVariant,
) {

  init {
    require(module.targets.any { it.abi == abi && it.variant == variant }) {
      "No native target exists for ABI " + abi + " and variant " + variant
    }
  }
}
