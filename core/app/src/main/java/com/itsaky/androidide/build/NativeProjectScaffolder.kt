package com.itsaky.androidide.build

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

enum class NativeProjectKind(
  val nativeLanguage: NativeLanguageKind,
  val uiLanguage: UiLanguageKind?,
  val nativeActivity: Boolean
) {
  PURE_C(NativeLanguageKind.C, null, true),
  PURE_CPP(NativeLanguageKind.CPP, null, true),
  JAVA_C(NativeLanguageKind.C, UiLanguageKind.JAVA, false),
  JAVA_CPP(NativeLanguageKind.CPP, UiLanguageKind.JAVA, false),
  KOTLIN_C(NativeLanguageKind.C, UiLanguageKind.KOTLIN, false),
  KOTLIN_CPP(NativeLanguageKind.CPP, UiLanguageKind.KOTLIN, false)
}

enum class NativeLanguageKind {
  C,
  CPP
}

enum class UiLanguageKind {
  JAVA,
  KOTLIN
}

data class NativeProjectScaffoldRequest(
  val rootDir: Path,
  val packageName: String,
  val appName: String,
  val sdkRoot: Path,
  val compileSdk: Int,
  val minSdk: Int = 26,
  val targetSdk: Int = compileSdk,
  val versionCode: Int = 1,
  val versionName: String = "1.0",
  val kind: NativeProjectKind
)

object NativeProjectScaffolder {

  fun create(request: NativeProjectScaffoldRequest): Path {
    validate(request)

    request.rootDir.createDirectories()
    writeDescriptor(request)
    writeManifest(request)
    writeStrings(request)
    writeNativeConfiguration(request)

    when (request.kind.nativeLanguage) {
      NativeLanguageKind.C -> writeCSource(request)
      NativeLanguageKind.CPP -> writeCppSource(request)
    }

    when (request.kind.uiLanguage) {
      UiLanguageKind.JAVA -> writeJavaActivity(request)
      UiLanguageKind.KOTLIN -> {
        writeKotlinNativeBridge(request)
        writeKotlinActivity(request)
      }
      null -> Unit
    }

    return request.rootDir
  }

  private fun writeDescriptor(request: NativeProjectScaffoldRequest) {
    val file = request.rootDir.resolve(NativeProjectDescriptor.FILE_NAME)

    Files.writeString(
      file,
      buildString {
        appendLine("namespace=" + request.packageName)
        appendLine("applicationId=" + request.packageName)
        appendLine("compileSdk=" + request.compileSdk)
        appendLine("minSdk=" + request.minSdk)
        appendLine("targetSdk=" + request.targetSdk)
        appendLine("sdkRoot=" + request.sdkRoot.toAbsolutePath().normalize())
        appendLine("versionCode=" + request.versionCode)
        appendLine("versionName=" + request.versionName)
        appendLine("nativeActivity=" + request.kind.nativeActivity)
        appendLine(
          "nativeLibraryName=" +
            if (request.kind.nativeActivity) "main" else "appnative"
        )
      }
    )
  }

  private fun writeManifest(request: NativeProjectScaffoldRequest) {
    val manifest = request.rootDir.resolve("src/main/AndroidManifest.xml")
    manifest.parent.createDirectories()

    val launcher = if (request.kind.nativeActivity) {
      """
      <activity
          android:name="android.app.NativeActivity"
          android:exported="true">
        <meta-data
            android:name="android.app.lib_name"
            android:value="main" />
        <meta-data
            android:name="android.app.func_name"
            android:value="ANativeActivity_onCreate" />
        <intent-filter>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
      </activity>
      """.trimIndent()
    } else {
      """
      <activity
          android:name=".MainActivity"
          android:exported="true">
        <intent-filter>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
      </activity>
      """.trimIndent()
    }

    Files.writeString(
      manifest,
      """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest
          xmlns:android="http://schemas.android.com/apk/res/android"
          package="${request.packageName}">

        <application
            android:allowBackup="false"
            android:label="@string/app_name"
            android:supportsRtl="true">

          ${launcher}

        </application>
      </manifest>
      """.trimIndent()
    )
  }

  private fun writeStrings(request: NativeProjectScaffoldRequest) {
    val strings = request.rootDir.resolve("src/main/res/values/strings.xml")
    strings.parent.createDirectories()

    Files.writeString(
      strings,
      """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
        <string name="app_name">${escapeXml(request.appName)}</string>
      </resources>
      """.trimIndent()
    )
  }

  private fun writeNativeConfiguration(request: NativeProjectScaffoldRequest) {
    val config = request.rootDir
      .resolve("src/main/cpp/androidide-native.properties")
    config.parent.createDirectories()

    Files.writeString(
      config,
      buildString {
        appendLine(
          "libraryName=" +
            if (request.kind.nativeActivity) "main" else "appnative"
        )
        appendLine("includeDirs=.")
        appendLine("libraryDirs=.")
        appendLine(
          "linkLibraries=log" +
            if (request.kind.nativeActivity) ",android" else ""
        )
        appendLine("cFlags=-Wall -Wextra")
        appendLine("cppFlags=-Wall -Wextra")
        appendLine("linkerFlags=-Wl,--gc-sections")
      }
    )
  }

