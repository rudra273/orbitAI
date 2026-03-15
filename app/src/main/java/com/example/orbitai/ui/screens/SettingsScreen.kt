package com.example.orbitai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.AVAILABLE_EMBEDDING_MODELS
import com.example.orbitai.data.AVAILABLE_MODELS
import com.example.orbitai.data.DownloadProgress
import com.example.orbitai.data.DownloadStatus
import com.example.orbitai.data.EmbeddingModelConfig
import com.example.orbitai.data.InferenceSettings
import com.example.orbitai.data.InferenceSettingsStore
import com.example.orbitai.data.LlmModel
import com.example.orbitai.data.ModelFormat
import com.example.orbitai.data.MODEL_DOWNLOAD_URLS
import com.example.orbitai.data.TokenStore
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
) {
    val progressMap by downloadViewModel.progress.collectAsState()
    val embeddingProgressMap by downloadViewModel.embeddingProgress.collectAsState()
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val inferenceStore = remember { InferenceSettingsStore(context) }

    LaunchedEffect(Unit) { downloadViewModel.refreshStatus() }

    Scaffold(
        containerColor = Void,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMuted)
                    }
                },
                title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            // ── HuggingFace Token ─────────────────────────────────────────────
            item {
                SectionLabel("HuggingFace Token")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Required to download Gemma models. Get your token at huggingface.co/settings/tokens — also accept each model's license on HuggingFace.",
                    color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
                HuggingFaceTokenCard(tokenStore)
                Spacer(Modifier.height(8.dp))
            }

            // ── Inference Settings ────────────────────────────────────────────
            item {
                SectionLabel("Inference Settings")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Controls how the AI generates responses. Changes take effect on the next message.",
                    color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
                InferenceSettingsCard(inferenceStore)
                Spacer(Modifier.height(8.dp))
            }

            // ── Models ────────────────────────────────────────────────────────
            item {
                SectionLabel("Models")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Download models to chat on-device. No internet needed after download.",
                    color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
            }

            items(AVAILABLE_MODELS) { model ->
                ModelDownloadCard(
                    model    = model,
                    progress = progressMap[model.id],
                    onDownload = { url -> downloadViewModel.startDownload(model, url) },
                    onCancel   = { downloadViewModel.cancelDownload(model) },
                    onDelete   = { downloadViewModel.deleteModel(model) },
                )
            }

            item { Spacer(Modifier.height(6.dp)) }

            // ── Semantic Search (Gecko) ───────────────────────────────────────
            item {
                SectionLabel("Semantic Search")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Download the Gecko embedding model to enable semantic (meaning-based) search in your documents. Without it, keyword search is used.",
                    color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
            }

            items(AVAILABLE_EMBEDDING_MODELS) { model ->
                EmbeddingModelDownloadCard(
                    model    = model,
                    progress = embeddingProgressMap[model.id],
                    onDownload = { downloadViewModel.startEmbeddingDownload(model) },
                    onCancel   = { downloadViewModel.cancelEmbeddingDownload(model) },
                    onDelete   = { downloadViewModel.deleteEmbeddingModel(model) },
                )
            }

            item { Spacer(Modifier.height(6.dp)) }

            item {
                SectionLabel("Custom Model URL")
                Spacer(Modifier.height(8.dp))
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

            item {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("OrbitAI • On-device • MediaPipe LLM",
                        color = TextMuted.copy(0.4f), fontSize = 12.sp)
                }
            }
        }
    }
}

// ── HuggingFace Token Card ────────────────────────────────────────────────────

