package com.itsaky.androidide.native.template

import com.itsaky.androidide.native.model.NativeLibraryType
import com.itsaky.androidide.native.model.NativeProjectConfigStore
import java.io.File

/**
 * Generates a small JNI bridge that can be compiled by the AndroidIDE Pro native pipeline.
 *
 * The generated Java class is intentionally dependency-free so javac -h can generate JNI
 * headers without requiring the rest of the application sources to be compilable.
 */
data class NativeJniProjectTemplateSpec(
  val packageName: String,
  val className: String = "NativeBridge",
  val methodName: String = "stringFromNative",
  val moduleName: String = "nativebridge",
)

object NativeJniProjectTemplateGenerator {

  fun generate(
    moduleRoot: File,
    spec: NativeJniProjectTemplateSpec,
  ): List<File> {
    require(moduleRoot.isDirectory) {
      "Native module root does not exist: " + moduleRoot.absolutePath
    }
    validate(spec)

    val packagePath = spec.packageName.replace('.', File.separatorChar)
    val javaFile =
      File(
        moduleRoot,
        "src/main/java/$packagePath/\${spec.className}.java",
      )
    val cppFile = File(moduleRoot, "src/main/cpp/native_bridge.cpp")

    javaFile.parentFile?.mkdirs()
    cppFile.parentFile?.mkdirs()

    javaFile.writeText(javaSource(spec))
    cppFile.writeText(cppSource(spec))
    NativeProjectConfigStore.writeDefault(moduleRoot, NativeLibraryType.SHARED)

    return listOf(javaFile, cppFile, File(moduleRoot, ".androidide/native.json"))
  }

  private fun javaSource(spec: NativeJniProjectTemplateSpec): String =
    """
    package \${spec.packageName};

    public final class \${spec.className} {
      static {
        System.loadLibrary("\${spec.moduleName}");
      }

      private \${spec.className}() {
      }

      public static native String \${spec.methodName}();
    }
    """.trimIndent() + "\n"

  private fun cppSource(spec: NativeJniProjectTemplateSpec): String {
    val nativeSymbol =
      buildString {
        append("Java_")
        append(manglePackage(spec.packageName))
        append("_")
        append(mangleIdentifier(spec.className))
        append("_")
        append(mangleIdentifier(spec.methodName))
      }

    return """
    #include <jni.h>

    extern "C" JNIEXPORT jstring JNICALL ${nativeSymbol}(
        JNIEnv* env,
        jclass) {
      return env->NewStringUTF("Hello from AndroidIDE Pro");
    }
    """.trimIndent() + "\n"
  }

  private fun validate(spec: NativeJniProjectTemplateSpec) {
    require(
      spec.packageName.matches(
        Regex("[A-Za-z_][A-Za-z0-9_]*(\\.([A-Za-z_][A-Za-z0-9_]*))*")
      )
    ) {
      "Invalid Java package: " + spec.packageName
    }
    require(spec.className.matches(Regex("[A-Z][A-Za-z0-9_]*"))) {
      "Invalid Java class name: " + spec.className
    }
    require(spec.methodName.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
      "Invalid native method name: " + spec.methodName
    }
    require(spec.moduleName.matches(Regex("[A-Za-z0-9_]+"))) {
      "Invalid native library name: " + spec.moduleName
    }
  }

  private fun manglePackage(packageName: String): String =
    packageName.split('.').joinToString("_") { mangleIdentifier(it) }

  private fun mangleIdentifier(identifier: String): String =
    identifier.replace("_", "_1")
}