  private fun writeCSource(request: NativeProjectScaffoldRequest) {
    val file = request.rootDir.resolve("src/main/cpp/main.c")
    file.parent.createDirectories()

    if (request.kind.nativeActivity) {
      Files.writeString(
        file,
        """
        #include <android/log.h>
        #include <android_native_app_glue.h>

        static const char* TAG = "${request.packageName}";

        void android_main(struct android_app* app) {
          (void) app;
          __android_log_print(
              ANDROID_LOG_INFO,
              TAG,
              "AndroidIDE Pro native C app started");
        }
        """.trimIndent()
      )
    } else {
      Files.writeString(
        file,
        """
        #include <jni.h>

        JNIEXPORT jint JNICALL
        Java_${jniMangledPackage(request.packageName)}_${nativeJniClass(request)}_nativeValue(
            JNIEnv* env,
            jobject thiz) {
          (void) env;
          (void) thiz;
          return 42;
        }
        """.trimIndent()
      )
    }
  }

  private fun writeCppSource(request: NativeProjectScaffoldRequest) {
    val file = request.rootDir.resolve("src/main/cpp/main.cpp")
    file.parent.createDirectories()

    if (request.kind.nativeActivity) {
      Files.writeString(
        file,
        """
        #include <android/log.h>
        #include <android_native_app_glue.h>

        static const char* TAG = "${request.packageName}";

        void android_main(struct android_app* app) {
          (void) app;
          __android_log_print(
              ANDROID_LOG_INFO,
              TAG,
              "AndroidIDE Pro native C++ app started");
        }
        """.trimIndent()
      )
    } else {
      Files.writeString(
        file,
        """
        #include <jni.h>

        extern "C"
        JNIEXPORT jint JNICALL
        Java_${jniMangledPackage(request.packageName)}_MainActivity_nativeValue(
            JNIEnv* env,
            jobject thiz) {
          (void) env;
          (void) thiz;
          return 42;
        }
        """.trimIndent()
      )
    }
  }

  private fun writeJavaActivity(request: NativeProjectScaffoldRequest) {
    val file = request.rootDir
      .resolve("src/main/java")
      .resolve(request.packageName.replace('.', '/'))
      .resolve("MainActivity.java")

    file.parent.createDirectories()

    Files.writeString(
      file,
      """
      package ${request.packageName};

      import android.app.Activity;
      import android.os.Bundle;
      import android.widget.TextView;

      public final class MainActivity extends Activity {
        static {
          System.loadLibrary("appnative");
        }

        public static native int nativeValue();

        @Override
        protected void onCreate(Bundle state) {
          super.onCreate(state);

          TextView text = new TextView(this);
          text.setText("Native value: " + nativeValue());
          text.setTextSize(22f);
          setContentView(text);
        }
      }
      """.trimIndent()
    )
  }

  private fun writeKotlinNativeBridge(request: NativeProjectScaffoldRequest) {
    val file = request.rootDir
      .resolve("src/main/java")
      .resolve(request.packageName.replace('.', '/'))
      .resolve("NativeBridge.java")

    file.parent.createDirectories()

    Files.writeString(
      file,
      """
      package ${request.packageName};

      public final class NativeBridge {

        private NativeBridge() {}

        public static native int nativeValue();
      }
      """.trimIndent()
    )
  }

  private fun writeKotlinActivity(request: NativeProjectScaffoldRequest) {
    val file = request.rootDir
      .resolve("src/main/kotlin")
      .resolve(request.packageName.replace('.', '/'))
      .resolve("MainActivity.kt")

    file.parent.createDirectories()

    Files.writeString(
      file,
      """
      package ${request.packageName}

      import android.app.Activity
      import android.os.Bundle
      import android.widget.TextView

      class MainActivity : Activity() {

        companion object {
          init {
            System.loadLibrary("appnative")
          }
        }

        override fun onCreate(state: Bundle?) {
          super.onCreate(state)

          TextView(this).apply {
            text = "Native value: " + NativeBridge.nativeValue()
            textSize = 22f
            setContentView(this)
          }
        }
      }
      """.trimIndent()
    )
  }

  private fun validate(request: NativeProjectScaffoldRequest) {
    require(PACKAGE_REGEX.matches(request.packageName)) {
      "Invalid Android package name: " + request.packageName
    }

    require(request.appName.isNotBlank()) {
      "Application name must not be blank"
    }

    require(request.compileSdk >= request.minSdk) {
      "compileSdk must be >= minSdk"
    }

    require(request.targetSdk >= request.minSdk) {
      "targetSdk must be >= minSdk"
    }

    require(Files.isDirectory(request.sdkRoot)) {
      "Android SDK root does not exist: " + request.sdkRoot
    }
  }

  private fun nativeJniClass(
    request: NativeProjectScaffoldRequest
  ): String =
    if (request.kind.uiLanguage == UiLanguageKind.KOTLIN) {
      "NativeBridge"
    } else {
      "MainActivity"
    }

  private fun escapeXml(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  private fun jniMangledPackage(packageName: String): String =
    packageName
      .replace("_", "_1")
      .replace('.', '_')

  private val PACKAGE_REGEX =
    Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")
}
