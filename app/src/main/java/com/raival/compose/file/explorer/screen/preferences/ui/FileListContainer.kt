package com.raival.compose.file.explorer.screen.preferences.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.screen.preferences.constant.FilesTabFileListSize

@Composable
fun FileListContainer() {
    val prefs = globalClass.preferencesManager

    Container(title = stringResource(R.string.file_list)) {
        PreferenceItem(
            label = stringResource(R.string.file_list_size),
            supportingText = when (prefs.itemSize) {
                FilesTabFileListSize.EXTRA_SMALL.ordinal -> stringResource(R.string.extra_small)
                FilesTabFileListSize.SMALL.ordinal -> stringResource(R.string.small)
                FilesTabFileListSize.MEDIUM.ordinal -> stringResource(R.string.medium)
                FilesTabFileListSize.LARGE.ordinal -> stringResource(R.string.large)
                else -> stringResource(R.string.extra_large)
            },
            icon = Icons.Rounded.Height,
            onClick = {
                prefs.singleChoiceDialog.show(
                    title = globalClass.getString(R.string.file_list_size),
                    description = globalClass.getString(R.string.file_list_size_desc),
                    choices = listOf(
                        globalClass.getString(R.string.extra_small),
                        globalClass.getString(R.string.small),
                        globalClass.getString(R.string.medium),
                        globalClass.getString(R.string.large),
                        globalClass.getString(R.string.extra_large)
                    ),
                    selectedChoice = prefs.itemSize,
                    onSelect = { prefs.itemSize = it }
                )
            }
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            thickness = 3.dp
        )

        val columnCount = arrayListOf(
            "1", "2", "3", "4", "Auto"
        )

        PreferenceItem(
            label = stringResource(R.string.files_list_column_count),
            supportingText = if (prefs.columnCount == -1) columnCount[4] else prefs.columnCount.toString(),
            icon = Icons.AutoMirrored.Rounded.ManageSearch,
            onClick = {
                prefs.singleChoiceDialog.show(
                    title = globalClass.getString(R.string.files_list_column_count),
                    description = globalClass.getString(R.string.choose_number_of_columns),
                    choices = columnCount,
                    selectedChoice = if (prefs.columnCount == -1) 4 else columnCount.indexOf(
                        prefs.columnCount.toString()
                    ),
                    onSelect = {
                        val limit = when (columnCount[it]) {
                            columnCount[4] -> -1
                            else -> columnCount[it].toIntOrNull() ?: -1
                        }
                        prefs.columnCount = limit
                    }
                )
            }
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            thickness = 3.dp
        )

        PreferenceItem(
            label = stringResource(R.string.show_hidden_files),
            supportingText = emptyString,
            icon = Icons.Rounded.HideSource,
            switchState = prefs.showHiddenFiles,
            onSwitchChange = { prefs.showHiddenFiles = it }
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            thickness = 3.dp
        )

        PreferenceItem(
            label = stringResource(R.string.show_folder_s_content_count),
            supportingText = emptyString,
            icon = Icons.Rounded.Numbers,
            switchState = prefs.showFolderContentCount,
            onSwitchChange = { prefs.showFolderContentCount = it }
        )
    }
}