@Composable
private fun HuggingFaceTokenCard(tokenStore: TokenStore) {
    var token       by remember { mutableStateOf(tokenStore.huggingFaceToken) }
    var showToken   by remember { mutableStateOf(false) }
    var saved       by remember { mutableStateOf(tokenStore.hasToken()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Surface1,
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(
            0.5.dp,
            if (saved) AiAccent.copy(0.4f) else Outline
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (saved) Icons.Default.CheckCircle else Icons.Default.Key,
                    contentDescription = null,
                    tint   = if (saved) AiAccent else TextMuted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (saved) "Token saved ✓" else "No token set",
                    color      = if (saved) AiAccent else TextMuted,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            OutlinedTextField(
                value         = token,
                onValueChange = { token = it; saved = false },
                label         = { Text("hf_xxxxxxxxxxxxxxxx", color = TextMuted, fontSize = 12.sp) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp),
                colors        = urlFieldColors(),
                textStyle     = LocalTextStyle.current.copy(
                    fontSize   = 13.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                singleLine            = true,
                visualTransformation  = if (showToken) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        tokenStore.huggingFaceToken = token
                        saved = true
                    },
                    enabled  = token.isNotBlank() && !saved,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = CyanCore,
                        contentColor   = Void,
                    ),
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Token", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (saved) {
                    OutlinedButton(
                        onClick = {
                            token = ""
                            tokenStore.huggingFaceToken = ""
                            saved = false
                        },
                        shape  = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(0.4f)),
                    ) {
                        Text("Clear", fontSize = 13.sp)
                    }
                }
            }

            // Help text
            Text(
                "1. Go to huggingface.co/settings/tokens\n" +
                        "2. Create a token with read access\n" +
                        "3. Accept the license for each model on HuggingFace",
                color    = TextMuted.copy(0.7f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

// ── Inference Settings Card ───────────────────────────────────────────────────

@Composable
private fun InferenceSettingsCard(store: InferenceSettingsStore) {
    val saved = remember { store.get() }
    var temperature     by remember { mutableFloatStateOf(saved.temperature) }
    var topK            by remember { mutableIntStateOf(saved.topK) }
    var topP            by remember { mutableFloatStateOf(saved.topP) }
    var maxDecodedTokens by remember { mutableIntStateOf(saved.maxDecodedTokens) }
    var isDirty         by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Surface1,
        shape    = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Temperature
            SliderRow(
                label    = "Temperature",
                value    = temperature,
                valueStr = "%.2f".format(temperature),
                range    = 0.1f..2.0f,
                onValueChange = { temperature = it; isDirty = true },
            )

            // Top-K
            SliderRow(
                label    = "Top-K",
                value    = topK.toFloat(),
                valueStr = topK.toString(),
                range    = 1f..100f,
                steps    = 98,
                onValueChange = { topK = it.toInt(); isDirty = true },
            )

            // Top-P
            SliderRow(
                label    = "Top-P",
                value    = topP,
                valueStr = "%.2f".format(topP),
                range    = 0.1f..1.0f,
                onValueChange = { topP = it; isDirty = true },
            )

            // Max output tokens
            SliderRow(
                label    = "Max output tokens",
                value    = maxDecodedTokens.toFloat(),
                valueStr = maxDecodedTokens.toString(),
                range    = 128f..2048f,
                steps    = 14,
                onValueChange = { maxDecodedTokens = (it / 128).toInt() * 128; isDirty = true },
            )

            Button(
                onClick = {
                    store.save(InferenceSettings(temperature, topK, topP, maxDecodedTokens))
                    isDirty = false
                },
                enabled  = isDirty,
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = CyanCore,
                    contentColor   = Void,
                ),
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apply Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Text(
                "These settings apply from your next message onwards.",
                color = TextMuted.copy(0.7f), fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueStr: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                valueStr,
                color      = CyanCore,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = range,
            steps         = steps,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor          = CyanCore,
                activeTrackColor    = CyanCore,
                inactiveTrackColor  = Surface2,
            ),
        )
    }
}

// ── Model Download Card ───────────────────────────────────────────────────────

@Composable
private fun ModelDownloadCard(
    model: LlmModel,
    progress: DownloadProgress?,
    onDownload: (customUrl: String?) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val status       = progress?.status ?: DownloadStatus.IDLE
    val isDownloaded = status == DownloadStatus.COMPLETED
    val isActive     = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED
    var showUrl      by remember { mutableStateOf(false) }
    var customUrl    by remember { mutableStateOf(MODEL_DOWNLOAD_URLS[model.id] ?: "") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Surface1,
        shape    = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                if (isDownloaded) listOf(AiAccent.copy(.2f), AiAccent.copy(.05f))
                                else              listOf(CyanCore.copy(.15f), CyanCore.copy(.04f))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(model.paramCount,
                        color      = if (isDownloaded) AiAccent else CyanCore,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface2)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        model.format.badgeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(model.displayName, color = TextPrimary, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(model.description, color = TextMuted, fontSize = 12.sp)
                }
                when {
                    isDownloaded -> Icon(Icons.Default.CheckCircle, null,
                        tint = AiAccent, modifier = Modifier.size(22.dp))
                    isActive     -> CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = CyanCore, strokeWidth = 2.dp)
                    else         -> Icon(Icons.Default.CloudDownload, null,
                        tint = TextMuted, modifier = Modifier.size(22.dp))
                }
            }

            // Progress bar
            if (isActive && (progress?.totalBytes ?: 0L) > 0L) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress   = { progress!!.progress },
                    modifier   = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color      = CyanCore,
                    trackColor = Surface2,
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${progress!!.progressPercent}% · ${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                        color = TextMuted, fontSize = 12.sp)
                    Text("Downloading…", color = CyanCore, fontSize = 12.sp)
                }
            }

            // Error
            if (status == DownloadStatus.FAILED && progress?.error != null) {
                Spacer(Modifier.height(6.dp))
                Text(progress.error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                    lineHeight = 16.sp)
            }

            // Custom URL
            AnimatedVisibility(visible = showUrl && !isDownloaded && !isActive,
                enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value         = customUrl,
                        onValueChange = { customUrl = it },
                        label         = { Text("Download URL", color = TextMuted, fontSize = 12.sp) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp),
                        colors        = urlFieldColors(),
                        textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace),
                        maxLines      = 3,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                when {
                    isDownloaded -> OutlinedButton(
                        onClick = onDelete,
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error),
                        border  = BorderStroke(0.5.dp,
                            MaterialTheme.colorScheme.error.copy(.4f)),
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Delete", fontSize = 13.sp)
                    }
                    isActive -> OutlinedButton(
                        onClick = onCancel,
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        border  = BorderStroke(0.5.dp, Outline),
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Cancel", fontSize = 13.sp)
                    }
                    else -> {
                        Button(
                            onClick = { onDownload(customUrl.takeIf { showUrl && it.isNotBlank() }) },
                            shape   = RoundedCornerShape(10.dp),
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = CyanCore, contentColor = Void),
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Download", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick  = { showUrl = !showUrl },
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (showUrl) CyanCore.copy(.1f) else Surface2),
                        ) {
                            Icon(
                                if (showUrl) Icons.Default.LinkOff else Icons.Default.Link,
                                "Custom URL",
                                tint     = if (showUrl) CyanCore else TextMuted,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Embedding Model Download Card (Gecko) ────────────────────────────────────

@Composable
private fun EmbeddingModelDownloadCard(
    model: EmbeddingModelConfig,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val status       = progress?.status ?: DownloadStatus.IDLE
    val isDownloaded = status == DownloadStatus.COMPLETED
    val isActive     = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Surface1,
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(
            0.5.dp,
            if (isDownloaded) AiAccent.copy(0.4f) else Outline,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                if (isDownloaded) listOf(AiAccent.copy(.2f), AiAccent.copy(.05f))
                                else              listOf(CyanCore.copy(.15f), CyanCore.copy(.04f))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("~8 MB",
                        color      = if (isDownloaded) AiAccent else CyanCore,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(model.displayName, color = TextPrimary, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(model.description, color = TextMuted, fontSize = 12.sp)
                }
                when {
                    isDownloaded -> Icon(Icons.Default.CheckCircle, null,
                        tint = AiAccent, modifier = Modifier.size(22.dp))
                    isActive     -> CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = CyanCore, strokeWidth = 2.dp)
                    else         -> Icon(Icons.Default.CloudDownload, null,
                        tint = TextMuted, modifier = Modifier.size(22.dp))
                }
            }

            if (isDownloaded) {
                Spacer(Modifier.height(6.dp))
                Text("Semantic search active — RAG queries use Gecko embeddings.",
                    color = AiAccent.copy(0.8f), fontSize = 12.sp)
            }

            if (isActive && (progress?.totalBytes ?: 0L) > 0L) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress   = { progress!!.progress },
                    modifier   = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color      = CyanCore,
                    trackColor = Surface2,
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${progress!!.progressPercent}% · ${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                        color = TextMuted, fontSize = 12.sp)
                    Text("Downloading…", color = CyanCore, fontSize = 12.sp)
                }
            }

            if (status == DownloadStatus.FAILED && progress?.error != null) {
                Spacer(Modifier.height(6.dp))
                Text(progress.error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                    lineHeight = 16.sp)
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isDownloaded -> OutlinedButton(
                        onClick = onDelete,
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error),
                        border  = BorderStroke(0.5.dp,
                            MaterialTheme.colorScheme.error.copy(.4f)),
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Delete", fontSize = 13.sp)
                    }
                    isActive -> OutlinedButton(
                        onClick = onCancel,
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        border  = BorderStroke(0.5.dp, Outline),
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Cancel", fontSize = 13.sp)
                    }
                    else -> Button(
                        onClick = onDownload,
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = CyanCore, contentColor = Void),
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Download", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Custom URL Card ───────────────────────────────────────────────────────────

@Composable
private fun CustomUrlCard(onDownload: (url: String, fileName: String) -> Unit) {
    var url      by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Surface1,
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(0.5.dp, Outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Any compatible Task or LiteRT-LM file", color = TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold)
            Text("Paste any MediaPipe LLM model URL (.task or .litertlm) below.",
                color = TextMuted, fontSize = 12.sp)

            OutlinedTextField(
                value         = url,
                onValueChange = { url = it },
                label         = { Text("https://…/model.task or model.litertlm", color = TextMuted, fontSize = 12.sp) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp),
                colors        = urlFieldColors(),
                textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace),
                maxLines      = 2,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value         = fileName,
                onValueChange = { fileName = it },
                label         = { Text("Save as (e.g. mymodel.task or mymodel.litertlm)", color = TextMuted,
                    fontSize = 12.sp) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp),
                colors        = urlFieldColors(),
                textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp),
                singleLine    = true,
            )

            Button(
                onClick  = {
                    val name = normalizeModelFileName(fileName)
                    if (url.isNotBlank() && name.isNotBlank()) onDownload(url.trim(), name)
                },
                enabled  = url.isNotBlank() && fileName.isNotBlank(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = CyanCore, contentColor = Void),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download Custom Model", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun ModelFormat.badgeLabel(): String = when (this) {
    ModelFormat.TASK -> "Task"
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
        trimmed.lowercase().endsWith(".task") || trimmed.lowercase().endsWith(".litertlm") -> trimmed
        else -> "$trimmed.task"
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = CyanCore, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
private fun urlFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor   = Surface2,
    unfocusedContainerColor = Surface2,
    focusedBorderColor      = CyanCore.copy(.6f),
    unfocusedBorderColor    = Outline,
    focusedTextColor        = TextPrimary,
    unfocusedTextColor      = TextPrimary,
    cursorColor             = CyanCore,
)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L         -> "%.1f KB".format(bytes / 1_024.0)
    else                    -> "$bytes B"
}