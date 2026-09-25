package com.itsaky.androidide.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.itsaky.androidide.activities.MainActivity
import com.itsaky.androidide.activities.PreferencesActivity
import com.itsaky.androidide.activities.TerminalActivity
import com.itsaky.androidide.common.databinding.LayoutDialogProgressBinding
import com.itsaky.androidide.preferences.databinding.LayoutDialogTextInputBinding
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.resources.R.string
import com.itsaky.androidide.tasks.executeAsyncProvideError
import com.itsaky.androidide.ui.compose.AndroidIDEProHome
import com.itsaky.androidide.ui.compose.IdeIcons
import com.itsaky.androidide.ui.compose.MainScreenAction
import com.itsaky.androidide.utils.DialogUtils
import com.itsaky.androidide.utils.Environment
import com.itsaky.androidide.utils.ILogger
import com.itsaky.androidide.utils.flashError
import com.itsaky.androidide.utils.flashSuccess
import com.itsaky.androidide.viewmodel.MainViewModel
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.File

class MainFragment : BaseFragment() {

  private val viewModel by viewModels<MainViewModel>(
    ownerProducer = { requireActivity() })

  private val log = ILogger.newInstance("MainFragment")

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AndroidIDEProHome(
          actions =
            listOf(
              MainScreenAction(string.create_project, IdeIcons.Add) { showCreateProject() },
              MainScreenAction(string.msg_open_existing_project, IdeIcons.FolderOpen) {
                pickDirectory(this@MainFragment::openProject)
              },
              MainScreenAction(string.git_clone_repo, IdeIcons.File) { cloneGitRepo() },
              MainScreenAction(string.title_terminal, IdeIcons.Terminal) {
                startActivity(Intent(requireActivity(), TerminalActivity::class.java))
              },
              MainScreenAction(string.msg_preferences, IdeIcons.Settings) {
                gotoPreferences()
              },
              MainScreenAction(string.btn_docs, Icons.Outlined.Description) {
                requireActivity().let { activity ->
                  (activity.application as com.itsaky.androidide.app.BaseApplication).openDocs()
                }
              },
              MainScreenAction(string.btn_donate, IdeIcons.Favorite) {
                requireActivity().let { activity ->
                  (activity.application as com.itsaky.androidide.app.BaseApplication).openSponsors()
                }
              },
            ),
        )
      }
    }
  }

  private fun showCreateProject() {
    viewModel.setScreen(MainViewModel.SCREEN_TEMPLATE_LIST)
  }

  fun openProject(root: File) {
    (requireActivity() as MainActivity).openProject(root)
  }

  private fun cloneGitRepo() {
    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    val binding = LayoutDialogTextInputBinding.inflate(layoutInflater)
    binding.name.setHint(string.git_clone_repo_url)

    builder.setView(binding.root)
    builder.setTitle(string.git_clone_repo)
    builder.setCancelable(true)
    builder.setPositiveButton(string.git_clone) { dialog, _ ->
      dialog.dismiss()
      doClone(binding.name.editText?.text?.toString())
    }
    builder.setNegativeButton(android.R.string.cancel, null)
    builder.show()
  }

  private fun doClone(repo: String?) {
    if (repo.isNullOrBlank()) {
      log.warn("Unable to clone repo. Invalid repo URL : '$repo'")
      return
    }

    var url = repo.trim()
    if (!url.endsWith(".git")) {
      url += ".git"
    }

    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    val binding = LayoutDialogProgressBinding.inflate(layoutInflater)

    binding.message.visibility = View.VISIBLE

    builder.setTitle(string.git_clone_in_progress)
    builder.setMessage(url)
    builder.setView(binding.root)
    builder.setCancelable(false)

    val repoName = url.substringAfterLast('/').substringBeforeLast(".git")
    val targetDir = File(Environment.PROJECTS_DIR, repoName)

    val progress = GitCloneProgressMonitor(binding.progress, binding.message)
    var git: Git? = null
    val future =
      executeAsyncProvideError(
        {
          return@executeAsyncProvideError Git.cloneRepository()
            .setURI(url)
            .setDirectory(targetDir)
            .setProgressMonitor(progress)
            .call()
            .also { git = it }
        },
        { _, _ -> },
      )

    builder.setPositiveButton(android.R.string.cancel) { iface, _ ->
      iface.dismiss()
      progress.cancel()
      git?.close()
      future.cancel(true)
    }

    val dialog = builder.show()

    future.whenComplete { result, error ->
      ThreadUtils.runOnUiThread {
        dialog?.dismiss()
        result?.close()
        if (result == null || error != null) {
          if (!future.isCancelled) {
            showCloneError(error)
          }
        } else {
          flashSuccess(string.git_clone_success)
        }
      }
    }
  }

  private fun showCloneError(error: Throwable?) {
    if (error == null) {
      flashError(string.git_clone_failed)
      return
    }

    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    builder.setTitle(string.git_clone_failed)
    builder.setMessage(error.localizedMessage)
    builder.setPositiveButton(android.R.string.ok, null)
    builder.show()
  }

  private fun gotoPreferences() {
    startActivity(Intent(requireActivity(), PreferencesActivity::class.java))
  }

  class GitCloneProgressMonitor(
    private val progress: LinearProgressIndicator,
    private val message: android.widget.TextView,
  ) : ProgressMonitor {

    @Volatile
    private var cancelled = false

    fun cancel() {
      cancelled = true
    }

    override fun start(totalTasks: Int) {
      ThreadUtils.runOnUiThread { progress.max = totalTasks }
    }

    override fun beginTask(title: String?, totalWork: Int) {
      ThreadUtils.runOnUiThread { message.text = title }
    }

    override fun update(completed: Int) {
      ThreadUtils.runOnUiThread { progress.progress = completed }
    }

    override fun endTask() = Unit

    override fun isCancelled(): Boolean {
      return cancelled || Thread.currentThread().isInterrupted
    }
  }
}
