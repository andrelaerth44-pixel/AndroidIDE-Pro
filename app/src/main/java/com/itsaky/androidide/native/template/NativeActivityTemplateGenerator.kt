package com.itsaky.androidide.native.template

import com.itsaky.androidide.native.model.NativeLibraryType
import com.itsaky.androidide.native.model.NativeProjectConfigStore
import com.itsaky.androidide.utils.NativeLibraryNaming
import java.io.File

data class NativeActivityTemplateSpec(
  val packageName: String,
  val className: String = "NativeHostActivity",
  val appLabel: String = "Native Activity",
)

object NativeActivityTemplateGenerator {

  fun generate(
    moduleRoot: File,
    spec: NativeActivityTemplateSpec,
  ): List<File> {
    require(moduleRoot.isDirectory) {
      "Native module root does not exist: " + moduleRoot.absolutePath
    }
    validate(spec)

    val moduleName = moduleRoot.name
    val packagePath = spec.packageName.replace('.', File.separatorChar)

    val javaFile =
      File(
        moduleRoot,
        "src/main/java/" + packagePath + "/" + spec.className + ".java",
      )
    val cppFile = File(moduleRoot, "src/main/cpp/native_activity.cpp")
    val manifestFile = File(moduleRoot, "src/main/AndroidManifest.xml")

    javaFile.parentFile?.mkdirs()
    cppFile.parentFile?.mkdirs()
    manifestFile.parentFile?.mkdirs()

    javaFile.writeText(javaSource(spec))
    cppFile.writeText(cppSource())
    manifestFile.writeText(manifestSource(spec, moduleName))
    NativeProjectConfigStore.writeDefault(moduleRoot, NativeLibraryType.SHARED)

    return listOf(
      javaFile,
      cppFile,
      manifestFile,
      File(moduleRoot, ".androidide/native.json"),
    )
  }

  private fun javaSource(spec: NativeActivityTemplateSpec): String =
    (
      "package " + spec.packageName + ";\n\n" +
        "public final class " + spec.className + " extends android.app.NativeActivity {\n" +
        "}\n"
    )

  private fun cppSource(): String =
    """
    #include <android/native_activity.h>
    #include <cstddef>

    extern "C" void ANativeActivity_onCreate(
        ANativeActivity* activity,
        void* savedState,
        size_t savedStateSize) {
      (void) activity;
      (void) savedState;
      (void) savedStateSize;
    }
    """.trimIndent() + "\n"

  private fun manifestSource(
    spec: NativeActivityTemplateSpec,
    moduleName: String,
  ): String =
    (
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
        "package=\"" + spec.packageName + "\">\n" +
        "  <application android:label=\"" + spec.appLabel + "\" " +
        "android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
        "    <activity android:name=\"" + spec.packageName + "." + spec.className + "\" " +
        "android:exported=\"true\">\n" +
        "      <meta-data android:name=\"android.app.lib_name\" " +
        "android:value=\"" + moduleName + "\" />\n" +
        "      <intent-filter>\n" +
        "        <action android:name=\"android.intent.action.MAIN\" />\n" +
        "        <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
        "      </intent-filter>\n" +
        "    </activity>\n" +
        "  </application>\n" +
        "</manifest>\n"
    )

  private fun validate(spec: NativeActivityTemplateSpec) {
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
    require(spec.appLabel.isNotBlank()) {
      "Application label must not be blank"
    }
  }
}
