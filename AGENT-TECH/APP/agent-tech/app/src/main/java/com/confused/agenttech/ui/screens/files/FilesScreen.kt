package com.confused.agenttech.ui.screens.files

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.confused.agenttech.AppContainer
import com.confused.agenttech.designsystem.components.CollapsibleHeader
import com.confused.agenttech.designsystem.components.GlassCard
import com.confused.agenttech.designsystem.components.pressScale
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun FilesScreen(hazeState: HazeState) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val shapes = LocalShapes.current
    val context = LocalContext.current

    val projects by AppContainer.projectRepository.observeAll().collectAsState(initial = emptyList())
    val activeProject = projects.firstOrNull()

    var currentPath by remember { mutableStateOf(listOf<String>()) }
    var searchText by remember { mutableStateOf("") }

    val currentDir: DocumentFile? = remember(activeProject, currentPath) {
        val rootUri = activeProject?.let { Uri.parse(it.folderUri) } ?: return@remember null
        var node: DocumentFile? = DocumentFile.fromTreeUri(context, rootUri)
        for (segment in currentPath) {
            node = node?.findFile(segment)
            if (node == null) break
        }
        node
    }

    val entries: List<DocumentFile> = remember(currentDir, searchText) {
        val list = currentDir?.listFiles()?.toList()?.sortedBy { it.name } ?: emptyList()
        if (searchText.isBlank()) list else list.filter {
            (it.name ?: "").contains(searchText, ignoreCase = true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            if (activeProject == null) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Open a project to browse files.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                        )
                    }
                }
            } else {
                // Breadcrumb
                item {
                    BreadcrumbRow(
                        path = currentPath,
                        onSegmentClick = { idx ->
                            currentPath = currentPath.subList(0, idx + 1)
                        },
                        onRootClick = { currentPath = emptyList() },
                    )
                }
                items(entries, key = { it.uri.toString() }) { entry ->
                    FileRow(
                        name = entry.name ?: "(unknown)",
                        isDir = entry.isDirectory,
                        size = if (entry.isFile) entry.length() else 0L,
                        onClick = {
                            if (entry.isDirectory) {
                                currentPath = currentPath + (entry.name ?: "")
                            }
                        },
                    )
                }
            }
        }
        CollapsibleHeader(
            title = "Files",
            listState = listState,
            hazeState = hazeState,
        )
    }
}

@Composable
private fun BreadcrumbRow(
    path: List<String>,
    onSegmentClick: (Int) -> Unit,
    onRootClick: () -> Unit,
) {
    val colors = LocalColors.current
    val typography = LocalTypography.current

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.pressScale { onRootClick() }) {
            BasicText(
                text = "root",
                style = typography.caption.copy(
                    color = colors.blue,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        path.forEachIndexed { idx, segment ->
            BasicText(
                text = "/",
                style = typography.caption.copy(color = colors.textTertiary),
            )
            Box(Modifier.pressScale { onSegmentClick(idx) }) {
                BasicText(
                    text = segment,
                    style = typography.caption.copy(
                        color = colors.blue,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FileRow(
    name: String,
    isDir: Boolean,
    size: Long,
    onClick: () -> Unit,
) {
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val shapes = LocalShapes.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(pressedScale = 0.98f, onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(shapes.medium)
                    .background(if (isDir) colors.yellowMuted else colors.blueMuted),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = if (isDir) "▤" else "▤",
                    style = typography.titleMedium.copy(color = if (isDir) colors.yellowPressed else colors.bluePressed),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = name + if (isDir) "/" else "",
                    style = typography.titleMedium.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isDir) {
                    BasicText(
                        text = formatSize(size),
                        style = typography.caption.copy(color = colors.textTertiary),
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
