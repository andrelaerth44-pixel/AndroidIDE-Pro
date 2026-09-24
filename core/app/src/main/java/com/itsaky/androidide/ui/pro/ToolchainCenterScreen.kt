package com.itsaky.androidide.ui.pro

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.itsaky.androidide.R
import com.itsaky.androidide.toolchain.CoreKotlinToolchainManager
import com.itsaky.androidide.toolchain.CoreNativeToolchainSelfTest
import com.itsaky.androidide.toolchain.CoreToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ToolchainCenterScreen(
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val llvmManager = remember { CoreToolchainManager(context.applicationContext) }
  val kotlinManager = remember { CoreKotlinToolchainManager(context.applicationContext) }

  var llvmReady by remember { mutableStateOf(llvmManager.isInstalled()) }
  var kotlinReady by remember { mutableStateOf(kotlinManager.isInstalled()) }
  val scope = rememberCoroutineScope()
  var importing by remember { mutableStateOf(false) }
  var selfTestRunning by remember { mutableStateOf(false) }
  var selfTestMessage by remember { mutableStateOf<String?>(null) }

  fun installApk(uri: android.net.Uri, fileName: String) {
    importing = true

    runCatching {
      val target = File(context.cacheDir, "toolchains/" + fileName)
      target.parentFile?.mkdirs()

      context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open selected APK" }
        target.outputStream().use { output ->
          input.copyTo(output)
        }
      }

      val installUri = FileProvider.getUriForFile(
        context,
        context.packageName + ".providers.fileprovider",
        target
      )

      context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(
            installUri,
            "application/vnd.android.package-archive"
          )
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      )
    }.onFailure {
      importing = false
    }
  }

  val llvmPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let { installApk(it, "llvm-core-pack.apk") }
  }

  val kotlinPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let { installApk(it, "kotlin-core-pack.apk") }
  }

  AndroidIDEProTheme {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Core Toolchains") },
          navigationIcon = {
            IconButton(onClick = onClose) {
              Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
          }
        )
      }
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Compiladores de primeira classe do AndroidIDE Pro. " +
            "Nenhuma linguagem depende de plugin ou de Gradle para compilar o projeto.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ToolchainCard(
          title = "LLVM C/C++ • Android arm64",
          ready = llvmReady,
          description = if (llvmReady) {
            "Pronto para C17 e C++20 no dispositivo."
          } else {
            "Instale o pack para compilar C/C++."
          },
          onInstall = {
            llvmPicker.launch(
              arrayOf(
                "application/vnd.android.package-archive",
                "application/octet-stream"
              )
            )
          }
        )

        ToolchainCard(
          title = "Kotlin Compiler • 1.9.24",
          ready = kotlinReady,
          description = if (kotlinReady) {
            "Pronto para compilar Kotlin no dispositivo."
          } else {
            "Instale o pack para projetos Kotlin."
          },
          onInstall = {
            kotlinPicker.launch(
              arrayOf(
                "application/vnd.android.package-archive",
                "application/octet-stream"
              )
            )
          }
        )

        if (importing) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
          onClick = {
            if (!llvmReady) {
              selfTestMessage = "Instale o Core LLVM antes do self-test."
              return@Button
            }

            selfTestRunning = true
            selfTestMessage = null

            scope.launch {
              val result = withContext(Dispatchers.IO) {
                val toolchain = llvmManager.resolveLlvm()
                if (toolchain == null) {
                  com.itsaky.androidide.toolchain.NativeToolchainSelfTestResult(
                    false,
                    "Core LLVM não pôde ser resolvido."
                  )
                } else {
                  CoreNativeToolchainSelfTest().run(toolchain)
                }
              }

              selfTestRunning = false
              selfTestMessage = result.message
            }
          },
          enabled = llvmReady && !selfTestRunning,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(if (selfTestRunning) "Testando C + C++…" else "Testar C/C++")
        }

        selfTestMessage?.let { message ->
          Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              llvmReady = llvmManager.isInstalled()
              kotlinReady = kotlinManager.isInstalled()
              importing = false
            },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(" Atualizar")
          }

          Button(
            onClick = onClose,
            modifier = Modifier.weight(1f)
          ) {
            Text(context.getString(R.string.build_center_close))
          }
        }
      }
    }
  }
}

@Composable
private fun ToolchainCard(
  title: String,
  ready: Boolean,
  description: String,
  onInstall: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = if (ready) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      MaterialTheme.colorScheme.errorContainer
    },
    shape = MaterialTheme.shapes.large
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Default.Build, contentDescription = null)
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium
        )
      }

      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium
      )

      Button(
        onClick = onInstall,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(if (ready) "Reinstalar / atualizar" else "Importar Core Pack")
      }
    }
  }
}
