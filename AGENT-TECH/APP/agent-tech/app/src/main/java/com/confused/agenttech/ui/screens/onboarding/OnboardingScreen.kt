package com.confused.agenttech.ui.screens.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.confused.agenttech.AppContainer
import com.confused.agenttech.common.Logger
import com.confused.agenttech.designsystem.components.pressScale
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onProjectAdded: () -> Unit,
) {
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val shapes = LocalShapes.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val projects by AppContainer.projectRepository.observeAll().collectAsState(initial = emptyList())

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            // Persist permission so we can access the folder across launches.
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
                Logger.i("Onboarding", "Project added: $name")
                onProjectAdded()
            }
        }
    }

    // Re-render safety: if projects exist, parent AppNavHost will auto-redirect.
    LaunchedEffect(projects.size) { }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Logo: red / yellow / blue glass-inspired circle
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(colors.red, colors.yellow, colors.blue),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "AT",
                    style = typography.displayMedium.copy(
                        color = colors.surface,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = "Welcome to Agent Tech",
                style = typography.displayMedium.copy(
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            )
            BasicText(
                text = "A dedicated Android AI agent that operates inside a project folder you choose.",
                style = typography.bodyMedium.copy(
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .clip(shapes.pill)
                    .background(colors.blue)
                    .pressScale { folderPicker.launch(null) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Add your first project",
                    style = typography.titleMedium.copy(color = colors.surface),
                )
            }
        }
    }
}
