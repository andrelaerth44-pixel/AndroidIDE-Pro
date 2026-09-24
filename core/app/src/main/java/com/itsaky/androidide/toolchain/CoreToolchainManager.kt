package com.itsaky.androidide.toolchain

import android.content.Context
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

    val appInfo = runCatching {
      context.packageManager.getApplicationInfo(LLVM_PACKAGE, 0)
    }.getOrNull() ?: return null

    val nativeLibraryDir = Path.of(appInfo.nativeLibraryDir)
    val compiler = nativeLibraryDir.resolve("libclang.so")
    val cppCompiler = nativeLibraryDir.resolve("libclang++.so")
    val linker = nativeLibraryDir.resolve("libld.lld.so")
    val runtime = nativeLibraryDir.resolve("libc++_shared.so")
    val sysroot = root.resolve("sysroot")
    val resourceDir = root.resolve("lib-clang").resolve(llvmMajor)

    if (!compiler.isRegularFile() ||
      !cppCompiler.isRegularFile() ||
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
      cppCompiler = cppCompiler,
      linker = linker,
      sysroot = sysroot,
      resourceDir = resourceDir,
      runtimeLibraryDir = nativeLibraryDir,
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
      val digest = MessageDigest.getInstance("SHA-256")
      val archive = input.readBytes()
      digest.update(archive)
      val actualStamp = digest.digest()
        .joinToString("") { "%02x".format(it) }
      check(expectedStamp.isBlank() || actualStamp == expectedStamp) {
        "Core LLVM Toolchain checksum mismatch"
      }

      ZipInputStream(
        BufferedInputStream(archive.inputStream())
      ).use { zip ->
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

    root.resolve(".installed").also {
      it.parent?.createDirectories()
      Files.writeString(it, expectedStamp)
    }

    cleanupOldVersions(root.parent, root.fileName.toString())
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
