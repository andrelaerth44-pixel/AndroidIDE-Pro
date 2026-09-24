/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */

package com.itsaky.androidide.build.local

import com.itsaky.androidide.build.api.BuildModule
import com.itsaky.androidide.build.api.BuildRequest
import com.itsaky.androidide.build.local.android.AndroidApplicationBuildInputs
import com.itsaky.androidide.build.local.android.NativeAndroidToolchain

fun interface AndroidApplicationInputsProvider {
  fun create(
    module: BuildModule,
    request: BuildRequest,
    toolchain: NativeAndroidToolchain,
  ): AndroidApplicationBuildInputs
}
