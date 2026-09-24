package com.itsaky.androidide.build

import java.nio.file.Files
import java.nio.file.Path

object NativeProjectLocator {

  fun findRoot(start: Path?): Path? {
    var current = start?.toAbsolutePath()?.normalize() ?: return null

    if (Files.isRegularFile(current)) {
      current = current.parent ?: return null
    }

    while (true) {
      if (Files.isRegularFile(
          current.resolve("androidide-project.properties")
        )
      ) {
        return current
      }

      current = current.parent ?: return null
    }
  }
}
