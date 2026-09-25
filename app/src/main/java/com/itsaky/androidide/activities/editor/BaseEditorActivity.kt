/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.activities.editor

import android.content.Intent
import android.content.pm.PackageInstaller.SessionCallback
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.annotation.GravityInt
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.IntentUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.itsaky.androidide.BuildConfig
import com.itsaky.androidide.R.attr
import com.itsaky.androidide.R.drawable
import com.itsaky.androidide.R.id
import com.itsaky.androidide.R.string
import com.itsaky.androidide.actions.ActionItem.Location.EDITOR_FILE_TABS
import com.itsaky.androidide.activities.PreferencesActivity
import com.itsaky.androidide.activities.TerminalActivity
import com.itsaky.androidide.adapters.DiagnosticsAdapter
import com.itsaky.androidide.adapters.SearchListAdapter
import com.itsaky.androidide.app.IDEActivity
import com.itsaky.androidide.databinding.ActivityEditorBinding
import com.itsaky.androidide.databinding.LayoutDiagnosticInfoBinding
import com.itsaky.androidide.events.InstallationResultEvent
import com.itsaky.androidide.fragments.FileTreeFragment
import com.itsaky.androidide.fragments.SearchResultFragment
import com.itsaky.androidide.handlers.EditorActivityLifecyclerObserver
import com.itsaky.androidide.handlers.LspHandler.registerLanguageServers
import com.itsaky.androidide.interfaces.DiagnosticClickListener
import com.itsaky.androidide.logsender.LogSender
import com.itsaky.androidide.lookup.Lookup
import com.itsaky.androidide.lsp.models.DiagnosticItem
import com.itsaky.androidide.models.DiagnosticGroup
import com.itsaky.androidide.models.LogLine
import com.itsaky.androidide.models.OpenedFile
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.models.SearchResult
import com.itsaky.androidide.native.build.AndroidProjectModelLoader
import com.itsaky.androidide.native.build.DefaultNativeAndroidBuildService
import com.itsaky.androidide.native.build.DefaultNativeBuildService
import com.itsaky.androidide.native.build.NativeAndroidBuildService
import com.itsaky.androidide.native.build.NativeAndroidBuildStage
import com.itsaky.androidide.native.build.NativeBuildExecutor
import com.itsaky.androidide.native.build.NativeBuildService
import com.itsaky.androidide.native.build.NativeBuildTaskState
import com.itsaky.androidide.native.build.NativePipeline
import com.itsaky.androidide.native.model.AbiTarget
import com.itsaky.androidide.native.model.BuildVariant
import com.itsaky.androidide.native.model.NativeBuildRequest
import com.itsaky.androidide.native.model.NativeProjectModelLoader
import com.itsaky.androidide.projects.ProjectManager.getProjectDirPath
import com.itsaky.androidide.projects.ProjectManager.projectPath
import com.itsaky.androidide.projects.builder.BuildService
import com.itsaky.androidide.services.log.LogReceiverService
import com.itsaky.androidide.services.log.LogReceiverServiceConnection
import com.itsaky.androidide.services.log.lookupLogService
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchainLocator
import com.itsaky.androidide.toolchain.ToolchainManager
import com.itsaky.androidide.toolchain.ToolchainSnapshot
import com.itsaky.androidide.ui.compose.AndroidIDETheme
import com.itsaky.androidide.ui.compose.BuildCenterDialog
import com.itsaky.androidide.ui.compose.BuildCenterUiState
import com.itsaky.androidide.ui.compose.BuildLogUi
import com.itsaky.androidide.ui.compose.BuildIssueUi
import com.itsaky.androidide.ui.compose.BuildLogLevel
import com.itsaky.androidide.ui.compose.BuildStepState
import com.itsaky.androidide.ui.compose.BuildStepUi
import com.itsaky.androidide.ui.compose.CommandPaletteItem
import com.itsaky.androidide.ui.compose.EditorBreadcrumb
import com.itsaky.androidide.ui.compose.EditorWorkspaceTopBar
import com.itsaky.androidide.ui.compose.IdeIcons
import com.itsaky.androidide.ui.compose.IdeStatusBarState
import com.itsaky.androidide.ui.compose.ToolchainManagerDialog
import com.itsaky.androidide.ui.compose.WorkspaceTab
import com.itsaky.androidide.ui.editor.CodeEditorView
import com.itsaky.androidide.uidesigner.UIDesignerActivity
import com.itsaky.androidide.utils.ActionMenuUtils.createMenu
import com.itsaky.androidide.utils.ApkInstallationSessionCallback
import com.itsaky.androidide.utils.ApkInstaller
import com.itsaky.androidide.utils.InstallationResultHandler
import com.itsaky.androidide.utils.DURATION_INDEFINITE
import com.itsaky.androidide.utils.DialogUtils.newMaterialDialogBuilder
import com.itsaky.androidide.utils.ILogger
import com.itsaky.androidide.utils.InstallationResultHandler.onResult
import com.itsaky.androidide.utils.flashError
import com.itsaky.androidide.utils.flashbarBuilder
import com.itsaky.androidide.utils.resolveAttr
import com.itsaky.androidide.utils.showOnUiThread
import com.itsaky.androidide.utils.withIcon
import com.itsaky.androidide.viewmodel.EditorViewModel
import com.itsaky.androidide.xml.resources.ResourceTableRegistry
import com.itsaky.androidide.xml.versions.ApiVersionsRegistry
import com.itsaky.androidide.xml.widgets.WidgetTableRegistry
import java.io.File
import java.util.Objects
import org.eclipse.jgit.api.Git
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN

/**
 * Base class for EditorActivity which handles most of the view related things.
 *
 * @author Akash Yadav
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseEditorActivity :
  IDEActivity(),
  TabLayout.OnTabSelectedListener,
  NavigationView.OnNavigationItemSelectedListener,
  DiagnosticClickListener {

  protected val mLifecycleObserver = EditorActivityLifecyclerObserver()
  protected var diagnosticInfoBinding: LayoutDiagnosticInfoBinding? = null
  protected var filesTreeFragment: FileTreeFragment? = null
  protected var editorBottomSheet: BottomSheetBehavior<out View?>? = null
  protected var isDestroying = false

  protected val log: ILogger = ILogger.newInstance("EditorActivity")
  protected val logServiceConnection = LogReceiverServiceConnection {
    lookupLogService()?.setConsumer(this::appendApkLog)
  }

  internal var installationCallback: ApkInstallationSessionCallback? = null

  var uiDesignerResultLauncher: ActivityResultLauncher<Intent>? = null
  val viewModel by viewModels<EditorViewModel>()
  lateinit var binding: ActivityEditorBinding
    protected set

  private val workspaceTabs = mutableStateListOf<WorkspaceTab>()
  private val workspaceBreadcrumbs = mutableStateListOf<EditorBreadcrumb>()
  private var workspaceSelectedTab by mutableIntStateOf(-1)
  private var workspaceStatus by mutableStateOf(IdeStatusBarState())
  private var buildCenterState by mutableStateOf(BuildCenterUiState())
  private var buildCenterOpen by mutableStateOf(false)
  private var nativeBuildService: NativeBuildService? = null
  private var nativeAndroidBuildService: NativeAndroidBuildService? = null
  private var toolchainManagerSnapshot by mutableStateOf(ToolchainManager.inspect())
  private var toolchainManagerOpen by mutableStateOf(false)
  private var workspaceComposeView: ComposeView? = null

  private val onBackPressedCallback: OnBackPressedCallback =
    object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (binding.root.isDrawerOpen(GravityCompat.END)) {
          binding.root.closeDrawer(GravityCompat.END)
        } else if (binding.root.isDrawerOpen(GravityCompat.START)) {
          binding.root.closeDrawer(GravityCompat.START)
        } else if (editorBottomSheet?.state != BottomSheetBehavior.STATE_COLLAPSED) {
          editorBottomSheet?.setState(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
          doConfirmProjectClose()
        }
      }
    }

  companion object {
    const val EDITOR_CONTAINER_SCALE_FACTOR = 0.87f
    const val KEY_BOTTOM_SHEET_SHOWN = "editor_bottomSheetShown"
    const val KEY_PROJECT_PATH = "saved_projectPath"
  }

  protected abstract fun provideCurrentEditor(): CodeEditorView?

  protected abstract fun provideEditorAt(index: Int): CodeEditorView?

  protected abstract fun doOpenFile(file: File, selection: Range?)

  protected abstract fun doSaveAll(): Boolean

  protected abstract fun doDismissSearchProgress()

  protected abstract fun doConfirmProjectClose()

  protected abstract fun getOpenedFiles(): List<OpenedFile>

  protected open fun preDestroy() {
    installationCallback?.destroy()
    installationCallback = null

    try {
      lookupLogService()?.setConsumer(null)
      logServiceConnection.onConnected = null
      unbindService(logServiceConnection)
    } catch (e: Exception) {
      log.error("Failed to unbind LogReceiver service")
    }
  }

  protected open fun postDestroy() {
    if (isDestroying) {
      Lookup.getDefault().unregisterAll()
      ApiVersionsRegistry.getInstance().clear()
      ResourceTableRegistry.getInstance().clear()
      WidgetTableRegistry.getInstance().clear()
    }
  }

  override fun bindLayout(): View {
    this.binding = ActivityEditorBinding.inflate(layoutInflater)
    this.diagnosticInfoBinding = this.binding.diagnosticInfo
    return this.binding.root
  }

  @Subscribe(threadMode = MAIN)
  open fun onInstallationResult(event: InstallationResultEvent) {
    val intent = event.intent
    if (isDestroying) {
      return
    }

    val packageName = onResult(this, intent)
    if (packageName != null) {
      Snackbar.make(binding.realContainer, string.msg_action_open_application, Snackbar.LENGTH_LONG)
        .setAction(string.yes) { tryLaunchApp(packageName) }
        .show()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    registerLanguageServers()

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PROJECT_PATH)) {
      projectPath = savedInstanceState.getString(KEY_PROJECT_PATH)!!
    }

    onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    lifecycle.addObserver(mLifecycleObserver)

    setSupportActionBar(binding.editorToolbar)

    setupDrawerToggle()
    binding.tabs.addOnTabSelectedListener(this)

    setupViews()
    setupComposeWorkspace()

    KeyboardUtils.registerSoftInputChangedListener(this) { onSoftInputChanged() }
    startLogReceiver()
    setupContainers()
    setupDiagnosticInfo()

    uiDesignerResultLauncher =
      registerForActivityResult(StartActivityForResult(), this::handleUiDesignerResult)
  }

  override fun onPause() {
    super.onPause()
    this.isDestroying = isFinishing
    getFileTreeFragment()?.saveTreeState()
  }

  override fun onResume() {
    super.onResume()
    invalidateOptionsMenu()

    try {
      getFileTreeFragment()?.listProjectFiles()
    } catch (th: Throwable) {
      log.error("Failed to update files list", th)
      flashError(string.msg_failed_list_files)
    }
  }

  override fun onStop() {
    super.onStop()

    checkIsDestroying()
  }

  override fun onDestroy() {
    checkIsDestroying()
    preDestroy()
    super.onDestroy()
    postDestroy()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putString(KEY_PROJECT_PATH, getProjectDirPath())
    super.onSaveInstanceState(outState)
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      id.editornav_discuss -> app.openTelegramGroup()
      id.editornav_channel -> app.openTelegramChannel()
      id.editornav_suggest -> app.openGitHub()
      id.editornav_needHelp -> showNeedHelpDialog()
      id.editornav_settings -> startActivity(Intent(this, PreferencesActivity::class.java))
      id.editornav_share ->
        startActivity(IntentUtils.getShareTextIntent(getString(string.msg_share_app)))
      id.editornav_close_project -> doConfirmProjectClose()
      id.editornav_terminal -> openTerminal()
    }

    binding.root.closeDrawer(GravityCompat.START)
    return false
  }

  override fun onTabSelected(tab: Tab) {
    val position = tab.position
    workspaceSelectedTab = position
    viewModel.displayedFileIndex = position

    val editorView = provideEditorAt(position)!!
    editorView.onEditorSelected()

    viewModel.setCurrentFile(position, editorView.file)
    refreshSymbolInput(editorView)
    syncWorkspaceStatus(position)
    invalidateOptionsMenu()
  }

  protected open fun closeWorkspaceTab(index: Int) {}

  protected fun refreshWorkspaceStatus(index: Int = workspaceSelectedTab) {
    syncWorkspaceStatus(index)
  }

  internal fun showBuildCenter() {
    buildCenterOpen = true
  }

  internal fun hideBuildCenter() {
    buildCenterOpen = false
  }

  internal fun showToolchainManager() {
    toolchainManagerSnapshot = ToolchainManager.inspect()
    toolchainManagerOpen = true
  }

  internal fun hideToolchainManager() {
    toolchainManagerOpen = false
  }

  internal fun refreshToolchainManager() {
    toolchainManagerSnapshot = ToolchainManager.inspect()
  }

  private fun updateNativeAndroidBuildStage(
    stage: NativeAndroidBuildStage,
    detail: String?,
  ) {
    val current = buildCenterState
    val stageId = "android-native." + stage.name.lowercase()
    val updated =
      current.steps.map { step ->
        when {
          step.id == stageId ->
            step.copy(
              state =
                when (stage) {
                  NativeAndroidBuildStage.SUCCESS -> BuildStepState.SUCCESS
                  NativeAndroidBuildStage.FAILED -> BuildStepState.FAILED
                  else -> BuildStepState.RUNNING
                },
              detail = detail ?: step.detail,
            )
          step.state == BuildStepState.RUNNING -> step.copy(state = BuildStepState.SUCCESS)
          else -> step
        }
      }
    val completed = updated.count { it.state == BuildStepState.SUCCESS }
    buildCenterState =
      current.copy(
        status = detail ?: current.status,
        progress = if (updated.isEmpty()) null else completed.toFloat() / updated.size.toFloat(),
        steps = updated,
        isBuilding = stage != NativeAndroidBuildStage.SUCCESS && stage != NativeAndroidBuildStage.FAILED,
      )
  }
  internal fun beginBuildCenterSteps(steps: List<BuildStepUi>, status: String = "Preparing build…") {
    buildCenterState =
      BuildCenterUiState(
        isBuilding = true,
        status = status,
        steps = steps,
      )
  }

  internal fun updateBuildCenterTask(
    taskId: String,
    state: NativeBuildTaskState,
    detail: String? = null,
  ) {
    val current = buildCenterState
    val updated =
      current.steps.map { step ->
        if (step.id != taskId) {
          step
        } else {
          step.copy(
            state =
              when (state) {
                NativeBuildTaskState.RUNNING -> BuildStepState.RUNNING
                NativeBuildTaskState.SUCCESS -> BuildStepState.SUCCESS
                NativeBuildTaskState.FAILED -> BuildStepState.FAILED
              },
            detail = detail ?: step.detail,
          )
        }
      }
    val completed = updated.count { it.state == BuildStepState.SUCCESS }
    val progress =
      if (updated.isEmpty()) null else completed.toFloat() / updated.size.toFloat()

    buildCenterState =
      current.copy(
        isBuilding = current.isBuilding,
        status = detail ?: current.status,
        progress = progress,
        steps = updated,
      )
  }

  internal fun beginBuildCenter(tasks: List<String>) {
    val steps =
      tasks.mapIndexed { index, task ->
        BuildStepUi(
          id = task.ifBlank { "task-${index}" },
          title = task.ifBlank { "Build task ${index + 1}" },
          state = BuildStepState.PENDING,
        )
      }
    buildCenterState =
      BuildCenterUiState(
        isBuilding = true,
        status = "Preparing build…",
        steps = steps,
      )
  }

  internal fun updateBuildCenterProgress(message: String) {
    val current = buildCenterState
    val normalized = message.trim()
    val updatedSteps =
      if (normalized.isBlank() || current.steps.isEmpty()) {
        current.steps
      } else {
        current.steps.mapIndexed { index, step ->
          when {
            step.state == BuildStepState.SUCCESS -> step
            step.title == normalized || normalized.contains(step.title) ->
              step.copy(state = BuildStepState.RUNNING, detail = normalized)
            index == 0 && current.steps.none { it.state == BuildStepState.RUNNING } ->
              step.copy(state = BuildStepState.RUNNING, detail = normalized)
            else -> step
          }
        }
      }
    buildCenterState =
      current.copy(
        isBuilding = true,
        status = normalized.ifBlank { current.status },
        steps = updatedSteps,
      )
  }

  internal fun appendBuildCenterOutput(line: String?) {
    val text = line?.trimEnd().orEmpty()
    if (text.isBlank()) return

    val current = buildCenterState
    val level =
      when {
        text.contains("error", ignoreCase = true) -> BuildLogLevel.ERROR
        text.contains("warning", ignoreCase = true) -> BuildLogLevel.WARNING
        else -> BuildLogLevel.INFO
      }

    val logs =
      (current.logs + BuildLogUi(
        id = System.nanoTime().toString(),
        message = text,
        level = level,
      )).takeLast(500)

    val issue = parseBuildIssue(text)
    val issues =
      if (issue == null) {
        current.issues
      } else {
        (current.issues + issue).takeLast(200)
      }

    buildCenterState = current.copy(
      logs = logs,
      issues = issues,
      status = text,
    )
  }

  private fun parseBuildIssue(line: String): BuildIssueUi? {
    val patterns =
      listOf(
        Regex("""^(.+?):\((\d+),(\d+)\):\s+(error|warning):\s+(.+)$"""),
        Regex("""^(.+?):(\d+):(\d+):\s+(error|warning):\s+(.+)$"""),
        Regex("""^(.+?):(\d+):\s+(error|warning):\s+(.+)$"""),
      )

    for ((index, pattern) in patterns.withIndex()) {
      val match = pattern.find(line) ?: continue
      val group = match.groupValues
      val file = group[1]
      val lineNumber = group[2].toIntOrNull()
      val column = if (index == 2) null else group[3].toIntOrNull()
      val level = if (index == 2) group[3] else group[4]
      val message = if (index == 2) group[4] else group[5]
      return BuildIssueUi(
        id = "${file}:${lineNumber ?: 0}:${column ?: 0}:${message}",
        message = message,
        file = file,
        line = lineNumber,
        column = column,
        isError = level.equals("error", ignoreCase = true),
      )
    }

    return null
  }

  private fun preferredNativeAbi(): AbiTarget =
    Build.SUPPORTED_ABIS
      .asSequence()
      .mapNotNull(AbiTarget::fromAndroidAbi)
      .firstOrNull()
      ?: AbiTarget.ARM64_V8A

  internal fun startNativeBuild() {
    val projectRoot =
      runCatching { File(getProjectDirPath()).canonicalFile }.getOrNull()
        ?: run {
          flashError("Project root is unavailable")
          return
        }

    val moduleRoot =
      File(projectRoot, "app").takeIf { it.isDirectory }
        ?: projectRoot

    val module =
      runCatching {
        NativeProjectModelLoader.load(
          moduleRoot = moduleRoot,
          abi = preferredNativeAbi(),
          variant = BuildVariant.DEBUG,
        )
      }.getOrNull()

    if (module == null) {
      flashError("No native C/C++ sources were found")
      return
    }

    val toolchain = NativeToolchainLocator.locate()
    val requiresClang = module.targets.any {
      it.sourceSet.cSources.isNotEmpty()
    }
    val requiresClangCpp = module.targets.any {
      it.sourceSet.cppSources.isNotEmpty()
    }

    val missingTool =
      when {
        requiresClang && toolchain.tool(NativeToolId.CLANG)?.path == null -> "Clang"
        requiresClangCpp && toolchain.tool(NativeToolId.CLANGXX)?.path == null -> "Clang++"
        module.targets.any { it.libraryType == com.itsaky.androidide.native.model.NativeLibraryType.SHARED } &&
          toolchain.tool(NativeToolId.CLANGXX)?.path == null -> "Clang++"
        module.targets.any { it.libraryType == com.itsaky.androidide.native.model.NativeLibraryType.STATIC } &&
          toolchain.tool(NativeToolId.LLVM_AR)?.path == null -> "LLVM ar"
        else -> null
      }

    if (missingTool != null) {
      appendBuildCenterOutput("Native toolchain is missing: $missingTool")
      showToolchainManager()
      return
    }

    val request =
      NativeBuildRequest(
        module = module,
        abi = preferredNativeAbi(),
        variant = BuildVariant.DEBUG,
      )

    val graph = NativePipeline.createGraph(request)
    beginBuildCenterSteps(
      graph.tasks.map {
        BuildStepUi(
          id = it.id,
          title = it.description,
          state = BuildStepState.PENDING,
        )
      },
      status = "Preparing native build…",
    )
    showBuildCenter()

    doSaveAll()

    val service =
      DefaultNativeBuildService(
        executor =
          NativeBuildExecutor(
            toolchain = toolchain,
            androidApiLevel = module.androidApiLevel,
          )
      )

    nativeBuildService = service

    service.execute(
      request = request,
      moduleRoot = moduleRoot,
      onTaskState = { task, state ->
        ThreadUtils.runOnUiThread {
          updateBuildCenterTask(task.id, state, task.description)
        }
      },
      onOutput = { line ->
        ThreadUtils.runOnUiThread {
          appendBuildCenterOutput(line)
        }
      },
    ).whenComplete { result, error ->
      ThreadUtils.runOnUiThread {
        when {
          error != null -> {
            finishBuildCenter(
              success = false,
              tasks = graph.tasks.map { it.description },
            )
            appendBuildCenterOutput(
              "Native build failed: " +
                (error.message ?: error.javaClass.simpleName)
            )
          }

          result?.success == true -> {
            finishBuildCenter(
              success = true,
              tasks = graph.tasks.map { it.description },
            )
            result.outputFile?.let {
              appendBuildCenterOutput("Native output: " + it.absolutePath)
            }
          }

          else -> {
            finishBuildCenter(
              success = false,
              tasks = graph.tasks.map { it.description },
            )
            appendBuildCenterOutput(
              result?.message ?: "Native build failed"
            )
          }
        }

        nativeBuildService = null
      }
    }
  }

  internal fun startBuildCenterBuild(installAfterBuild: Boolean = false) {
    val root = runCatching { File(getProjectDirPath()).canonicalFile }.getOrNull() ?: return
    val module = File(root, "app").takeIf { it.isDirectory } ?: root
    if (AndroidProjectModelLoader.load(module) == null) {
      flashError("Android module is unavailable")
      appendBuildCenterOutput("Native backend could not find a valid Android module.")
      return
    }
    val stages = NativeAndroidBuildStage.values()
      .filter { it != NativeAndroidBuildStage.SUCCESS && it != NativeAndroidBuildStage.FAILED }
      .map { s -> BuildStepUi("android-native." + s.name.lowercase(), s.name.lowercase().replace('_', ' '), BuildStepState.PENDING) }
    beginBuildCenterSteps(stages, "Preparing native Android build…")
    showBuildCenter()
    doSaveAll()
    val service = DefaultNativeAndroidBuildService()
    nativeAndroidBuildService = service
    service.execute(
      moduleRoot = module,
      variant = BuildVariant.DEBUG,
      abi = preferredNativeAbi(),
      onStage = { stage, detail -> ThreadUtils.runOnUiThread { updateNativeAndroidBuildStage(stage, detail) } },
      onOutput = { line -> ThreadUtils.runOnUiThread { appendBuildCenterOutput(line) } },
    ).whenComplete { result, error ->
      ThreadUtils.runOnUiThread {
        if (error != null || result?.success != true) {
          finishBuildCenter(false, stages.map { it.title })
          appendBuildCenterOutput(error?.message ?: result?.message ?: "Native Android build failed")
        } else {
          finishBuildCenter(true, stages.map { it.title })
          result.outputApk?.let { apk ->
            appendBuildCenterOutput("APK: " + apk.absolutePath)
            if (installAfterBuild) {
              ApkInstaller.installApk(
                this@BaseEditorActivity,
                InstallationResultHandler.createEditorActivitySender(this@BaseEditorActivity),
                apk,
                installationSessionCallback(),
              )
            }
          }
        }
        nativeAndroidBuildService = null
      }
    }
  }
  internal fun startLegacyGradleBuild() {
    val buildService = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)
    if (buildService == null) {
      flashError("Build service is unavailable")
      return
    }
    if (!buildService.isToolingServerStarted()) {
      flashError(string.msg_tooling_server_unavailable)
      return
    }

    beginBuildCenter(listOf("assembleDebug"))
    doSaveAll()
    showBuildCenter()

    try {
      buildService.executeTasks("assembleDebug").whenComplete { _, error ->
        if (error != null) {
          ThreadUtils.runOnUiThread {
            viewModel.isBuildInProgress = false
            finishBuildCenter(success = false, tasks = listOf("assembleDebug"))
            appendBuildCenterOutput("Build failed to start: ${error.message ?: error.javaClass.simpleName}")
          }
        }
      }
    } catch (error: Throwable) {
      viewModel.isBuildInProgress = false
      finishBuildCenter(success = false, tasks = listOf("assembleDebug"))
      appendBuildCenterOutput("Build failed to start: ${error.message ?: error.javaClass.simpleName}")
    }
  }

  internal fun stopBuildCenterBuild() {
    nativeAndroidBuildService?.let { service ->
      if (service.isBuildInProgress) {
        buildCenterState = buildCenterState.copy(status = "Cancelling native Android build…")
        if (!service.cancelCurrentBuild()) {
          appendBuildCenterOutput("Unable to cancel native Android build.")
        }
        return
      }
    }

    nativeBuildService?.let { nativeService ->
      if (nativeService.isBuildInProgress) {
        buildCenterState = buildCenterState.copy(status = "Cancelling native build…")
        if (!nativeService.cancelCurrentBuild()) {
          appendBuildCenterOutput("Unable to cancel native build.")
        }
        return
      }
    }

    val buildService = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)
    if (buildService == null) {
      hideBuildCenter()
      return
    }

    buildCenterState = buildCenterState.copy(status = "Cancelling build…")
    buildService.cancelCurrentBuild().whenComplete { result, error ->
      if (error != null || result?.wasEnqueued != true) {
        ThreadUtils.runOnUiThread {
          appendBuildCenterOutput(
            "Unable to cancel build${error?.let { ": ${it.message}" } ?: "."}"
          )
        }
      }
    }
  }

  internal fun openBuildIssue(issue: BuildIssueUi) {
    val rawPath = issue.file?.trim().orEmpty()
    if (rawPath.isBlank()) return

    val projectRoot = runCatching { File(getProjectDirPath()).canonicalFile }.getOrNull()
    val rawFile = File(rawPath)
    val candidates =
      buildList {
        add(rawFile)
        if (projectRoot != null && !rawFile.isAbsolute) {
          add(File(projectRoot, rawPath))
        }
      }

    val target =
      candidates
        .asSequence()
        .map { runCatching { it.canonicalFile }.getOrNull() }
        .filterNotNull()
        .firstOrNull { it.exists() && it.isFile }
        ?: return

    val line = ((issue.line ?: 1) - 1).coerceAtLeast(0)
    val column = ((issue.column ?: 1) - 1).coerceAtLeast(0)
    val position = com.itsaky.androidide.models.Position(line, column)
    doOpenFile(target, Range(position, position))
    hideBuildCenter()
  }

  internal fun finishBuildCenter(success: Boolean, tasks: List<String>) {
    val finalState =
      buildCenterState.steps.map { step ->
        when {
          success && step.state != BuildStepState.FAILED -> step.copy(state = BuildStepState.SUCCESS)
          !success && step.state != BuildStepState.SUCCESS -> step.copy(state = BuildStepState.FAILED)
          else -> step
        }
      }
    val status = if (success) "Build successful" else "Build failed"
    buildCenterState =
      buildCenterState.copy(
        isBuilding = false,
        status = status,
        progress = if (success) 1f else buildCenterState.progress,
        steps = finalState.ifEmpty {
          tasks.mapIndexed { index, task ->
            BuildStepUi(
              id = task.ifBlank { "task-${index}" },
              title = task.ifBlank { "Build task ${index + 1}" },
              state = if (success) BuildStepState.SUCCESS else BuildStepState.FAILED,
            )
          }
        },
      )
  }


  private fun syncWorkspaceStatus(index: Int = workspaceSelectedTab) {
    val editorView = provideEditorAt(index) ?: run {
      workspaceBreadcrumbs.clear()
      workspaceStatus = workspaceStatus.copy(message = "Ready", line = 1, column = 1)
      return
    }

    val file = editorView.file
    val extension = file.extension.lowercase()
    val language =
      when (extension) {
        "kt", "kts" -> "Kotlin"
        "java" -> "Java"
        "c", "h" -> "C"
        "cc", "cpp", "cxx", "hpp", "hh" -> "C++"
        "xml" -> "XML"
        "json" -> "JSON"
        "gradle", "groovy" -> "Gradle"
        else -> extension.ifBlank { "Text" }.uppercase()
      }

    val position = runCatching { editorView.editor.cursorLSPPosition }.getOrNull()
    val root = runCatching { File(getProjectDirPath()).canonicalFile }.getOrNull()
    val target = runCatching { file.canonicalFile }.getOrNull()
    val relative =
      if (root != null && target != null) {
        target.path.removePrefix(root.path).trimStart(File.separatorChar)
      } else {
        file.name
      }

    workspaceBreadcrumbs.clear()
    relative
      .split(File.separatorChar)
      .filter { it.isNotBlank() }
      .forEach { segment -> workspaceBreadcrumbs += EditorBreadcrumb(segment) }

    val branch =
      runCatching {
        val git = Git.open(File(getProjectDirPath()))
        try {
          git.repository.branch
        } finally {
          git.close()
        }
      }.getOrDefault("-")

    workspaceStatus =
      IdeStatusBarState(
        language = language,
        abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "-",
        branch = branch,
        line = (position?.line ?: 0) + 1,
        column = (position?.column ?: 0) + 1,
        message = if (editorView.isModified) "Modified" else "Ready",
      )
  }

  override fun onTabUnselected(tab: Tab) {}

  override fun onTabReselected(tab: Tab) {
    createMenu(this, tab.view, EDITOR_FILE_TABS, true).show()
  }

  override fun onGroupClick(group: DiagnosticGroup?) {
    if (group?.file?.exists() == true && FileUtils.isUtf8(group.file)) {
      doOpenFile(group.file, null)
      hideBottomSheet()
    }
  }

  override fun onDiagnosticClick(file: File, diagnostic: DiagnosticItem) {
    doOpenFile(file, diagnostic.range)
    hideBottomSheet()
  }

  open fun appendApkLog(line: LogLine) {
    binding.bottomSheet.appendApkLog(line)
  }

  open fun handleSearchResults(map: Map<File, List<SearchResult>>?) {
    val results = map ?: emptyMap()
    setSearchResultAdapter(
      SearchListAdapter(
        results,
        { file ->
          doOpenFile(file, null)
          hideBottomSheet()
        }
      ) { match ->
        doOpenFile(match.file, match)
        hideBottomSheet()
      }
    )

    showSearchResults()
    doDismissSearchProgress()
  }

  open fun setSearchResultAdapter(adapter: SearchListAdapter) {
    binding.bottomSheet.setSearchResultAdapter(adapter)
  }

  open fun setDiagnosticsAdapter(adapter: DiagnosticsAdapter) {
    binding.bottomSheet.setDiagnosticsAdapter(adapter)
  }

  open fun hideBottomSheet() {
    if (editorBottomSheet?.state != BottomSheetBehavior.STATE_COLLAPSED) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
    }
  }

  open fun showSearchResults() {
    if (editorBottomSheet?.state != BottomSheetBehavior.STATE_EXPANDED) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    val index =
      binding.bottomSheet.pagerAdapter.findIndexOfFragmentByClass(SearchResultFragment::class.java)

    if (index >= 0 && index < binding.bottomSheet.binding.tabs.tabCount) {
      binding.bottomSheet.binding.tabs.getTabAt(index)?.select()
    }
  }

  open fun handleDiagnosticsResultVisibility(errorVisible: Boolean) {
    binding.bottomSheet.handleDiagnosticsResultVisibility(errorVisible)
  }

  open fun handleSearchResultVisibility(errorVisible: Boolean) {
    binding.bottomSheet.handleSearchResultVisibility(errorVisible)
  }

  open fun showFirstBuildNotice() {
    newMaterialDialogBuilder(this)
      .setPositiveButton(string.ok, null)
      .setTitle(string.title_first_build)
      .setMessage(string.msg_first_build)
      .setCancelable(false)
      .create()
      .show()
  }

  fun notifySyncNeeded(onConfirm: () -> Unit) {
    val buildService = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)
    if (buildService == null || buildService.isBuildInProgress) return

    flashbarBuilder(
        duration = DURATION_INDEFINITE,
        backgroundColor = resolveAttr(attr.colorSecondaryContainer),
        messageColor = resolveAttr(attr.colorOnSecondaryContainer)
      )
      .withIcon(drawable.ic_sync, colorFilter = resolveAttr(attr.colorOnSecondaryContainer))
      .message(string.msg_sync_needed)
      .positiveActionText(string.btn_sync)
      .positiveActionTapListener {
        onConfirm()
        it.dismiss()
      }
      .negativeActionText(string.btn_ignore_changes)
      .negativeActionTapListener { it.dismiss() }
      .showOnUiThread()
  }

  open fun getFileTreeFragment(): FileTreeFragment? {
    if (filesTreeFragment == null) {
      filesTreeFragment =
        supportFragmentManager.findFragmentByTag(FileTreeFragment.TAG) as FileTreeFragment?
    }
    return filesTreeFragment
  }

  fun doSetStatus(text: CharSequence, @GravityInt gravity: Int) {
    viewModel.statusText = text
    viewModel.statusGravity = gravity
  }

  private fun tryLaunchApp(packageName: String) {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    launchIntent?.let {
      try {
        startActivity(it)
      } catch (e: Throwable) {
        flashError(string.msg_app_launch_failed)
        log.error("Failed to launch application with package name '$packageName'", e)
      }
    }
  }

  private fun checkIsDestroying() {
    if (!isDestroying && isFinishing) {
      isDestroying = true
    }
  }

  private fun handleUiDesignerResult(result: ActivityResult) {
    if (result.resultCode != RESULT_OK || result.data == null) {
      log.warn("UI Designer returned invalid result", result.resultCode, result.data)
      return
    }
    val generated = result.data!!.getStringExtra(UIDesignerActivity.RESULT_GENERATED_XML)
    if (TextUtils.isEmpty(generated)) {
      log.warn("UI Designer returned blank generated XML code")
      return
    }
    val view = provideCurrentEditor()
    if (view?.editor == null) {
      log.warn("No file opened to append UI designer result")
      return
    }
    val text = view.editor.text
    val endLine = text.lineCount - 1
    text.replace(0, 0, endLine, text.getColumnCount(endLine), generated)
  }

  private fun setupDrawerToggle() {
    val toggle =
      ActionBarDrawerToggle(
        this,
        binding.editorDrawerLayout,
        binding.editorToolbar,
        string.app_name,
        string.app_name
      )

    binding.editorDrawerLayout.addDrawerListener(toggle)
    binding.startNav.setNavigationItemSelectedListener(this)
    toggle.syncState()
    binding.editorDrawerLayout.childId = binding.realContainer.id
  }

  private fun onBuildStatusChanged() {
    val visible = viewModel.isBuildInProgress || viewModel.isInitializing
    binding.buildProgressIndicator.visibility = if (visible) View.VISIBLE else View.GONE
    invalidateOptionsMenu()
  }

  private fun setupComposeWorkspace() {
    val compose = ComposeView(this)
    compose.setViewCompositionStrategy(
      ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    )
    compose.setContent {
      AndroidIDETheme {
        EditorWorkspaceTopBar(
          projectName = File(getProjectDirPath()).name.ifBlank { "Project" },
          tabs = workspaceTabs,
          selectedTab = workspaceSelectedTab,
          commands = buildWorkspaceCommands(),
          breadcrumbs = workspaceBreadcrumbs,
          statusBarState = workspaceStatus,
          onTabSelected = { index -> binding.tabs.getTabAt(index)?.select() },
          onTabClosed = { index -> closeWorkspaceTab(index) },
          onSave = { doSaveAll() },
          onExplorer = { binding.root.openDrawer(GravityCompat.END) },
          onBuild = { showBuildCenter() },
          onMore = { binding.editorToolbar.showOverflowMenu() },
        )

        if (buildCenterOpen) {
          BuildCenterDialog(
            state = buildCenterState,
            onBuild = { startBuildCenterBuild() },
            onStop = { stopBuildCenterBuild() },
            onRefresh = { refreshWorkspaceStatus() },
            onIssueClick = { openBuildIssue(it) },
            onDismiss = { hideBuildCenter() },
          )
        }

        if (toolchainManagerOpen) {
          ToolchainManagerDialog(
            snapshot = toolchainManagerSnapshot,
            onRefresh = { refreshToolchainManager() },
            onDismiss = { hideToolchainManager() },
          )
        }
      }
    }

    binding.editorToolbar.visibility = View.GONE
    binding.tabs.visibility = View.GONE
    binding.editorAppBarLayout.addView(
      compose,
      0,
      AppBarLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
    )
    workspaceComposeView = compose
  }

  private fun buildWorkspaceCommands(): List<CommandPaletteItem> {
    val commands = mutableListOf<CommandPaletteItem>()

    commands +=
      CommandPaletteItem(
        title = "Project Explorer",
        subtitle = "Open files and folders",
        icon = IdeIcons.FolderOpen,
        category = "Navigation",
        shortcut = "Ctrl+Shift+E",
        keywords = listOf("files", "tree", "project"),
        onClick = { binding.root.openDrawer(GravityCompat.END) },
      )

    commands +=
      CommandPaletteItem(
        title = "Build Center",
        subtitle = "Monitor the current build",
        icon = IdeIcons.Build,
        category = "Build",
        shortcut = "F9",
        keywords = listOf("compile", "assemble", "logs", "problems"),
        onClick = { showBuildCenter() },
      )

    commands +=
      CommandPaletteItem(
        title = "Toolchain Manager",
        subtitle = "Inspect SDK, Java and native toolchains",
        icon = IdeIcons.Settings,
        category = "Toolchain",
        keywords = listOf("sdk", "jdk", "clang", "clangd", "ndk", "toolchain"),
        onClick = { showToolchainManager() },
      )

    commands +=
      CommandPaletteItem(
        title = "Build APK",
        subtitle = "Build the Android app with the native pipeline",
        icon = IdeIcons.Build,
        category = "Build",
        shortcut = "F9",
        keywords = listOf("build", "apk", "native", "aapt2", "dex", "sign"),
        onClick = { startBuildCenterBuild() },
      )

    commands +=
      CommandPaletteItem(
        title = "Build & Install APK",
        subtitle = "Build with the native pipeline and install the result",
        icon = IdeIcons.Build,
        category = "Build",
        keywords = listOf("run", "install", "apk", "native", "debug"),
        onClick = { startBuildCenterBuild(installAfterBuild = true) },
      )

    commands +=
      CommandPaletteItem(
        title = "Native Library Build",
        subtitle = "Build only the detected C/C++ module",
        icon = IdeIcons.Code,
        category = "Build",
        keywords = listOf("native", "c", "c++", "clang", "ndk", "jni"),
        onClick = { startNativeBuild() },
      )

    commands +=
      CommandPaletteItem(
        title = "Legacy Gradle Build",
        subtitle = "Use Gradle compatibility mode for existing projects",
        icon = IdeIcons.Build,
        category = "Build",
        keywords = listOf("gradle", "assemble", "compatibility"),
        onClick = { startLegacyGradleBuild() },
      )

    commands +=
      CommandPaletteItem(
        title = "Save All",
        subtitle = "Save all open files",
        icon = IdeIcons.Save,
        category = "Editor",
        shortcut = "Ctrl+S",
        keywords = listOf("save", "files", "write"),
        onClick = { doSaveAll() },
      )

    commands +=
      CommandPaletteItem(
        title = "Find in Project",
        subtitle = "Search source files and resources",
        icon = IdeIcons.Search,
        category = "Navigation",
        shortcut = "Ctrl+Shift+F",
        keywords = listOf("search", "find", "project", "text"),
        onClick = { findInProjectDialog.show() },
      )

    commands +=
      CommandPaletteItem(
        title = "Terminal",
        subtitle = "Open the integrated terminal",
        icon = IdeIcons.Terminal,
        category = "Tools",
        keywords = listOf("shell", "console", "command"),
        onClick = { openTerminal() },
      )

    commands +=
      CommandPaletteItem(
        title = "Preferences",
        subtitle = "Open AndroidIDE settings",
        icon = IdeIcons.Settings,
        category = "Tools",
        keywords = listOf("settings", "options", "configuration"),
        onClick = {
          startActivity(Intent(this@BaseEditorActivity, PreferencesActivity::class.java))
        },
      )

    workspaceTabs.forEachIndexed { index, tab ->
      commands +=
        CommandPaletteItem(
          title = tab.title,
          subtitle = tab.subtitle ?: "Open in editor",
          icon = IdeIcons.FileOpen,
          category = "Open Files",
          keywords = listOf(tab.id, tab.title),
          onClick = { binding.tabs.getTabAt(index)?.select() },
        )
    }

    return commands
  }

  private fun syncWorkspaceTabs() {
    workspaceTabs.clear()
    val opened = getOpenedFiles()
    opened.forEachIndexed { index, file ->
      val path = file.absolutePath
      workspaceTabs +=
        WorkspaceTab(
          id = path,
          title = File(path).name,
          modified = provideEditorAt(index)?.isModified == true,
        )
    }
    workspaceSelectedTab = viewModel.getCurrentFileIndex()
    syncWorkspaceStatus(workspaceSelectedTab)
  }

  private fun setupViews() {
    viewModel._isBuildInProgress.observe(this) { onBuildStatusChanged() }
    viewModel._isInitializing.observe(this) { onBuildStatusChanged() }
    viewModel._statusText.observe(this) { binding.bottomSheet.setStatus(it.first, it.second) }

    viewModel.observeFiles(this) { files ->
      syncWorkspaceTabs()
      binding.apply {
        if (files.isNullOrEmpty()) {
          tabs.visibility = View.GONE
          viewContainer.displayedChild = 1
        } else {
          tabs.visibility = View.GONE
          viewContainer.displayedChild = 0
        }
      }
    }

    setupNoEditorView()
    setupBottomSheet()

    if (
      !app.prefManager.getBoolean(KEY_BOTTOM_SHEET_SHOWN) &&
        editorBottomSheet?.state != BottomSheetBehavior.STATE_EXPANDED
    ) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
      ThreadUtils.runOnUiThreadDelayed(
        {
          editorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
          app.prefManager.putBoolean(KEY_BOTTOM_SHEET_SHOWN, true)
        },
        1500
      )
    }
  }

  private fun setupNoEditorView() {
    binding.noEditorSummary.movementMethod = LinkMovementMethod()
    val filesSpan: ClickableSpan =
      object : ClickableSpan() {
        override fun onClick(widget: View) {
          binding.root.openDrawer(GravityCompat.END)
        }
      }
    val bottomSheetSpan: ClickableSpan =
      object : ClickableSpan() {
        override fun onClick(widget: View) {
          editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
        }
      }
    val sb = SpannableStringBuilder()
    appendClickableSpan(sb, string.msg_swipe_for_files, filesSpan)
    appendClickableSpan(sb, string.msg_swipe_for_output, bottomSheetSpan)
    binding.noEditorSummary.text = sb
  }

  private fun appendClickableSpan(
    sb: SpannableStringBuilder,
    @StringRes textRes: Int,
    span: ClickableSpan,
  ) {
    val str = getString(textRes)
    val split = str.split("@@", limit = 3)
    if (split.size != 3) {
      // Not a valid format
      sb.append(str)
      sb.append('\n')
      return
    }
    sb.append(split[0])
    sb.append(split[1], span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.append(split[2])
    sb.append('\n')
  }

  private fun setupBottomSheet() {
    editorBottomSheet = BottomSheetBehavior.from<View>(binding.bottomSheet)
    editorBottomSheet?.addBottomSheetCallback(
      object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
          if (newState == BottomSheetBehavior.STATE_EXPANDED) {
            val editor = provideCurrentEditor()
            if (editor?.editor != null) {
              editor.editor.ensureWindowsDismissed()
            }
          }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
          binding.apply {
            val editorScale = 1 - slideOffset * (1 - EDITOR_CONTAINER_SCALE_FACTOR)
            this.bottomSheet.onSlide(slideOffset)
            this.viewContainer.scaleX = editorScale
            this.viewContainer.scaleY = editorScale
          }
        }
      }
    )

    val observer: OnGlobalLayoutListener =
      object : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          binding.let {
            it.viewContainer.pivotY = 0f
            it.viewContainer.pivotX = it.viewContainer.width / 2f
            it.viewContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        }
      }

    binding.apply {
      viewContainer.viewTreeObserver.addOnGlobalLayoutListener(observer)
      bottomSheet.setOffsetAnchor(workspaceComposeView ?: editorToolbar)
    }
  }

  private fun refreshSymbolInput(editor: CodeEditorView) {
    binding.bottomSheet.refreshSymbolInput(editor)
  }

  private fun setupDiagnosticInfo() {
    val gd = GradientDrawable()
    gd.shape = GradientDrawable.RECTANGLE
    gd.setColor(-0xdededf)
    gd.setStroke(1, -0x1)
    gd.cornerRadius = 8f
    diagnosticInfoBinding?.root?.background = gd
    diagnosticInfoBinding?.root?.visibility = View.GONE
  }

  private fun setupContainers() {
    handleDiagnosticsResultVisibility(true)
    handleSearchResultVisibility(true)
  }

  private fun onSoftInputChanged() {
    invalidateOptionsMenu()
    binding.bottomSheet.onSoftInputChanged()
  }

  private fun openTerminal() {
    val intent = Intent(this, TerminalActivity::class.java)
    intent.putExtra(
      TerminalActivity.KEY_WORKING_DIRECTORY,
      Objects.requireNonNull(getProjectDirPath())
    )
    startActivity(intent)
  }

  private fun startLogReceiver() {
    try {
      val intent = Intent(this, LogReceiverService::class.java)
        .setAction(LogSender.SERVICE_ACTION)
      check(bindService(intent, logServiceConnection, BIND_AUTO_CREATE or BIND_IMPORTANT))
      log.info("LogReceiver service is being started")
    } catch (err: Throwable) {
      log.error("Failed to start LogReceiver service", err)
    }
  }

  private fun showNeedHelpDialog() {
    val builder = newMaterialDialogBuilder(this)
    builder.setTitle(string.need_help)
    builder.setMessage(string.msg_need_help)
    builder.setPositiveButton(string.ok, null)
    builder.create().show()
  }

  open fun installationSessionCallback(): SessionCallback {
    return ApkInstallationSessionCallback(this).also { installationCallback = it }
  }
}
