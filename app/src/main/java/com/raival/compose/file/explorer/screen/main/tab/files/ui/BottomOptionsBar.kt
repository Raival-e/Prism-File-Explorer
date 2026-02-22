package com.raival.compose.file.explorer.screen.main.tab.files.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.app.Activity
import android.provider.OpenableColumns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.block
import com.raival.compose.file.explorer.common.detectVerticalSwipe
import com.raival.compose.file.explorer.common.ui.Space
import com.raival.compose.file.explorer.screen.main.tab.files.FilesTab
import com.raival.compose.file.explorer.screen.main.tab.files.task.CopyTask

@Composable
fun BottomOptionsBar(tab: FilesTab) {
    val state = tab.bottomOptionsBarState.collectAsState().value

    // ── Share mode: Save here banner ──────────────────────────────────────────
    if (globalClass.isShareMode && globalClass.shareUris.isNotEmpty()) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var isSaving by remember { mutableStateOf(false) }
        val fileLabel = if (globalClass.shareUris.size == 1)
            getSharedFileName(context, globalClass.shareUris[0]) ?: context.getString(R.string.unknown_file)
        else context.getString(R.string.shared_files_count, globalClass.shareUris.size)

        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = fileLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    onClick = {
                        globalClass.isShareMode = false
                        globalClass.shareUris = emptyList()
                    }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
                androidx.compose.material3.Button(
                    modifier = Modifier.weight(1f),
                enabled = !isSaving && tab.activeFolder.canWrite,
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        isSaving = true
                        val ok = saveSharedFilesToFolder(context, globalClass.shareUris, tab)
                        isSaving = false
                        if (ok) {
                            globalClass.isShareMode = false
                            globalClass.shareUris = emptyList()
                            globalClass.showMsg(R.string.file_saved_successfully)
                            tab.reloadFiles()
                        } else {
                            globalClass.showMsg(R.string.failed_to_save_file)
                        }
                    }
                }
            ) {
                if (isSaving) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Space(size = 8.dp)
                    Text(text = stringResource(R.string.saving_file))
                } else {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Rounded.SaveAlt,
                        contentDescription = null
                    )
                    Space(size = 8.dp)
                    Text(text = stringResource(R.string.save_here))
                }
            }
            } // end Row
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    AnimatedVisibility(
        visible = state.showQuickOptions && tab.selectedFiles.isNotEmpty(),
        enter = expandIn(expandFrom = Alignment.TopCenter) + slideInVertically(
            initialOffsetY = { it }),
        exit = shrinkOut(shrinkTowards = Alignment.BottomCenter) + slideOutVertically(
            targetOffsetY = { it })

    ) {
        Column {
            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .block(shape = RectangleShape)
                    .detectVerticalSwipe(
                        onSwipeUp = {
                            tab.toggleFileOptionsMenu(tab.selectedFiles[tab.selectedFiles.keys.first()]!!)
                        }
                    )
            ) {
                // Delete
                IconButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab.toggleDeleteConfirmationDialog(true)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // Cut
                IconButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        globalClass.taskManager.addTask(
                            CopyTask(
                                tab.selectedFiles.values.toList(),
                                deleteSourceFiles = true
                            )
                        )
                        tab.unselectAllFiles()
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCut, contentDescription = null)
                }

                // Copy
                IconButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        globalClass.taskManager.addTask(
                            CopyTask(
                                tab.selectedFiles.values.toList(),
                                deleteSourceFiles = false
                            )
                        )
                        tab.unselectAllFiles()
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.FileCopy, contentDescription = null)
                }

                // Rename
                IconButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab.toggleRenameDialog(true)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatColorText,
                        contentDescription = null
                    )
                }

                // Properties
                IconButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        tab.toggleFilePropertiesDialog(true)
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.Info, contentDescription = null)
                }
            }
        }
    }

    HorizontalDivider()

    Row(
        Modifier
            .fillMaxWidth()
            .block(shape = RectangleShape)
            .detectVerticalSwipe(
                onSwipeUp = {
                    tab.toggleBookmarksDialog(true)
                }
            )
    ) {
        if (state.showEmptyRecycleBinButton) {
            BottomOptionsBarButton(Icons.Rounded.DeleteSweep, stringResource(R.string.empty)) {
                tab.unselectAllFiles(false)
                tab.activeFolderContent.forEach {
                    tab.selectedFiles[it.uniquePath] = it
                }
                tab.quickReloadFiles()
                tab.toggleDeleteConfirmationDialog(true)
            }
        } else {
            BottomOptionsBarButton(Icons.Rounded.AddTask, stringResource(R.string.task)) {
                tab.toggleTasksPanel(true)
            }
        }

        BottomOptionsBarButton(Icons.Rounded.Search, stringResource(R.string.search)) {
            tab.toggleSearchPenal(true)
        }

        if (!state.showEmptyRecycleBinButton && state.showCreateNewContentButton) {
            BottomOptionsBarButton(Icons.Rounded.Add, stringResource(R.string.create)) {
                tab.toggleCreateNewFileDialog(true)
            }
        }

        if (state.showMoreOptionsButton && tab.selectedFiles.isNotEmpty()) {
            BottomOptionsBarButton(Icons.Rounded.SelectAll, stringResource(R.string.select_all)) {
                if (tab.selectedFiles.size == tab.activeFolderContent.size) {
                    tab.unselectAllFiles()
                } else {
                    tab.unselectAllFiles(false)
                    tab.activeFolderContent.forEach {
                        tab.selectedFiles[it.uniquePath] = it
                    }
                    tab.quickReloadFiles()
                }
            }

            BottomOptionsBarButton(Icons.Rounded.MoreVert, stringResource(R.string.options)) {
                tab.toggleFileOptionsMenu(tab.selectedFiles[tab.selectedFiles.keys.first()]!!)
            }
        } else {
            BottomOptionsBarButton(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort)) {
                tab.toggleSortingMenu(true)
            }

            BottomOptionsBarButton(Icons.Rounded.Bookmark, stringResource(R.string.bookmarks)) {
                tab.toggleBookmarksDialog(true)
            }
        }
    }
}

