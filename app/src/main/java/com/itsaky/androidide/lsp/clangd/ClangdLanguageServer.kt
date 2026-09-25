package com.itsaky.androidide.lsp.clangd

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.itsaky.androidide.eventbus.events.editor.DocumentChangeEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentCloseEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentOpenEvent
import com.itsaky.androidide.lsp.api.ILanguageClient
import com.itsaky.androidide.lsp.api.ILanguageServer
import com.itsaky.androidide.lsp.api.IServerSettings
import com.itsaky.androidide.lsp.models.CodeFormatResult
import com.itsaky.androidide.lsp.models.CompletionParams
import com.itsaky.androidide.lsp.models.CompletionResult
import com.itsaky.androidide.lsp.models.DefinitionParams
import com.itsaky.androidide.lsp.models.DefinitionResult
import com.itsaky.androidide.lsp.models.DiagnosticResult
import com.itsaky.androidide.lsp.models.ExpandSelectionParams
import com.itsaky.androidide.lsp.models.FormatCodeParams
import com.itsaky.androidide.lsp.models.LSPFailure
import com.itsaky.androidide.lsp.models.ReferenceParams
import com.itsaky.androidide.lsp.models.ReferenceResult
import com.itsaky.androidide.lsp.models.SignatureHelp
import com.itsaky.androidide.lsp.models.SignatureHelpParams
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.native.lsp.ClangdCommandPlanner
import com.itsaky.androidide.native.lsp.ClangdProcessSession
import com.itsaky.androidide.native.lsp.ClangdWorkspaceSession
import com.itsaky.androidide.progress.ICancelChecker
import com.itsaky.androidide.projects.ProjectManager
import com.itsaky.androidide.projects.api.Project
import com.itsaky.androidide.toolchain.NativeToolId
import com.itsaky.androidide.toolchain.NativeToolchainLocator
import com.itsaky.androidide.utils.ILogger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.TimeUnit
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ClangdLanguageServer : ILanguageServer {

  companion object {
    const val SERVER_ID = "clangd"
  }

  private val log = ILogger.newInstance("ClangdLanguageServer")
  private var client: ILanguageClient? = null
  private var settings: IServerSettings? = null
  private val diagnostics = mutableMapOf<Path, DiagnosticResult>()
  private var workspaceRoot: File? = null
  private var workspace: ClangdWorkspaceSession? = null

  init {
    EventBus.getDefault().register(this)
  }

  override fun getServerId(): String = SERVER_ID

  override fun shutdown() {
    workspace?.close()
    workspace = null
    workspaceRoot = null
    diagnostics.clear()
    if (EventBus.getDefault().isRegistered(this)) {
      EventBus.getDefault().unregister(this)
    }
  }

  override fun connectClient(@Nullable client: ILanguageClient?) {
    this.client = client
  }

  @Nullable
  override fun getClient(): ILanguageClient? = client

  override fun applySettings(@Nullable settings: IServerSettings?) {
    this.settings = settings
  }

  override fun setupWithProject(@NonNull project: Project) {
    val moduleRoot =
      ProjectManager.getApplicationModule()?.projectDir
        ?: project.projectDir
    startWorkspace(moduleRoot)
  }

  @NonNull
  override fun complete(
    params: CompletionParams,
    cancelChecker: ICancelChecker,
  ): CompletionResult {
    if (!isNativeFile(params.file)) return CompletionResult.EMPTY
    if (cancelChecker.isCancelled) return CompletionResult.EMPTY

    val session = workspace ?: return CompletionResult.EMPTY
    return runCatching {
      session.completion(
        file = params.file.toFile(),
        line = params.position.line,
        character = params.position.column,
      ).get(2, TimeUnit.SECONDS) ?: CompletionResult.EMPTY
    }.getOrElse {
      CompletionResult.EMPTY
    }
  }

  @NonNull
  override fun findReferences(
    params: ReferenceParams,
    cancelChecker: ICancelChecker,
  ): ReferenceResult = ReferenceResult(Collections.emptyList())

  @NonNull
  override fun findDefinition(
    params: DefinitionParams,
    cancelChecker: ICancelChecker,
  ): DefinitionResult = DefinitionResult(Collections.emptyList())

  @NonNull
  override fun expandSelection(params: ExpandSelectionParams): Range = params.selection

  @NonNull
  override fun signatureHelp(params: SignatureHelpParams): SignatureHelp =
    SignatureHelp(Collections.emptyList(), -1, -1)

  @NonNull
  override fun analyze(file: Path): DiagnosticResult {
    if (!isNativeFile(file)) return DiagnosticResult.NO_UPDATE
    return diagnostics[file] ?: DiagnosticResult.NO_UPDATE
  }

  @NonNull
  override fun formatCode(params: FormatCodeParams): CodeFormatResult =
    CodeFormatResult(false, Collections.emptyList())

  override fun handleFailure(failure: LSPFailure): Boolean = false

  @Subscribe(threadMode = ThreadMode.ASYNC)
  fun onDocumentOpened(event: DocumentOpenEvent) {
    if (!isNativeFile(event.openedFile)) return
    ensureWorkspaceFor(event.openedFile)?.openFile(
      file = event.openedFile.toFile(),
      version = event.version,
      text = event.text,
    )
  }

  @Subscribe(threadMode = ThreadMode.ASYNC)
  fun onDocumentChanged(event: DocumentChangeEvent) {
    if (!isNativeFile(event.changedFile)) return
    val text =
      event.newText ?: runCatching {
        Files.readString(event.changedFile)
      }.getOrNull() ?: return
    ensureWorkspaceFor(event.changedFile)?.changeFile(
      file = event.changedFile.toFile(),
      version = event.version,
      text = text,
    )
  }

  @Subscribe(threadMode = ThreadMode.ASYNC)
  fun onDocumentClosed(event: DocumentCloseEvent) {
    if (!isNativeFile(event.closedFile)) return
    workspace?.runCatching { closeFile(event.closedFile.toFile()) }
    diagnostics.remove(event.closedFile)
  }

  private fun startWorkspace(moduleRoot: File) {
    if (workspaceRoot?.canonicalFile == moduleRoot.canonicalFile && workspace != null) return

    workspace?.close()
    workspace = null
    workspaceRoot = moduleRoot

    val toolchain = NativeToolchainLocator.locate()
    if (toolchain.tool(NativeToolId.CLANGD)?.path == null) {
      log.info("Native clangd is not installed; native language server remains inactive")
      return
    }

    val command = runCatching {
      ClangdCommandPlanner.plan(toolchain, moduleRoot)
    }.getOrElse { error ->
      log.warn("Unable to plan clangd launch", error)
      return
    }

    val processSession =
      ClangdProcessSession(
        command = command,
      )
    workspace =
      ClangdWorkspaceSession(
        processSession = processSession,
        onDiagnostics = { result ->
          diagnostics[result.file] = result
          client?.publishDiagnostics(result)
        },
      )

    runCatching {
      workspace?.start(moduleRoot)
    }.onFailure { error ->
      log.warn("Unable to start clangd", error)
      workspace?.close()
      workspace = null
    }
  }

  private fun ensureWorkspaceFor(file: Path): ClangdWorkspaceSession? {
    val moduleRoot =
      ProjectManager.findModuleForFile(file)?.projectDir
        ?: workspaceRoot
        ?: ProjectManager.getProjectDir()
    startWorkspace(moduleRoot)
    return workspace
  }

  private fun isNativeFile(file: Path): Boolean =
    when (file.toFile().extension.lowercase()) {
      "c", "h", "cc", "cpp", "cxx", "hh", "hpp", "hxx", "m", "mm" -> true
      else -> false
    }
}
