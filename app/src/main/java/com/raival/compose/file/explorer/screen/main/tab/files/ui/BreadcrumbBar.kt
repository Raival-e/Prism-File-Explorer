package com.raival.compose.file.explorer.screen.main.tab.files.ui

import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.copyToClipboard
import com.raival.compose.file.explorer.common.getIndexIf
import com.raival.compose.file.explorer.common.orIf
import com.raival.compose.file.explorer.common.showMsg
import com.raival.compose.file.explorer.screen.main.tab.files.FilesTab
import com.raival.compose.file.explorer.screen.main.tab.files.holder.VirtualFileHolder
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BreadcrumbBar(tab: FilesTab) {
    val highlightedPathListItemColor = MaterialTheme.colorScheme.primary

    if (tab.showCategories) {
        CategoriesRow(tab)
    } else if (tab.activeFolder !is VirtualFileHolder) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    tab.openFolder(tab.homeDir, false)
                }
            ) {
                Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
            }

            val animationScope = rememberCoroutineScope()

            LaunchedEffect(key1 = tab.highlightedPathSegment) {
                val index =
                    tab.currentPathSegments.getIndexIf { uniquePath == tab.highlightedPathSegment.uniquePath }
                animationScope.launch {
                    tab.currentPathSegmentsListState.scrollToItem(max(index, 0))
                }
            }

            val consumeLeftoverScroll = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // Only consume the scroll that LazyRow couldn't handle (at boundaries)
                        return Offset(available.x, 0f)
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        // Consume all horizontal velocity to prevent fling from propagating to parent
                        return Velocity(available.x, 0f)
                    }
                }
            }


            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(consumeLeftoverScroll),
                state = tab.currentPathSegmentsListState,
            ) {
                itemsIndexed(tab.currentPathSegments, key = { _, it -> it.uid }) { index, item ->
                    val isHighlighted = item.uniquePath == tab.highlightedPathSegment.uniquePath
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        tab.openFolder(
                                            item = item,
                                            rememberSelectedFiles = true,
                                        )
                                    },
                                    onLongClick = {
                                        item.uniquePath.copyToClipboard()
                                        showMsg(R.string.path_copied_to_clipboard)
                                    }
                                )
                                .padding(8.dp)
                                .alpha(0.8f),
                            text = item.displayName
                                .orIf(stringResource(id = R.string.internal_storage)) {
                                    item.uniquePath == Environment.getExternalStorageDirectory().absolutePath
                                }
                                .orIf(stringResource(id = R.string.root)) {
                                    item.uniquePath == File.separator
                                },
                            fontSize = 14.sp,
                            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                            color = if (isHighlighted) highlightedPathListItemColor else Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}
