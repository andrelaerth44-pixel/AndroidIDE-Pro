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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.itsaky.androidide.R
import com.itsaky.androidide.toolchain.CoreToolchainManager
import java.io.File

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ToolchainCenterScreen(
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val manager = remember { CoreToolchainManager(context.applicationContext) }
  var ready by remember { mutableStateOf(manager.isInstalled()) }
  var importing by remember { mutableStateOf(false) }

  val picker = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    importing = true

    runCatching {
      val target = File(
        context.cacheDir,
        "toolchains/llvm-core-pack.apk"
      )
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.Build, contentDescription = null)
              Text(
                text = "LLVM C/C++ • Android arm64",
                style = MaterialTheme.typography.titleMedium
              )
            }

            Text(
              text = if (ready) {
                "Pronto para compilar C17 e C++20 no próprio dispositivo."
              } else {
                "O pacote Core LLVM ainda não está instalado neste dispositivo."
              },
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }

        Text(
          text = "O compilador é um componente de primeira classe do AndroidIDE Pro. " +
            "Ele não é um plugin de linguagem e não usa Gradle para compilar o seu projeto.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (importing) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              picker.launch(
                arrayOf(
                  "application/vnd.android.package-archive",
                  "application/octet-stream"
                )
              )
            },
            enabled = !importing,
            modifier = Modifier.weight(1f)
          ) {
            Text("Importar pack LLVM")
          }

          IconButton(
            onClick = {
              ready = manager.isInstalled()
              importing = false
            }
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
          }
        }

        Text(
          text = "Use o APK oficial AndroidIDE Pro LLVM Toolchain produzido pelo pipeline " +
            "de distribuição arm64 do projeto.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
          onClick = onClose,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(context.getString(R.string.build_center_close))
        }
      }
    }
  }
}
