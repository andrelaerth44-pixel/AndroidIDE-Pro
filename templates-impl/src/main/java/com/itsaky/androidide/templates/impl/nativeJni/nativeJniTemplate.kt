package com.itsaky.androidide.templates.impl.nativeJni

import com.itsaky.androidide.templates.Language
import com.itsaky.androidide.templates.ParameterConstraint.DIRECTORY
import com.itsaky.androidide.templates.ParameterConstraint.EXISTS
import com.itsaky.androidide.templates.ParameterConstraint.NONEMPTY
import com.itsaky.androidide.templates.ProjectTemplate
import com.itsaky.androidide.templates.ProjectTemplateData
import com.itsaky.androidide.templates.ProjectTemplateRecipeResult
import com.itsaky.androidide.templates.ProjectVersionData
import com.itsaky.androidide.templates.RecipeExecutor
import com.itsaky.androidide.templates.TemplateRecipe
import com.itsaky.androidide.templates.TextFieldWidget
import com.itsaky.androidide.templates.packageNameParameter
import com.itsaky.androidide.templates.projectNameParameter
import com.itsaky.androidide.templates.stringParameter
import com.itsaky.androidide.templates.impl.ProjectTemplateRecipeResultImpl
import com.itsaky.androidide.templates.impl.R
import com.itsaky.androidide.utils.Environment
import java.io.File

fun nativeJniProject(): ProjectTemplate {
  val projectName = projectNameParameter()
  val packageName = packageNameParameter()
  val saveLocation = stringParameter {
    name = R.string.wizard_save_location
    default = Environment.PROJECTS_DIR.absolutePath
    endIcon = { R.drawable.ic_folder }
    constraints = listOf(NONEMPTY, DIRECTORY, EXISTS)
  }

  projectName.observe { parameter ->
    packageName.setValue(
      com.itsaky.androidide.utils.AndroidUtils.appNameToPackageName(
        parameter.value,
        packageName.value,
      )
    )
  }

  val widgets = listOf(
    TextFieldWidget(projectName),
    TextFieldWidget(packageName),
    TextFieldWidget(saveLocation),
  )

  val recipe = TemplateRecipe<ProjectTemplateRecipeResult> { executor ->
    val projectDir = File(saveLocation.value, projectName.value)
    require(!projectDir.exists() || projectDir.listFiles().isNullOrEmpty()) {
      "Project directory already exists and is not empty"
    }

    val data = ProjectTemplateData(
      name = projectName.value,
      projectDir = projectDir,
      version = ProjectVersionData(),
      language = Language.Java,
      useKts = false,
    )

    writeNativeJniProject(executor, data, packageName.value)
    ProjectTemplateRecipeResultImpl(data)
  }

  return ProjectTemplate(
    moduleTemplates = emptyList(),
    templateName = R.string.template_native_jni,
    thumb = R.drawable.template_cpp_configure,
    description = R.string.template_native_jni_description,
    widgets = widgets,
    recipe = recipe,
  )
}

private fun writeNativeJniProject(
  executor: RecipeExecutor,
  data: ProjectTemplateData,
  packageName: String,
) {
  val packagePath = packageName.replace('.', File.separatorChar)
  val moduleName = data.name.lowercase().replace(Regex("[^a-z0-9_]"), "_")
  val javaDir = File(data.projectDir, "src/main/java/$packagePath")
  val bridge = File(javaDir, "NativeBridge.java")
  val activity = File(javaDir, "MainActivity.java")
  val cpp = File(data.projectDir, "src/main/cpp/native_bridge.cpp")
  val manifest = File(data.projectDir, "src/main/AndroidManifest.xml")
  val nativeConfig = File(data.projectDir, ".androidide/native.json")

  executor.save(
    "package $packageName;\n\n" +
      "public final class NativeBridge {\n" +
      "  static { System.loadLibrary(\"$moduleName\"); }\n\n" +
      "  private NativeBridge() {}\n\n" +
      "  public static native String stringFromNative();\n" +
      "}\n",
    bridge,
  )

  executor.save(
    "package $packageName;\n\n" +
      "public final class MainActivity extends android.app.Activity {\n" +
      "  @Override public void onCreate(android.os.Bundle state) {\n" +
      "    super.onCreate(state);\n" +
      "    android.widget.TextView text = new android.widget.TextView(this);\n" +
      "    text.setText(NativeBridge.stringFromNative());\n" +
      "    setContentView(text);\n" +
      "  }\n" +
      "}\n",
    activity,
  )

  val nativeSymbol =
    "Java_" + packageName.replace("_", "_1").replace('.', '_') +
      "_NativeBridge_stringFromNative"

  executor.save(
    "#include <jni.h>\n\n" +
      "extern \"C\" JNIEXPORT jstring JNICALL $nativeSymbol(\n" +
      "    JNIEnv* env,\n" +
      "    jclass) {\n" +
      "  return env->NewStringUTF(\"Hello from AndroidIDE Pro\");\n" +
      "}\n",
    cpp,
  )

  executor.save(
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
      "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
      "package=\"$packageName\">\n" +
      "  <application android:label=\"" + data.name + "\" " +
      "android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
      "    <activity android:name=\"$packageName.MainActivity\" android:exported=\"true\">\n" +
      "      <intent-filter>\n" +
      "        <action android:name=\"android.intent.action.MAIN\" />\n" +
      "        <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
      "      </intent-filter>\n" +
      "    </activity>\n" +
      "  </application>\n" +
      "</manifest>\n",
    manifest,
  )

  executor.save(
    "{\n" +
      "  \"libraryType\": \"SHARED\",\n" +
      "  \"androidApiLevel\": 28\n" +
      "}\n",
    nativeConfig,
  )
}