@Composable
fun RowScope.BottomOptionsBarButton(
    imageVector: ImageVector,
    text: String,
    view: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    val preferencesManager = globalClass.preferencesManager

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable {
                onClick()
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!preferencesManager.showBottomBarLabels) {
            Space(size = 4.dp)
        }

        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = imageVector,
            contentDescription = null
        )

        Space(size = 4.dp)

        if (preferencesManager.showBottomBarLabels) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        view()
    }
}

private fun getSharedFileName(context: android.content.Context, uri: android.net.Uri): String? {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return cursor.getString(idx)
        }
    }
    return uri.lastPathSegment
}

private fun saveSharedFilesToFolder(
    context: android.content.Context,
    uris: List<android.net.Uri>,
    tab: FilesTab
): Boolean {
    return try {
        val localFolder = tab.activeFolder
            as? com.raival.compose.file.explorer.screen.main.tab.files.holder.LocalFileHolder
            ?: return false
        val destDir = localFolder.file
        if (!destDir.exists() || !destDir.isDirectory) return false
        uris.forEach { uri ->
            val originalName = getSharedFileName(context, uri) ?: "shared_${System.currentTimeMillis()}"
            val destFile = getUniqueFile(destDir, originalName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { input.copyTo(it) }
            }
        }
        true
    } catch (e: Exception) { false }
}

private fun getUniqueFile(dir: java.io.File, fileName: String): java.io.File {
    val dot = fileName.lastIndexOf('.')
    val name = if (dot != -1) fileName.substring(0, dot) else fileName
    val ext = if (dot != -1) fileName.substring(dot) else ""

    var file = java.io.File(dir, fileName)
    var counter = 1
    while (file.exists()) {
        file = java.io.File(dir, "${name} Copy($counter)${ext}")
        counter++
    }
    return file
}