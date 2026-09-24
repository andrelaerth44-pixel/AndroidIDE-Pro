package com.itsaky.androidide.ui.pro

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.itsaky.androidide.R
import com.itsaky.androidide.activities.toolchain.ToolchainCenterActivity
import com.itsaky.androidide.build.BuildRouter
import com.itsaky.androidide.build.api.BuildResult
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.toolchain.CoreToolchainManager
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildCenterScreen(
  modulePath: String?,
  projectRoot: String? = null,
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val logs = remember { mutableStateListOf<String>() }
  val scope = rememberCoroutineScope()
  val llvmReady = remember {
    CoreToolchainManager(context.applicationContext).isInstalled()
  }

  var result by remember { mutableStateOf<BuildResult?>(null) }
  var started by remember { mutableStateOf(false) }
  var installed by remember { mutableStateOf(false) }

  val installLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) {
    val appId = result?.applicationId
    installed = appId != null &&
      context.packageManager.getLaunchIntentForPackage(appId) != null
  }

  LaunchedEffect(result?.applicationId) {
    val appId = result?.applicationId
    installed = appId != null &&
      context.packageManager.getLaunchIntentForPackage(appId) != null
  }

  LaunchedEffect(modulePath) {
    if (started) return@LaunchedEffect
    started = true

    result = withContext(Dispatchers.IO) {
      val workspace = IProjectManager.getInstance().getWorkspace()

      when {
        projectRoot != null -> {
          BuildRouter(context.applicationContext)
            .assembleDebug(Path.of(projectRoot)) { line ->
              scope.launch {
                logs.add(line)
              }
            }
        }

        workspace == null -> {
          BuildResult(false, message = context.getString(R.string.build_center_no_project))
        }

        else -> {
          val path = modulePath ?: workspace.androidProjects().firstOrNull()?.path
          if (path == null) {
            BuildResult(false, message = context.getString(R.string.build_center_no_project))
          } else {
            BuildRouter(context.applicationContext)
              .assembleDebug(workspace, path) { line ->
                scope.launch {
                  logs.add(line)
                }
              }
          }
        }
      }
    }
  }

  AndroidIDEProTheme {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.build_center_title),
              fontWeight = FontWeight.SemiBold
            )
          },
          navigationIcon = {
            IconButton(onClick = onClose) {
              Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null
              )
            }
          }
        )
      }
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (llvmReady) {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            } else {
              MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
            }
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Core LLVM • C/C++",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = if (llvmReady) {
                  "Android arm64 toolchain pronta"
                } else {
                  "Instale o Core LLVM para projetos C/C++"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Button(
              onClick = {
                context.startActivity(
                  Intent(context, ToolchainCenterActivity::class.java)
                )
              }
            ) {
              Text(if (llvmReady) "Gerenciar" else "Instalar")
            }
          }
        }

        val current = result

        if (current == null) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

          Text(
            text = stringResource(R.string.build_center_running),
            style = MaterialTheme.typography.titleMedium
          )
        } else {
          val success = current.success

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (success) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.errorContainer
              }
            )
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = stringResource(
                  if (success) R.string.build_center_success
                  else R.string.build_center_failed
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )

              Text(
                text = current.message,
                style = MaterialTheme.typography.bodyMedium
              )

              current.outputApk?.let {
                Text(
                  text = stringResource(R.string.build_center_output) + ": " + it,
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }

          current.outputApk?.let { apk ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".providers.fileprovider",
                    apk.toFile()
                  )

                  installLauncher.launch(
                    Intent(Intent.ACTION_VIEW).apply {
                      setDataAndType(
                        uri,
                        "application/vnd.android.package-archive"
                      )
                      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                  )
                },
                modifier = Modifier.weight(1f)
              ) {
                Text(stringResource(R.string.build_center_install))
              }

              Button(
                onClick = {
                  val appId = current.applicationId
                  val launchIntent = appId?.let {
                    context.packageManager.getLaunchIntentForPackage(it)
                  }

                  if (launchIntent == null) {
                    Toast.makeText(
                      context,
                      "Instale o APK primeiro para executar.",
                      Toast.LENGTH_SHORT
                    ).show()
                  } else {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                  }
                },
                enabled = installed,
                modifier = Modifier.weight(1f)
              ) {
                Text("Run")
              }
            }

            Button(
              onClick = onClose,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(stringResource(R.string.build_center_close))
            }
          } ?: run {
            Button(
              onClick = onClose,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(stringResource(R.string.build_center_close))
            }
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
              RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
        ) {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            items(logs) { line ->
              Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
