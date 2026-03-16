package com.example.orbitai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.*
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SETTINGS HUB
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private enum class SettingsDestination {
    HUB, MODEL, INFERENCE, MEMORY, RAG, DEVELOPER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    downloadViewModel: DownloadViewModel,
    memoryViewModel:   MemoryViewModel,
    onNavigate:        (String) -> Unit = {},   // for external nav if needed
    onBack:            () -> Unit,
) {
    val context        = LocalContext.current
    val tokenStore     = remember { TokenStore(context) }
    val inferenceStore = remember { InferenceSettingsStore(context) }
    val memoryStore    = remember { MemoryFeatureStore(context) }

    LaunchedEffect(Unit) { downloadViewModel.refreshStatus() }

    // Internal sub-screen state — keeps back stack simple
    var destination by remember { mutableStateOf(SettingsDestination.HUB) }

    // Slide transitions between hub and sub-screens
    AnimatedContent(
        targetState   = destination,
        transitionSpec = {
            if (targetState == SettingsDestination.HUB) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "settings_nav",
    ) { dest ->
        when (dest) {
            SettingsDestination.HUB -> SettingsHub(
                downloadViewModel = downloadViewModel,
                memoryStore       = memoryStore,
                onOpenSection     = { destination = it },
                onBack            = onBack,
            )
            SettingsDestination.MODEL -> ModelSettingsScreen(
                downloadViewModel = downloadViewModel,
                onBack = { destination = SettingsDestination.HUB },
            )
            SettingsDestination.INFERENCE -> InferenceSettingsScreen(
                inferenceStore = inferenceStore,
                onBack = { destination = SettingsDestination.HUB },
            )
            SettingsDestination.MEMORY -> MemorySettingsScreen(
                memoryViewModel = memoryViewModel,
                memoryStore     = memoryStore,
                onBack = { destination = SettingsDestination.HUB },
            )
            SettingsDestination.RAG -> RagSettingsScreen(
                downloadViewModel = downloadViewModel,
                onBack = { destination = SettingsDestination.HUB },
            )
            SettingsDestination.DEVELOPER -> DeveloperSettingsScreen(
                tokenStore = tokenStore,
                downloadViewModel = downloadViewModel,
                onBack = { destination = SettingsDestination.HUB },
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HUB SCREEN — category cards
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private data class SettingsCategory(
    val destination: SettingsDestination,
    val icon:        ImageVector,
    val title:       String,
    val subtitle:    String,
    val accentColor: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHub(
    downloadViewModel: DownloadViewModel,
    memoryStore:       MemoryFeatureStore,
    onOpenSection:     (SettingsDestination) -> Unit,
    onBack:            () -> Unit,
) {
    val categories = listOf(
        SettingsCategory(
            destination  = SettingsDestination.MODEL,
            icon         = Icons.Default.DeveloperBoard,
            title        = "Model",
            subtitle     = "Download & select on-device models",
            accentColor  = VioletCore,
        ),
        SettingsCategory(
            destination  = SettingsDestination.INFERENCE,
            icon         = Icons.Default.Tune,
            title        = "Inference",
            subtitle     = "Temperature, top-K, top-P, token limits",
            accentColor  = Color(0xFF60A5FA),   // cool blue
        ),
        SettingsCategory(
            destination  = SettingsDestination.MEMORY,
            icon         = Icons.Default.Psychology,
            title        = "Memory",
            subtitle     = "View, edit & toggle stored memories",
            accentColor  = Color(0xFF34D399),   // teal
        ),
        SettingsCategory(
            destination  = SettingsDestination.RAG,
            icon         = Icons.Default.Search,
            title        = "Knowledge / RAG",
            subtitle     = "Semantic search & embedding model",
            accentColor  = Color(0xFFFBBF24),   // amber
        ),
        SettingsCategory(
            destination  = SettingsDestination.DEVELOPER,
            icon         = Icons.Default.Code,
            title        = "Developer",
            subtitle     = "HuggingFace token, custom model URL",
            accentColor  = Color(0xFFF472B6),   // pink
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to VioletGlowSoft.copy(alpha = 0.07f),
                            1.0f to Color.Transparent,
                        ),
                        radius = 600f,
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextSecondary,
                            )
                        }
                    },
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                            )
                            Text(
                                "OrbitAI",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color        = VioletBright,
                                    letterSpacing = 1.5.sp,
                                    fontWeight   = FontWeight.SemiBold,
                                ),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding(),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier        = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding  = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = 8.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(categories.indices.toList()) { index ->
                    StaggeredFadeSlide(index = index) {
                        SettingsCategoryCard(
                            category  = categories[index],
                            onClick   = { onOpenSection(categories[index].destination) },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    // Footer
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "OrbitAI • On-device • MediaPipe LLM",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick:  () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            color       = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                20f, 0f, 2f,
                                category.accentColor.copy(alpha = 0.12f).toArgb(),
                            )
                        }
                    }
                    canvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        16.dp.toPx(), 16.dp.toPx(), paint,
                    )
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite8)
            .background(
                brush = Brush.linearGradient(
                    listOf(GlassBorder, GlassBorder.copy(0.03f))
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
    ) {
        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(category.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = category.icon,
                    contentDescription = null,
                    tint               = category.accentColor,
                    modifier           = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = category.title,
                    style     = MaterialTheme.typography.titleMedium,
                    color     = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = category.subtitle,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint     = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUB-SCREEN SCAFFOLD — shared shell for all settings sub-screens
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSubScreen(
    title:    String,
    icon:     ImageVector,
    accent:   Color = VioletCore,
    onBack:   () -> Unit,
    content:  @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextSecondary,
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
                            }
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding(),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier        = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding  = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = 8.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MODEL SUB-SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ModelSettingsScreen(
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
) {
    val progressMap by downloadViewModel.progress.collectAsState()

    SettingsSubScreen(
        title  = "Model",
        icon   = Icons.Default.Memory,
        accent = VioletCore,
        onBack = onBack,
    ) {
        SettingsDescription("Download models to chat on-device. No internet needed after download.")

        AVAILABLE_MODELS.forEach { model ->
            ModelDownloadCard(
                model    = model,
                progress = progressMap[model.id],
                onDownload = { url -> downloadViewModel.startDownload(model, url) },
                onCancel   = { downloadViewModel.cancelDownload(model) },
                onDelete   = { downloadViewModel.deleteModel(model) },
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// INFERENCE SUB-SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun InferenceSettingsScreen(
    inferenceStore: InferenceSettingsStore,
    onBack: () -> Unit,
) {
    val saved            = remember { inferenceStore.get() }
    var temperature      by remember { mutableFloatStateOf(saved.temperature) }
    var topK             by remember { mutableIntStateOf(saved.topK) }
    var topP             by remember { mutableFloatStateOf(saved.topP) }
    var maxDecodedTokens by remember { mutableIntStateOf(saved.maxDecodedTokens) }
    var isDirty          by remember { mutableStateOf(false) }
    var saved2           by remember { mutableStateOf(false) }

    SettingsSubScreen(
        title  = "Inference",
        icon   = Icons.Default.Tune,
        accent = Color(0xFF60A5FA),
        onBack = onBack,
    ) {
        SettingsDescription("Controls how the AI generates responses. Changes take effect on the next message.")

        // Sliders card
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                OrbitSlider(
                    label    = "Temperature",
                    value    = temperature,
                    valueStr = "%.2f".format(temperature),
                    range    = 0.1f..2.0f,
                    accent   = Color(0xFF60A5FA),
                    hint     = "Higher = more creative, lower = more focused",
                    onChange = { temperature = it; isDirty = true; saved2 = false },
                )
                OrbitDivider()
                OrbitSlider(
                    label    = "Top-K",
                    value    = topK.toFloat(),
                    valueStr = topK.toString(),
                    range    = 1f..100f,
                    steps    = 98,
                    accent   = Color(0xFF60A5FA),
                    hint     = "Tokens considered at each step",
                    onChange = { topK = it.toInt(); isDirty = true; saved2 = false },
                )
                OrbitDivider()
                OrbitSlider(
                    label    = "Top-P",
                    value    = topP,
                    valueStr = "%.2f".format(topP),
                    range    = 0.1f..1.0f,
                    accent   = Color(0xFF60A5FA),
                    hint     = "Nucleus sampling threshold",
                    onChange = { topP = it; isDirty = true; saved2 = false },
                )
                OrbitDivider()
                OrbitSlider(
                    label    = "Max output tokens",
                    value    = maxDecodedTokens.toFloat(),
                    valueStr = maxDecodedTokens.toString(),
                    range    = 128f..2048f,
                    steps    = 14,
                    accent   = Color(0xFF60A5FA),
                    hint     = "Maximum length of each response",
                    onChange = { maxDecodedTokens = (it / 128).toInt() * 128; isDirty = true; saved2 = false },
                )
            }
        }

        // Apply button
        OrbitPrimaryButton(
            label    = if (saved2) "✓ Applied" else "Apply Settings",
            enabled  = isDirty,
            accent   = Color(0xFF60A5FA),
            onClick  = {
                inferenceStore.save(InferenceSettings(temperature, topK, topP, maxDecodedTokens))
                isDirty = false
                saved2  = true
            },
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MEMORY SUB-SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun MemorySettingsScreen(
    memoryViewModel: MemoryViewModel,
    memoryStore:     MemoryFeatureStore,
    onBack:          () -> Unit,
) {
    var enabled by remember { mutableStateOf(memoryStore.isEnabled) }
    val memories by memoryViewModel.memories.collectAsState()

    SettingsSubScreen(
        title  = "Memory",
        icon   = Icons.Default.Psychology,
        accent = Color(0xFF34D399),
        onBack = onBack,
    ) {
        SettingsDescription("When enabled, OrbitAI saves and uses memories from your conversations.")

        // Toggle card
        GlassCard {
            Row(
                modifier             = Modifier.fillMaxWidth(),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Use Memory",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (enabled) "Memory is active" else "Memory is off",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) Color(0xFF34D399) else TextMuted,
                    )
                }
                Switch(
                    checked          = enabled,
                    onCheckedChange  = { enabled = it; memoryStore.isEnabled = it },
                    colors           = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = Color(0xFF34D399),
                        uncheckedTrackColor = GlassWhite8,
                    ),
                )
            }
        }

        if (memories.isNotEmpty()) {
            SettingsSectionLabel("Stored Memories")

            memories.forEach { memory ->
                MemoryItemCard(
                    memory   = memory,
                    onDelete = { memoryViewModel.deleteMemory(memory.id) },
                )
            }
        } else if (enabled) {
            GlassCard {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint     = Color(0xFF34D399).copy(0.5f),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "No memories stored yet. Chat to build memories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryItemCard(
    memory:   com.example.orbitai.data.db.MemoryEntity,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite4)
            .background(
                brush = Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(0.02f))),
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment    = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34D399).copy(0.7f)),
            )
            Text(
                text     = memory.content,
                style    = MaterialTheme.typography.bodyMedium,
                color    = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete memory",
                    tint     = TextMuted.copy(0.4f),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// RAG / KNOWLEDGE SUB-SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun RagSettingsScreen(
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
) {
    val embeddingProgressMap by downloadViewModel.embeddingProgress.collectAsState()

    SettingsSubScreen(
        title  = "Knowledge / RAG",
        icon   = Icons.Default.Search,
        accent = Color(0xFFFBBF24),
        onBack = onBack,
    ) {
        SettingsDescription("Download the Gecko embedding model to enable semantic (meaning-based) search in your documents. Without it, keyword search is used.")

        AVAILABLE_EMBEDDING_MODELS.forEach { model ->
            EmbeddingModelDownloadCard(
                model    = model,
                progress = embeddingProgressMap[model.id],
                onDownload = { downloadViewModel.startEmbeddingDownload(model) },
                onCancel   = { downloadViewModel.cancelEmbeddingDownload(model) },
                onDelete   = { downloadViewModel.deleteEmbeddingModel(model) },
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// DEVELOPER SUB-SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun DeveloperSettingsScreen(
    tokenStore:        TokenStore,
    downloadViewModel: DownloadViewModel,
    onBack:            () -> Unit,
) {
    SettingsSubScreen(
        title  = "Developer",
        icon   = Icons.Default.Code,
        accent = Color(0xFFF472B6),
        onBack = onBack,
    ) {
        SettingsDescription("HuggingFace token is required to download Gemma models. Custom URL lets you sideload any compatible model.")

        HuggingFaceTokenCard(tokenStore)

        SettingsSectionLabel("Custom Model URL")

        CustomUrlCard { url, fileName ->
            val normalizedFileName = normalizeModelFileName(fileName)
            val custom = LlmModel(
                id          = "custom-${System.currentTimeMillis()}",
                displayName = normalizedFileName
                    .removeSuffix(".litertlm")
                    .removeSuffix(".task"),
                fileName    = normalizedFileName,
                description = "Custom model",
                paramCount  = "?",
                format      = inferModelFormat(normalizedFileName),
            )
            downloadViewModel.startDownload(custom, url)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SHARED UI COMPONENTS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite8)
            .background(
                brush = Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(0.03f))),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsDescription(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
    )
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        style         = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.5.sp,
            fontWeight    = FontWeight.Bold,
        ),
        color         = VioletBright.copy(alpha = 0.7f),
        modifier      = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun OrbitDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GlassBorder.copy(alpha = 0.5f)),
    )
}

@Composable
private fun OrbitSlider(
    label:   String,
    value:   Float,
    valueStr: String,
    range:   ClosedFloatingPointRange<Float>,
    accent:  Color,
    hint:    String = "",
    steps:   Int    = 0,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                if (hint.isNotEmpty()) {
                    Text(hint, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            // Value badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    valueStr,
                    style      = MaterialTheme.typography.labelLarge,
                    color      = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Slider(
            value         = value,
            onValueChange = onChange,
            valueRange    = range,
            steps         = steps,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor         = accent,
                activeTrackColor   = accent,
                inactiveTrackColor = GlassWhite8,
            ),
        )
    }
}

@Composable
private fun OrbitPrimaryButton(
    label:   String,
    enabled: Boolean,
    accent:  Color  = VioletCore,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled)
                    Brush.linearGradient(listOf(accent.copy(0.9f), accent.copy(0.7f)))
                else
                    Brush.linearGradient(listOf(GlassWhite8, GlassWhite8))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                enabled           = enabled,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.titleMedium,
            color      = if (enabled) Color.White else TextMuted,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OrbitDestructiveButton(
    label:   String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DestructiveSoft)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.labelLarge,
            color      = Destructive,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HuggingFace Token Card
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun HuggingFaceTokenCard(tokenStore: TokenStore) {
    var token     by remember { mutableStateOf(tokenStore.huggingFaceToken) }
    var showToken by remember { mutableStateOf(false) }
    var saved     by remember { mutableStateOf(tokenStore.hasToken()) }

    GlassCard {
        // Status row
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (saved) Color(0xFF34D399) else TextMuted.copy(0.4f)),
            )
            Text(
                if (saved) "Token saved" else "No token set",
                style  = MaterialTheme.typography.labelLarge,
                color  = if (saved) Color(0xFF34D399) else TextMuted,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Token field
        TextField(
            value         = token,
            onValueChange = { token = it; saved = false },
            modifier      = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassWhite4),
            placeholder   = {
                Text(
                    "hf_xxxxxxxxxxxxxxxx",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = TextMuted,
                )
            },
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                cursorColor             = Color(0xFFF472B6),
            ),
            textStyle            = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
            ),
            singleLine           = true,
            visualTransformation = if (showToken) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint     = TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Done,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrbitPrimaryButton(
                label   = if (saved) "✓ Saved" else "Save Token",
                enabled = token.isNotBlank() && !saved,
                accent  = Color(0xFFF472B6),
                onClick = {
                    tokenStore.huggingFaceToken = token
                    saved = true
                },
            )
            if (saved) {
                OrbitDestructiveButton(
                    label   = "Clear",
                    onClick = {
                        token = ""
                        tokenStore.huggingFaceToken = ""
                        saved = false
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "1. Go to huggingface.co/settings/tokens\n" +
            "2. Create a token with read access\n" +
            "3. Accept each model's license on HuggingFace",
            style      = MaterialTheme.typography.bodySmall,
            color      = TextMuted.copy(0.65f),
            lineHeight = 18.sp,
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Model Download Card
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ModelDownloadCard(
    model:      LlmModel,
    progress:   DownloadProgress?,
    onDownload: (customUrl: String?) -> Unit,
    onCancel:   () -> Unit,
    onDelete:   () -> Unit,
) {
    val status       = progress?.status ?: DownloadStatus.IDLE
    val isDownloaded = status == DownloadStatus.COMPLETED
    val isActive     = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED
    var showUrl      by remember { mutableStateOf(false) }
    var customUrl    by remember { mutableStateOf(MODEL_DOWNLOAD_URLS[model.id] ?: "") }

    val accent = if (isDownloaded) Color(0xFF34D399) else VioletCore

    GlassCard {
        // Header row
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Param count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    model.paramCount,
                    style      = MaterialTheme.typography.labelLarge,
                    color      = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            // Format badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassWhite8)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    model.format.badgeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    model.displayName,
                    style  = MaterialTheme.typography.titleMedium,
                    color  = TextPrimary,
                )
                Text(
                    model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            when {
                isDownloaded -> Icon(Icons.Default.CheckCircle, null,
                    tint = Color(0xFF34D399), modifier = Modifier.size(22.dp))
                isActive     -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), color = VioletBright, strokeWidth = 2.dp)
                else         -> Icon(Icons.Default.CloudDownload, null,
                    tint = TextMuted.copy(0.5f), modifier = Modifier.size(22.dp))
            }
        }

        // Progress bar
        if (isActive && (progress?.totalBytes ?: 0L) > 0L) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress   = { progress!!.progress },
                modifier   = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color      = VioletBright,
                trackColor = GlassWhite8,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${progress!!.progressPercent}% · ${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Text(
                    "Downloading…",
                    style = MaterialTheme.typography.labelSmall,
                    color = VioletBright,
                )
            }
        }

        // Error
        if (status == DownloadStatus.FAILED && progress?.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(progress.error,
                style = MaterialTheme.typography.bodySmall,
                color = Destructive,
                lineHeight = 16.sp,
            )
        }

        // Custom URL expand
        AnimatedVisibility(
            visible = showUrl && !isDownloaded && !isActive,
            enter   = expandVertically(),
            exit    = shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                TextField(
                    value         = customUrl,
                    onValueChange = { customUrl = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassWhite4),
                    placeholder   = {
                        Text("https://…/model.task", style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace), color = TextMuted)
                    },
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        cursorColor             = VioletBright,
                    ),
                    textStyle     = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines      = 3,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            when {
                isDownloaded -> OrbitDestructiveButton(label = "Delete", onClick = onDelete)
                isActive -> Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassWhite8)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onCancel,
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OrbitGradients.primaryButton)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                            ) { onDownload(customUrl.takeIf { showUrl && it.isNotBlank() }) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.White,
                                modifier = Modifier.size(15.dp))
                            Text("Download", style = MaterialTheme.typography.labelLarge,
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    // URL toggle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (showUrl) VioletFrost else GlassWhite8)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                            ) { showUrl = !showUrl },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (showUrl) Icons.Default.LinkOff else Icons.Default.Link,
                            contentDescription = "Custom URL",
                            tint     = if (showUrl) VioletBright else TextMuted,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Embedding Model Download Card
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun EmbeddingModelDownloadCard(
    model:      EmbeddingModelConfig,
    progress:   DownloadProgress?,
    onDownload: () -> Unit,
    onCancel:   () -> Unit,
    onDelete:   () -> Unit,
) {
    val status       = progress?.status ?: DownloadStatus.IDLE
    val isDownloaded = status == DownloadStatus.COMPLETED
    val isActive     = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED
    val accent       = Color(0xFFFBBF24)

    GlassCard {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("~8 MB", style = MaterialTheme.typography.labelLarge,
                    color = accent, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(model.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(model.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            when {
                isDownloaded -> Icon(Icons.Default.CheckCircle, null,
                    tint = Color(0xFF34D399), modifier = Modifier.size(22.dp))
                isActive     -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                else         -> Icon(Icons.Default.CloudDownload, null,
                    tint = TextMuted.copy(0.5f), modifier = Modifier.size(22.dp))
            }
        }

        if (isDownloaded) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34D399)))
                Text("Semantic search active — RAG queries use Gecko embeddings.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF34D399).copy(0.8f))
            }
        }

        if (isActive && (progress?.totalBytes ?: 0L) > 0L) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress   = { progress!!.progress },
                modifier   = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color      = accent,
                trackColor = GlassWhite8,
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${progress!!.progressPercent}% · ${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("Downloading…", style = MaterialTheme.typography.labelSmall, color = accent)
            }
        }

        if (status == DownloadStatus.FAILED && progress?.error != null) {
            Spacer(Modifier.height(6.dp))
            Text(progress.error, style = MaterialTheme.typography.bodySmall,
                color = Destructive, lineHeight = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        when {
            isDownloaded -> OrbitDestructiveButton("Delete", onDelete)
            isActive     -> Box(
                modifier = Modifier.height(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(GlassWhite8)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null, onClick = onCancel)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Cancel", style = MaterialTheme.typography.labelLarge, color = TextMuted) }
            else -> Box(
                modifier = Modifier.height(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(0.9f), accent.copy(0.7f))))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null, onClick = onDownload)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Download, null, tint = Color.White,
                        modifier = Modifier.size(15.dp))
                    Text("Download", style = MaterialTheme.typography.labelLarge,
                        color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Custom URL Card
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun CustomUrlCard(onDownload: (url: String, fileName: String) -> Unit) {
    var url      by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    GlassCard {
        Text("Any compatible Task or LiteRT-LM file",
            style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Paste any MediaPipe LLM model URL (.task or .litertlm) below.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(Modifier.height(12.dp))

        TextField(
            value         = url,
            onValueChange = { url = it },
            modifier      = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(GlassWhite4),
            placeholder   = {
                Text("https://…/model.task or model.litertlm",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextMuted)
            },
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = VioletBright,
            ),
            textStyle     = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines      = 2,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )

        Spacer(Modifier.height(8.dp))

        TextField(
            value         = fileName,
            onValueChange = { fileName = it },
            modifier      = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(GlassWhite4),
            placeholder   = {
                Text("Save as (e.g. mymodel.task)",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted)
            },
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = VioletBright,
            ),
            textStyle     = MaterialTheme.typography.bodyMedium,
            singleLine    = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
        )

        Spacer(Modifier.height(12.dp))

        OrbitPrimaryButton(
            label   = "Download Custom Model",
            enabled = url.isNotBlank() && fileName.isNotBlank(),
            onClick = {
                val name = normalizeModelFileName(fileName)
                if (url.isNotBlank() && name.isNotBlank()) onDownload(url.trim(), name)
            },
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Helpers
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun ModelFormat.badgeLabel(): String = when (this) {
    ModelFormat.TASK     -> "Task"
    ModelFormat.LITERTLM -> "LiteRT-LM"
}

private fun inferModelFormat(fileName: String): ModelFormat = when {
    fileName.lowercase().endsWith(".litertlm") -> ModelFormat.LITERTLM
    else -> ModelFormat.TASK
}

private fun normalizeModelFileName(fileName: String): String {
    val trimmed = fileName.trim()
    return when {
        trimmed.isEmpty() -> trimmed
        trimmed.lowercase().endsWith(".task") ||
        trimmed.lowercase().endsWith(".litertlm") -> trimmed
        else -> "$trimmed.task"
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L         -> "%.1f KB".format(bytes / 1_024.0)
    else                    -> "$bytes B"
}