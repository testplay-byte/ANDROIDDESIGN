package com.confused.agenttech.ui.screens.projects

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.confused.agenttech.AppContainer
import com.confused.agenttech.common.Logger
import com.confused.agenttech.designsystem.components.CollapsibleHeader
import com.confused.agenttech.designsystem.components.GlassCard
import com.confused.agenttech.designsystem.components.pressScale
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalElevation
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography
import com.confused.agenttech.designsystem.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    hazeState: HazeState,
    onProjectOpened: () -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val shapes = LocalShapes.current
    val elevation = LocalElevation.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val projects by AppContainer.projectRepository.observeAll().collectAsState(initial = emptyList())

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            scope.launch {
                val name = uri.lastPathSegment?.substringAfterLast('/')?.replace("%2F", "/")
                    ?.substringAfterLast('/') ?: "Project"
                AppContainer.projectRepository.create(name = name, folderUri = uri.toString())
                Logger.i("Projects", "Project added: $name")
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            if (projects.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "No projects yet. Tap + to add your first project folder.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .pressScale(pressedScale = 0.97f) {
                                scope.launch {
                                    AppContainer.projectRepository.touch(project.id)
                                    AppContainer.updateToolContext(android.net.Uri.parse(project.folderUri))
                                    onProjectOpened()
                                }
                            },
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                BasicText(
                                    text = project.name,
                                    style = typography.titleLarge.copy(color = colors.textPrimary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                BasicText(
                                    text = formatFolder(project.folderUri),
                                    style = typography.caption.copy(color = colors.textTertiary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                BasicText(
                                    text = "Last active: " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                        .format(Date(project.lastActiveAt)),
                                    style = typography.bodySmall.copy(color = colors.textSecondary),
                                )
                            }
                            // Delete (a small "×" affordance — long-press in v2).
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceVariant)
                                    .pressScale {
                                        scope.launch {
                                            AppContainer.projectRepository.delete(project.id)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = "×",
                                    style = typography.titleLarge.copy(color = colors.textSecondary),
                                )
                            }
                        }
                    }
                }
            }
        }
        CollapsibleHeader(title = "Projects", listState = listState, hazeState = hazeState)

        // FAB "+"
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .shadow(
                    elevation = elevation.bottomNav,
                    shape = shapes.pill,
                    ambientColor = colors.blue.copy(alpha = 0.4f),
                    spotColor = colors.blue.copy(alpha = 0.5f),
                )
                .clip(shapes.pill)
                .background(colors.blue)
                .pressScale { folderPicker.launch(null) }
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Add project",
                colorFilter = ColorFilter.tint(colors.surface),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private fun formatFolder(folderUri: String): String {
    val last = folderUri.substringAfterLast('/').replace("%2F", "/").substringAfterLast('/')
    return if (last.isBlank()) folderUri else last
}
