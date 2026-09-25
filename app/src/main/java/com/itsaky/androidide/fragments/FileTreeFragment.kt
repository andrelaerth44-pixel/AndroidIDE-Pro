/*
 * This file is part of AndroidIDE.
 *
 * AndroidIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.itsaky.androidide.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.BottomSheetDialogFragment
import com.itsaky.androidide.R
import com.itsaky.androidide.adapters.viewholders.FileTreeViewHolder
import com.itsaky.androidide.eventbus.events.filetree.FileClickEvent
import com.itsaky.androidide.eventbus.events.filetree.FileLongClickEvent
import com.itsaky.androidide.events.ExpandTreeNodeRequestEvent
import com.itsaky.androidide.events.ListProjectFilesRequestEvent
import com.itsaky.androidide.projects.ProjectManager.getProjectDirPath
import com.itsaky.androidide.tasks.TaskExecutor.executeAsync
import com.itsaky.androidide.tasks.callables.FileTreeCallable
import com.itsaky.androidide.tasks.callables.FileTreeCallable.SortFileName
import com.itsaky.androidide.tasks.callables.FileTreeCallable.SortFolder
import com.itsaky.androidide.ui.compose.AndroidIDETheme
import com.itsaky.androidide.ui.compose.GlassPanel
import com.itsaky.androidide.utils.ILogger
import com.unnamed.b.atv.model.TreeNode
import com.unnamed.b.atv.model.TreeNode.TreeNodeClickListener
import com.unnamed.b.atv.model.TreeNode.TreeNodeLongClickListener
import java.io.File
import java.util.Arrays

private data class VisibleTreeNode(
  val node: TreeNode,
  val depth: Int,
)

class FileTreeFragment :
  BottomSheetDialogFragment(), TreeNodeClickListener, TreeNodeLongClickListener {

  private var mRoot: TreeNode? = null
  private var mTreeState: String? = null

  private var treeVersion by mutableIntStateOf(0)
  private var isLoading by mutableStateOf(false)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_STORED_TREE_STATE)) {
      mTreeState = savedInstanceState.getString(KEY_STORED_TREE_STATE)
    }

    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AndroidIDETheme {
          GlassPanel(
            modifier = Modifier.fillMaxSize(),
          ) {
            Column(modifier = Modifier.fillMaxSize()) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = getString(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = File(getProjectDirPath()).name.ifBlank { "Project" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                  )
                } else {
                  IconButton(onClick = { listProjectFiles() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                  }
                }
              }

              val visibleNodes = buildVisibleTree()
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                items(
                  items = visibleNodes,
                  key = { it.node.path },
                ) { item ->
                  FileTreeRow(
                    node = item.node,
                    depth = item.depth,
                    onClick = { onClick(item.node, item.node.value) },
                    onLongClick = { onLongClick(item.node, item.node.value) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    listProjectFiles()
  }

  override fun onStart() {
    super.onStart()
    if (!org.greenrobot.eventbus.EventBus.getDefault().isRegistered(this)) {
      org.greenrobot.eventbus.EventBus.getDefault().register(this)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    saveTreeState()
    outState.putString(KEY_STORED_TREE_STATE, mTreeState)
    super.onSaveInstanceState(outState)
  }

  override fun onStop() {
    super.onStop()
    org.greenrobot.eventbus.EventBus.getDefault().unregister(this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    mRoot = null
  }

  fun saveTreeState() {
    val root = mRoot
    if (root == null) {
      mTreeState = null
      return
    }

    val expanded = mutableListOf<String>()
    collectExpandedNodes(root, expanded)
    mTreeState = expanded.joinToString(AndroidIDE_STATE_SEPARATOR)
  }

  private fun collectExpandedNodes(node: TreeNode, output: MutableList<String>) {
    for (child in node.children) {
      if (child.isExpanded) {
        output += child.path
        collectExpandedNodes(child, output)
      }
    }
  }

  override fun onClick(node: TreeNode, p2: Any) {
    val file = p2 as File
    if (!file.exists()) {
      return
    }

    if (file.isDirectory) {
      if (node.isExpanded) {
        collapseNode(node)
      } else {
        setLoading(node)
        listNode(node) { expandNode(node) }
      }
    }

    val event = FileClickEvent(file)
    event.put(Context::class.java, requireContext())
    org.greenrobot.eventbus.EventBus.getDefault().post(event)
  }

  private fun collapseNode(node: TreeNode) {
    node.isExpanded = false
    invalidateComposeTree()
  }

  private fun expandNode(node: TreeNode) {
    node.isExpanded = true
    invalidateComposeTree()
  }

  private fun setLoading(node: TreeNode) {
    if (node.viewHolder is FileTreeViewHolder) {
      (node.viewHolder as FileTreeViewHolder).setLoading(true)
    }
  }

  private fun listNode(node: TreeNode, whenDone: Runnable) {
    node.children.clear()
    node.isExpanded = false
    invalidateComposeTree()

    executeAsync({
      listFilesForNode(node.value.listFiles() ?: return@executeAsync null, node)
      var temp = node
      while (temp.size() == 1) {
        temp = temp.childAt(0)
        if (!temp.value.isDirectory) {
          break
        }
        listFilesForNode(temp.value.listFiles() ?: continue, temp)
        temp.isExpanded = true
      }
      null
    }) {
      whenDone.run()
      invalidateComposeTree()
    }
  }

  private fun listFilesForNode(files: Array<File>, parent: TreeNode) {
    Arrays.sort(files, SortFileName())
    Arrays.sort(files, SortFolder())
    for (file in files) {
      val node = TreeNode(file)
      node.viewHolder = FileTreeViewHolder(context)
      parent.addChild(node)
    }
  }

  override fun onLongClick(node: TreeNode, value: Any): Boolean {
    val event = FileLongClickEvent(value as File)
    event.put(Context::class.java, requireContext())
    event.put(TreeNode::class.java, node)
    org.greenrobot.eventbus.EventBus.getDefault().post(event)
    return true
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
  fun onGetListFilesRequested(event: ListProjectFilesRequestEvent?) {
    if (!isVisible || context == null) return
    listProjectFiles()
  }

  @Suppress("unused")
  @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
  fun onGetExpandTreeNodeRequest(event: ExpandTreeNodeRequestEvent) {
    if (!isVisible || context == null) return
    expandNode(event.node)
  }

  fun listProjectFiles() {
    if (context == null) return

    isLoading = true
    treeVersion++

    val projectDir = File(getProjectDirPath())
    mRoot = TreeNode(File(""))
    mRoot!!.viewHolder = FileTreeViewHolder(requireContext())

    val projectRoot = TreeNode.root(projectDir)
    projectRoot.viewHolder = FileTreeViewHolder(requireContext())
    mRoot!!.addChild(projectRoot)

    executeAsync(FileTreeCallable(context, projectRoot, projectDir)) {
      if (context == null) return@executeAsync

      isLoading = false
      treeVersion++

      val root = mRoot
      if (root != null && root.children.isNotEmpty()) {
        tryRestoreState()
      }
      invalidateComposeTree()
    }
  }

  private fun tryRestoreState() {
    val openNodes =
      mTreeState
        ?.split(AndroidIDE_STATE_SEPARATOR)
        ?.filter { it.isNotBlank() }
        ?.toHashSet()
        ?: hashSetOf()

    mRoot?.childAt(0)?.let { projectRoot ->
      if (!projectRoot.isExpanded) {
        expandNode(projectRoot)
      }
      restoreNodeState(projectRoot, openNodes)
    }
  }

  private fun restoreNodeState(root: TreeNode, openNodes: Set<String>) {
    for (node in root.children) {
      if (openNodes.contains(node.path) && node.value.isDirectory) {
        listNode(node) {
          expandNode(node)
          restoreNodeState(node, openNodes)
        }
      }
    }
  }

  private fun buildVisibleTree(): List<VisibleTreeNode> {
    // Touch state so Compose observes asynchronous tree changes.
    treeVersion

    val root = mRoot ?: return emptyList()
    val result = mutableListOf<VisibleTreeNode>()

    fun append(node: TreeNode, depth: Int) {
      result += VisibleTreeNode(node, depth)
      if (node.isExpanded) {
        for (child in node.children) {
          append(child, depth + 1)
        }
      }
    }

    for (child in root.children) {
      append(child, 0)
    }

    return result
  }

  private fun invalidateComposeTree() {
    treeVersion++
  }

  companion object {
    const val TAG = "editor.fileTree"
    private const val KEY_STORED_TREE_STATE = "fileTree_state"
    private const val ANDROIDIDE_STATE_SEPARATOR = "\n"
    private val LOG = ILogger.newInstance("FileTreeFragment")

    @JvmStatic
    fun newInstance(): FileTreeFragment = FileTreeFragment()
  }
}

@androidx.compose.runtime.Composable
private fun FileTreeRow(
  node: TreeNode,
  depth: Int,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val file = node.value

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(start = (depth * 14).dp)
        .combinedClickable(
          onClick = onClick,
          onLongClick = onLongClick,
        )
        .padding(horizontal = 10.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (file.isDirectory) {
      Icon(
        imageVector =
          if (node.isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
    } else {
      Spacer(Modifier.width(18.dp))
    }

    Spacer(Modifier.width(4.dp))

    Icon(
      imageVector = fileIcon(file),
      contentDescription = null,
      modifier = Modifier.size(19.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.width(10.dp))

    Text(
      text = file.name.ifBlank { "Project" },
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
    )
  }
}

private fun fileIcon(file: File): ImageVector {
  if (file.isDirectory) return Icons.Outlined.Folder

  return when (file.extension.lowercase()) {
    "kt", "kts", "java", "cpp", "c", "h", "xml", "json", "gradle" -> Icons.Outlined.Code
    else -> Icons.Outlined.Description
  }
}
