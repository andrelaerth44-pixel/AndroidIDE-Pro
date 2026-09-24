package com.itsaky.androidide.toolchain

import android.content.Context
import android.system.Os
import com.itsaky.androidide.build.android.AndroidNativeToolchain
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class CoreToolchainManager(
  private val context: Context
) {

  fun resolveLlvm(): AndroidNativeToolchain? =
    resolveLlvmInternal(installIfMissing = true)

  fun isInstalled(): Boolean =
    resolveLlvmInternal(installIfMissing = false) != null

  private fun resolveLlvmInternal(
    installIfMissing: Boolean
  ): AndroidNativeToolchain? {
    val packageContext = runCatching {
      context.createPackageContext(
        LLVM_PACKAGE,
        Context.CONTEXT_IGNORE_SECURITY
      )
    }.getOrNull() ?: return null

    val properties = readProperties(packageContext) ?: return null
    val version = properties.getProperty("version")?.trim().orEmpty()
    val llvmMajor = properties.getProperty("llvmMajor")?.trim().orEmpty()
    if (version.isBlank() || llvmMajor.isBlank()) return null

    val root = context.filesDir.toPath()
      .resolve("toolchains")
      .resolve("llvm")
      .resolve(version)

    val marker = root.resolve(".installed")
    val archiveStamp = properties.getProperty("archiveSha256")?.trim().orEmpty()
    val installedStamp = runCatching {
      Files.readString(marker).trim()
    }.getOrNull()

    if (!marker.exists() || installedStamp != archiveStamp) {
      if (!installIfMissing) return null

      val installed = runCatching {
        installAssetArchive(
          packageContext = packageContext,
          root = root,
          expectedStamp = archiveStamp
        )
      }.isSuccess
      if (!installed) return null
    }

    val compiler = root.resolve("bin/clang")
    val linker = root.resolve("bin/ld.lld")
    val runtime = root.resolve("lib/libc++_shared.so")
    val runtimeLibraryDir = root.resolve("lib")
    val sysroot = root.resolve("sysroot")
    val resourceDir = root.resolve("lib-clang").resolve(llvmMajor)

    if (!compiler.isRegularFile() ||
      !linker.isRegularFile() ||
      !runtime.isRegularFile() ||
      !Files.isDirectory(sysroot) ||
      !Files.isDirectory(resourceDir)
    ) {
      return null
    }

    return AndroidNativeToolchain(
      version = version,
      compiler = compiler,
      linker = linker,
      sysroot = sysroot,
      resourceDir = resourceDir,
      runtimeLibraryDir = runtimeLibraryDir,
      runtimeSharedLibrary = runtime,
      includeDirs = buildList {
        add(root.resolve("sysroot/usr/include"))
        add(root.resolve("sysroot/usr/include/c++/v1"))
      },
      nativeAppGlueDir = root.resolve("native_app_glue")
        .takeIf { Files.isDirectory(it) }
    )
  }

  fun describe(): String =
    if (isInstalled()) {
      "Core LLVM C/C++ toolchain ready"
    } else {
      "Core LLVM C/C++ toolchain not installed"
    }

  private fun readProperties(packageContext: Context): Properties? =
    runCatching {
      Properties().apply {
        packageContext.assets.open(PROPERTIES_FILE).use { load(it) }
      }
    }.getOrNull()

  private fun installAssetArchive(
    packageContext: Context,
    root: Path,
    expectedStamp: String
  ) {
    val parent = root.parent
      ?: error("Invalid LLVM toolchain root: " + root)

    parent.createDirectories()
    val temp = parent.resolve("." + root.fileName + ".tmp")
    temp.deleteRecursively()
    temp.createDirectories()

    packageContext.assets.open(ARCHIVE_FILE).use { input ->
      ZipInputStream(BufferedInputStream(input)).use { zip ->
        var entry: ZipEntry? = zip.nextEntry
        while (entry != null) {
          val name = entry!!.name
          val target = safeResolve(temp, name)

          if (entry!!.isDirectory) {
            target.createDirectories()
          } else {
            target.parent?.createDirectories()
            Files.newOutputStream(target).use { output ->
              zip.copyTo(output)
            }
          }

          zip.closeEntry()
          entry = zip.nextEntry
        }
      }
    }

    root.deleteRecursively()
    Files.move(temp, root)

    makeExecutables(root.resolve("bin"))

    root.resolve(".installed").also {
      it.parent?.createDirectories()
      Files.writeString(it, expectedStamp)
    }

    cleanupOldVersions(root.parent, root.fileName.toString())
  }

  private fun makeExecutables(binDir: Path) {
    if (!Files.isDirectory(binDir)) return
    Files.list(binDir).use { stream ->
      stream
        .filter { Files.isRegularFile(it) }
        .forEach { Os.chmod(it.toString(), 0x1ED) }
    }
  }

  private fun cleanupOldVersions(parent: Path, keepVersion: String) {
    if (!Files.isDirectory(parent)) return
    Files.list(parent).use { stream ->
      stream
        .filter { Files.isDirectory(it) }
        .filter { it.fileName.toString() != keepVersion }
        .forEach { old -> runCatching { old.deleteRecursively() } }
    }
  }

  private fun safeResolve(root: Path, entryName: String): Path {
    val normalized = root.resolve(entryName).normalize()
    check(normalized.startsWith(root.normalize())) {
      "Unsafe toolchain archive entry: " + entryName
    }
    return normalized
  }

  companion object {
    const val LLVM_PACKAGE = "com.itsaky.androidide.toolchain.llvm"
    const val PROPERTIES_FILE = "toolchain.properties"
    const val ARCHIVE_FILE = "toolchain.zip"

  }
}
