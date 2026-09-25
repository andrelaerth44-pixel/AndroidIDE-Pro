/*
 * This file is part of AndroidIDE.
 *
 * AndroidIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 */

package com.itsaky.androidide.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.itsaky.androidide.adapters.viewholders.FileTreeViewHolder
import com.itsaky.androidide.eventbus.events.filetree.FileClickEvent
import com.itsaky.androidide.eventbus.events.filetree.FileLongClickEvent
import com.itsaky.androidide.events.ExpandTreeNodeRequestEvent
import com.itsaky.androidide.events.ListProjectFilesRequestEvent
import com.itsaky.androidide.projects.ProjectManager.getProjectDirPath
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.tasks.TaskExecutor.executeAsync
import com.itsaky.androidide.tasks.callables.FileTreeCallable
import com.itsaky.androidide.tasks.callables.FileTreeCallable.SortFileName
import com.itsaky.androidide.tasks.callables.FileTreeCallable.SortFolder
import com.itsaky.androidide.ui.compose.AndroidIDETheme
import com.itsaky.androidide.ui.compose.GlassPanel
import com.itsaky.androidide.ui.compose.IdeIcons
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
  private var selectedPath by mutableStateOf<String?>(null)

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
          var query by rememberSaveable { mutableStateOf("") }

          GlassPanel(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
              val projectDir = File(getProjectDirPath())

              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  modifier = Modifier.size(42.dp),
                  shape = RoundedCornerShape(13.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = IdeIcons.FolderOpen,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = projectDir.name.ifBlank { "Project" },
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                  ) {
                    Text(
                      text = "Project",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                  }
                }

                IconButton(
                  onClick = {
                    mRoot?.childAt(0)?.let { postProjectContextMenu(it, projectDir) }
                  },
                ) {
                  Icon(IdeIcons.Add, contentDescription = "New")
                }

                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                  )
                } else {
                  IconButton(onClick = { listProjectFiles() }) {
                    Icon(IdeIcons.Refresh, contentDescription = "Refresh")
                  }
                }

                IconButton(
                  onClick = {
                    mRoot?.childAt(0)?.let { postProjectContextMenu(it, projectDir) }
                  },
                ) {
                  Icon(IdeIcons.More, contentDescription = "More")
                }
              }

              Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    IdeIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(Modifier.width(9.dp))

                  Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                      Text(
                        text = "Search files",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                      )
                    }

                    BasicTextField(
                      value = query,
                      onValueChange = { query = it },
                      modifier = Modifier.fillMaxWidth(),
                      singleLine = true,
                      textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                          color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                  }

                  if (query.isNotBlank()) {
                    Text(
                      text = "×",
                      modifier = Modifier.padding(start = 8.dp),
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              Spacer(Modifier.height(8.dp))

              val visibleNodes =
                if (query.isBlank()) buildVisibleTree()
                else buildSearchTree(query)

              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (query.isBlank()) "Project" else visibleNodes.size.toString() + " result(s)",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                  text = if (query.isBlank()) "Files" else "Filtered",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
              }

              if (visibleNodes.isEmpty()) {
                Box(
                  modifier = Modifier.fillMaxSize().padding(24.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                      imageVector = if (query.isBlank()) IdeIcons.FolderOpen else IdeIcons.Search,
                      contentDescription = null,
                      modifier = Modifier.size(30.dp),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                      text = if (query.isBlank()) "No files" else "No matching files",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                      text =
                        if (query.isBlank()) "Refresh the project tree to try again."
                        else "Try another file or folder name.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 18.dp),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  items(
                    items = visibleNodes,
                    key = { it.node.path },
                  ) { item ->
                    val file = item.node.value
                    var menuExpanded by rememberSaveable(file.absolutePath) { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                      FileTreeRow(
                        node = item.node,
                        depth = item.depth,
                        selected = selectedPath == file.absolutePath,
                        onClick = {
                          menuExpanded = false
                          selectedPath = file.absolutePath
                          onClick(item.node, file)
                        },
                        onLongClick = {
                          selectedPath = file.absolutePath
                          menuExpanded = true
                        },
                      )

                      DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                      ) {
                        DropdownMenuItem(
                          text = { Text("Open") },
                          leadingIcon = {
                            Icon(IdeIcons.FileOpen, contentDescription = null)
                          },
                          onClick = {
                            menuExpanded = false
                            selectedPath = file.absolutePath
                            onClick(item.node, file)
                          },
                        )
                        DropdownMenuItem(
                          text = { Text("Copy path") },
                          leadingIcon = {
                            Icon(IdeIcons.Copy, contentDescription = null)
                          },
                          onClick = {
                            val clipboard =
                              requireContext().getSystemService(Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            clipboard.setPrimaryClip(
                              android.content.ClipData.newPlainText("Path", file.absolutePath),
                            )
                            menuExpanded = false
                          },
                        )
                        DropdownMenuItem(
                          text = { Text("Refresh project") },
                          leadingIcon = {
                            Icon(IdeIcons.Refresh, contentDescription = null)
                          },
                          onClick = {
                            menuExpanded = false
                            listProjectFiles()
                          },
                        )
                        DropdownMenuItem(
                          text = { Text("More actions") },
                          leadingIcon = {
                            Icon(IdeIcons.More, contentDescription = null)
                          },
                          onClick = {
                            menuExpanded = false
                            onLongClick(item.node, file)
                          },
                        )
                      }
                    }
                  }
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
    mTreeState = expanded.joinToString(ANDROIDIDE_STATE_SEPARATOR)
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
    if (!file.exists()) return

    if (file.isDirectory) {
      if (node.isExpanded) {
        collapseNode(node)
      } else {
        isLoading = true
        listNode(node) {
          expandNode(node)
          isLoading = false
        }
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

  private fun listNode(node: TreeNode, whenDone: Runnable) {
    node.children.clear()
    node.isExpanded = false
    invalidateComposeTree()

    executeAsync({
      listFilesForNode(node.value.listFiles() ?: return@executeAsync null, node)
      var temp = node
      while (temp.size() == 1) {
        temp = temp.childAt(0)
        if (!temp.value.isDirectory) break
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

      if (mRoot?.children?.isNotEmpty() == true) {
        tryRestoreState()
      }
      invalidateComposeTree()
    }
  }

  private fun tryRestoreState() {
    val openNodes =
      mTreeState
        ?.split(ANDROIDIDE_STATE_SEPARATOR)
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

  private fun buildSearchTree(query: String): List<VisibleTreeNode> {
    val root = mRoot ?: return emptyList()
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return buildVisibleTree()

    val result = mutableListOf<VisibleTreeNode>()

    fun collect(node: TreeNode, depth: Int) {
      for (child in node.children) {
        val file = child.value
        if (file.name.lowercase().contains(normalized)) {
          result += VisibleTreeNode(child, depth)
        }
        if (child.children.isNotEmpty()) {
          collect(child, depth + 1)
        }
      }
    }

    collect(root, 0)
    return result
  }

  private fun postProjectContextMenu(node: TreeNode, projectDir: File) {
    val event = FileLongClickEvent(projectDir)
    event.put(Context::class.java, requireContext())
    event.put(TreeNode::class.java, node)
    org.greenrobot.eventbus.EventBus.getDefault().post(event)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeRow(
  node: TreeNode,
  depth: Int,
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val file = node.value
  val rowColor =
    if (selected) {
      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
      Color.Transparent
    }

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(start = (depth * 14).dp)
        .background(rowColor, RoundedCornerShape(11.dp))
        .combinedClickable(
          onClick = onClick,
          onLongClick = onLongClick,
        )
        .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (file.isDirectory) {
      Icon(
        imageVector = if (node.isExpanded) IdeIcons.ExpandMore else IdeIcons.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      Spacer(Modifier.width(18.dp))
    }

    Spacer(Modifier.width(5.dp))

    FileTypeIcon(
      file = file,
      modifier = Modifier.size(20.dp),
    )

    Spacer(Modifier.width(10.dp))

    Text(
      text = file.name.ifBlank { "Project" },
      style =
        if (selected) {
          MaterialTheme.typography.bodyLarge
        } else {
          MaterialTheme.typography.bodyMedium
        },
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
    )
  }
}

@Composable
private fun FileTypeIcon(
  file: File,
  modifier: Modifier = Modifier,
) {
  if (file.isDirectory) {
    Icon(
      imageVector = IdeIcons.Folder,
      contentDescription = null,
      modifier = modifier,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }

  val ext = file.extension.lowercase()
  val badge =
    when (ext) {
      "java" -> "J"
      "kt", "kts" -> "K"
      "xml" -> "<>"
      "json" -> "{}"
      "gradle", "groovy" -> "G"
      "c", "h" -> "C"
      "cpp", "cc", "cxx", "hpp" -> "C++"
      else -> null
    }

  if (badge == null) {
    Icon(
      imageVector =
        when (ext) {
          "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg" -> IdeIcons.Image
          "sh", "bash", "bat", "cmd" -> IdeIcons.Terminal
          "apk", "aab" -> IdeIcons.Download
          else -> IdeIcons.File
        },
      contentDescription = null,
      modifier = modifier,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = badge,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
