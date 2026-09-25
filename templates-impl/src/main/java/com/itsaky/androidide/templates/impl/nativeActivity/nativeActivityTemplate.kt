package com.itsaky.androidide.templates.impl.nativeActivity

import com.itsaky.androidide.templates.ProjectTemplate
import com.itsaky.androidide.templates.ProjectTemplateData
import com.itsaky.androidide.templates.ProjectTemplateRecipeResult
import com.itsaky.androidide.templates.RecipeExecutor
import com.itsaky.androidide.templates.TemplateRecipe
import com.itsaky.androidide.templates.TextFieldWidget
import com.itsaky.androidide.templates.base.util.optonallyKts
import com.itsaky.androidide.templates.packageNameParameter
import com.itsaky.androidide.templates.projectNameParameter
import com.itsaky.androidide.templates.stringParameter
import com.itsaky.androidide.templates.ParameterConstraint.DIRECTORY
import com.itsaky.androidide.templates.ParameterConstraint.EXISTS
import com.itsaky.androidide.templates.ParameterConstraint.NONEMPTY
import com.itsaky.androidide.templates.Language
import com.itsaky.androidide.templates.ProjectVersionData
import com.itsaky.androidide.templates.Sdk
import com.itsaky.androidide.templates.impl.ProjectTemplateRecipeResultImpl
import com.itsaky.androidide.templates.impl.R
import com.itsaky.androidide.utils.Environment
import java.io.File

fun nativeActivityProject(): ProjectTemplate {
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

    writeNativeActivityProject(executor, data, packageName.value)
    ProjectTemplateRecipeResultImpl(data)
  }

  return ProjectTemplate(
    moduleTemplates = emptyList(),
    templateName = R.string.template_native_activity,
    thumb = R.drawable.template_cpp_configure,
    description = R.string.template_native_activity_description,
    widgets = widgets,
    recipe = recipe,
  )
}

private fun writeNativeActivityProject(
  executor: RecipeExecutor,
  data: ProjectTemplateData,
  packageName: String,
) {
  val packagePath = packageName.replace('.', File.separatorChar)
  val java = File(
    data.projectDir,
    "src/main/java/$packagePath/NativeHostActivity.java",
  )
  val cpp = File(data.projectDir, "src/main/cpp/native_activity.cpp")
  val manifest = File(data.projectDir, "src/main/AndroidManifest.xml")
  val nativeConfig = File(data.projectDir, ".androidide/native.json")

  executor.save(
    "package $packageName;\n\n" +
      "public final class NativeHostActivity extends android.app.NativeActivity {\n" +
      "}\n",
    java,
  )

  executor.save(
    "#include <android/native_activity.h>\n" +
      "#include <cstddef>\n\n" +
      "extern \"C\" void ANativeActivity_onCreate(\n" +
      "    ANativeActivity* activity,\n" +
      "    void* savedState,\n" +
      "    size_t savedStateSize) {\n" +
      "  (void) activity;\n" +
      "  (void) savedState;\n" +
      "  (void) savedStateSize;\n" +
      "}\n",
    cpp,
  )

  executor.save(
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
      "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
      "package=\"$packageName\">\n" +
      "  <application android:label=\"" + data.name + "\" " +
      "android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
      "    <activity android:name=\"$packageName.NativeHostActivity\" " +
      "android:exported=\"true\">\n" +
      "      <meta-data android:name=\"android.app.lib_name\" " +
      "android:value=\"" + data.name.lowercase().replace(Regex("[^a-z0-9_]"), "_") + "\" />\n" +
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