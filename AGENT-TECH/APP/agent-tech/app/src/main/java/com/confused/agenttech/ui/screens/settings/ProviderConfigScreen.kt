package com.confused.agenttech.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.confused.agenttech.AppContainer
import com.confused.agenttech.common.Logger
import com.confused.agenttech.database.entity.ProviderEntity
import com.confused.agenttech.designsystem.components.GlassCard
import com.confused.agenttech.designsystem.components.pressScale
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProviderConfigScreen(
    hazeState: HazeState,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val shapes = LocalShapes.current
    val scope = rememberCoroutineScope()

    val providers by AppContainer.providerRepository.observeAll().collectAsState(initial = emptyList())
    val activeProvider by AppContainer.providerRepository.observeActive().collectAsState(initial = null)
    val editing = activeProvider ?: providers.firstOrNull()

    var name by remember(editing?.id) { mutableStateOf(editing?.name ?: "") }
    var apiKey by remember(editing?.id) { mutableStateOf(editing?.apiKey ?: "") }
    var baseUrl by remember(editing?.id) { mutableStateOf(editing?.baseUrl ?: "") }
    var modelName by remember(editing?.id) { mutableStateOf(editing?.modelName ?: "") }
    var contextWindow by remember(editing?.id) { mutableStateOf((editing?.contextWindow ?: 8192L).toString()) }
    var maxTokens by remember(editing?.id) { mutableStateOf((editing?.maxTokens ?: 4096L).toString()) }
    var temperature by remember(editing?.id) { mutableStateOf((editing?.temperature ?: 0.7f).toString()) }
    var inputPrice by remember(editing?.id) { mutableStateOf((editing?.inputPricePer1K ?: 0f).toString()) }
    var outputPrice by remember(editing?.id) { mutableStateOf((editing?.outputPricePer1K ?: 0f).toString()) }
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Field("Name") { TextField(value = name, onValueChange = { name = it }) }
                    Field("API Key") {
                        TextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            obscured = !showKey,
                            trailing = {
                                Box(
                                    Modifier
                                        .clip(shapes.small)
                                        .background(colors.surfaceVariant)
                                        .pressScale { showKey = !showKey }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    BasicText(
                                        text = if (showKey) "hide" else "view",
                                        style = typography.caption.copy(color = colors.blue),
                                    )
                                }
                            },
                        )
                    }
                    Field("Base URL") { TextField(value = baseUrl, onValueChange = { baseUrl = it }) }
                    Field("Model Name") { TextField(value = modelName, onValueChange = { modelName = it }) }
                }
            }
            item {
                SectionHeader("Advanced")
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Field("Context Window") { TextField(value = contextWindow, onValueChange = { contextWindow = it }) }
                    Field("Max Output Tokens") { TextField(value = maxTokens, onValueChange = { maxTokens = it }) }
                    Field("Temperature") { TextField(value = temperature, onValueChange = { temperature = it }) }
                }
            }
            item {
                SectionHeader("Pricing (per 1K tokens)")
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Field("Input Price") { TextField(value = inputPrice, onValueChange = { inputPrice = it }) }
                    Field("Output Price") { TextField(value = outputPrice, onValueChange = { outputPrice = it }) }
                }
            }
            testResult?.let { result ->
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(shapes.medium)
                            .background(if (result.startsWith("✓")) colors.success else colors.error)
                            .padding(12.dp),
                    ) {
                        BasicText(
                            text = result,
                            style = typography.bodyMedium.copy(color = colors.surface),
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(shapes.pill)
                            .background(colors.surfaceVariant)
                            .pressScale {
                                if (!testing) {
                                    testing = true
                                    testResult = null
                                    scope.launch {
                                        val result: Result<String> = try {
                                            val provider = com.confused.agenttech.agent.llm.OpenAiCompatibleProvider(
                                                displayName = "test",
                                                baseUrl = baseUrl,
                                                apiKey = apiKey,
                                                modelName = modelName,
                                                temperature = temperature.toFloatOrNull() ?: 0.7f,
                                                maxTokens = maxTokens.toLongOrNull() ?: 1024L,
                                            )
                                            provider.testConnection()
                                        } catch (e: Exception) {
                                            Result.failure(e)
                                        }
                                        testResult = result.fold(
                                            onSuccess = { r -> "✓ Connection OK (echo: ${r.take(40)})" },
                                            onFailure = { "✗ ${it.message}" },
                                        )
                                        testing = false
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = if (testing) "Testing…" else "Test Connection",
                            style = typography.titleMedium.copy(color = colors.textPrimary),
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(shapes.pill)
                            .background(colors.blue)
                            .pressScale {
                                scope.launch {
                                    val id = editing?.id ?: "prov_${System.currentTimeMillis()}"
                                    val now = System.currentTimeMillis()
                                    val provider = ProviderEntity(
                                        id = id,
                                        name = name.ifBlank { "Custom" },
                                        apiKey = apiKey,
                                        baseUrl = baseUrl.ifBlank { "https://api.openai.com/v1" },
                                        modelName = modelName.ifBlank { "gpt-4o" },
                                        contextWindow = contextWindow.toLongOrNull() ?: 8192L,
                                        maxTokens = maxTokens.toLongOrNull() ?: 4096L,
                                        temperature = temperature.toFloatOrNull() ?: 0.7f,
                                        inputPricePer1K = inputPrice.toFloatOrNull() ?: 0f,
                                        outputPricePer1K = outputPrice.toFloatOrNull() ?: 0f,
                                        isActive = editing?.isActive ?: (activeProvider == null),
                                        createdAt = editing?.createdAt ?: now,
                                    )
                                    AppContainer.providerRepository.upsert(provider)
                                    if (editing == null || activeProvider == null) {
                                        AppContainer.providerRepository.setActive(id)
                                    }
                                    Logger.i("ProviderConfig", "saved provider ${provider.name}")
                                    onBack()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Save",
                            style = typography.titleMedium.copy(color = colors.surface),
                        )
                    }
                }
            }
        }

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .clip(shapes.pill)
                    .pressScale { onBack() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                BasicText(
                    text = "‹ Back",
                    style = typography.titleMedium.copy(color = colors.blue),
                )
            }
            BasicText(
                text = "Provider Config",
                style = typography.titleLarge.copy(color = colors.textPrimary),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val typography = LocalTypography.current
    val colors = LocalColors.current
    BasicText(
        text = title,
        style = typography.titleMedium.copy(color = colors.textSecondary),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun Field(
    label: String,
    content: @Composable () -> Unit,
) {
    val typography = LocalTypography.current
    val colors = LocalColors.current
    val shapes = LocalShapes.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            text = label,
            style = typography.caption.copy(color = colors.textSecondary),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(colors.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    obscured: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val typography = LocalTypography.current
    val colors = LocalColors.current

    var fieldValue by remember(value) { mutableStateOf(TextFieldValue(value)) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = fieldValue,
            onValueChange = {
                fieldValue = it
                onValueChange(it.text)
            },
            textStyle = typography.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.blue),
            visualTransformation = if (obscured) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Box(Modifier.padding(start = 8.dp)) { trailing() }
        }
    }
}
