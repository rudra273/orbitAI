package com.example.orbitai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.verticalScroll
import com.example.orbitai.feature.prompts.CommandPalette
import com.example.orbitai.feature.prompts.PaletteItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import android.widget.Toast
import com.example.orbitai.feature.chat.markdownToPlainText
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import com.example.orbitai.R
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.common.SUPPORTED_DOCUMENT_MIME_TYPES
import com.example.orbitai.core.common.extractDocumentText
import com.example.orbitai.core.common.isImageDocument
import com.example.orbitai.core.common.normalizeDocumentMimeType
import com.example.orbitai.core.database.Mode
import com.example.orbitai.core.database.Space
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ChatUiEvent
import com.example.orbitai.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private data class PendingAttachment(
    val name: String,
    val promptText: String,
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CHAT SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId:    String,
    viewModel: ChatViewModel,
    onBack:    () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    initialText: String = "",
    onInitialTextConsumed: () -> Unit = {},
    initialAttachments: List<String> = emptyList(),
    onInitialAttachmentsConsumed: () -> Unit = {},
    /** A message to scroll to and highlight, e.g. from search. */
    focusMessageId: String? = null,
) {
    val callContactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::onCallContactsPermissionResult)
    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::onCallPermissionResult)
    val callReview by viewModel.callReview.collectAsState()
    val voiceCallPrompt by viewModel.voiceCallPrompt.collectAsState()
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onContactsPermissionResult(granted)
    }
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationsPermissionResult(granted)
    }
    val reminderReview by viewModel.reminderReview.collectAsState()
    val cloudSendRequest by viewModel.cloudSendRequest.collectAsState()
    val chat by remember(chatId) { viewModel.observeChat(chatId) }.collectAsState(initial = null)
    val uiState        by viewModel.uiState.collectAsState()
    callReview?.takeIf { it.chatId == chatId && !it.voice }?.let { request ->
        CallReviewDialog(request, uiState.loadError, viewModel::dismissCallReview, viewModel::confirmCall)
    }
    reminderReview?.takeIf { it.chatId == chatId }?.let { request ->
        ReminderReviewDialog(request, viewModel::dismissReminderReview, viewModel::confirmReminder, uiState.loadError)
    }
    val spaces         by viewModel.spaces.collectAsState()
    val activeSpaceIds by viewModel.activeSpaceIds.collectAsState()
    val modes          by viewModel.modes.collectAsState()
    val activeModeId   by viewModel.activeModeId.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()

    val messages = chat?.messages ?: emptyList()
    val selectedModel = remember(chat?.modelId, availableModels) {
        availableModels.find { it.id == chat?.modelId } ?: availableModels.firstOrNull()
    }

    var inputText by androidx.compose.runtime.saveable.rememberSaveable(chatId) { mutableStateOf("") }
    var cursorRequest by remember { mutableStateOf<Int?>(null) }
    val savedPrompts by viewModel.savedPrompts.collectAsState()
    LaunchedEffect(initialText) {
        if (initialText.isNotBlank()) {
            inputText = initialText
            onInitialTextConsumed()
        }
    }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    var attachmentInfo by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()
    val context = LocalContext.current

    cloudSendRequest?.takeIf { it.chatId == chatId }?.let { request ->
        fun cancelCloudSend() {
            inputText = request.text
            selectedImageUris = request.imageUris
            viewModel.onCloudSendConsent(false)
        }
        AlertDialog(
            onDismissRequest = { cancelCloudSend() },
            title = { Text("Send to ${request.modelName}?") },
            text = {
                Text("Orbit will send this message, chat history, attached images or document text, and relevant memories and Space excerpts to Google for a cloud AI response. Google processes this data under its Gemini API terms. Use a local model to keep inference on this device.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onCloudSendConsent(true) }) { Text("Send to Google") }
            },
            dismissButton = { TextButton(onClick = { cancelCloudSend() }) { Text("Cancel") } },
        )
    }

    /**
     * Attaches an image (vision input) or a document (its text goes into the prompt).
     * With [combine], a document is added to the one already attached instead of replacing it.
     */
    suspend fun attachFile(uri: Uri, combine: Boolean = false) {
        val mimeType = normalizeDocumentMimeType(context.contentResolver.getType(uri), "file")
        if (isImageDocument(mimeType)) {
            if (selectedImageUris.size >= 3) {
                attachmentError = "You can only attach up to 3 images."
            } else {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // Some document providers grant access only for the current session.
                }
                selectedImageUris = selectedImageUris + uri
                attachmentError = null
            }
        } else {
            val attachment = readAttachmentForPrompt(context, uri)
            if (attachment == null) {
                if (!combine) {
                    pendingAttachment = null
                    attachmentInfo = null
                }
                attachmentError = "Couldn't read that file. Try PDF, TXT, Markdown, CSV, JSON, XML, HTML, or an image."
            } else {
                val combined = pendingAttachment?.takeIf { combine }?.let {
                    PendingAttachment("${it.name}, ${attachment.name}", it.promptText + "\n\n" + attachment.promptText)
                } ?: attachment
                pendingAttachment = combined
                attachmentError = null
                attachmentInfo = "${combined.name} attached"
            }
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val model = selectedModel
        if (model == null || !model.supportsDocumentAttachments()) {
            pendingAttachment = null
            attachmentInfo = null
            attachmentError = "This model doesn't support file attachments yet. Use Gemma 3 or Gemini."
            return@rememberLauncherForActivityResult
        }

        scope.launch { attachFile(uri) }
    }

    // Camera and photo picker for vision models. The camera writes to an app-private file, so no permission is needed.
    var pendingPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhotoUri?.let(Uri::parse)
        pendingPhotoUri = null
        if (saved && uri != null) scope.launch { attachFile(uri) }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { attachFile(uri) }
    }
    fun launchCamera() {
        val file = java.io.File(context.filesDir, "shared/camera/${java.util.UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingPhotoUri = uri.toString()
        try {
            takePicture.launch(uri)
        } catch (_: android.content.ActivityNotFoundException) {
            pendingPhotoUri = null
            attachmentError = "No camera app found on this phone."
        }
    }

    // Files shared to Orbit from another app: pick a model that can read them, then attach.
    LaunchedEffect(initialAttachments, availableModels.isNotEmpty()) {
        if (initialAttachments.isEmpty() || availableModels.isEmpty()) return@LaunchedEffect
        val uris = initialAttachments.map(Uri::parse)
        onInitialAttachmentsConsumed()
        val needsVision = uris.any { isImageDocument(normalizeDocumentMimeType(context.contentResolver.getType(it), "file")) }
        fun fits(model: LlmModel) = model.supportsDocumentAttachments() && (!needsVision || model.supportsVision)
        val current = selectedModel
        val target = current?.takeIf(::fits) ?: availableModels.firstOrNull(::fits)
        if (target == null) {
            attachmentError = if (needsVision) {
                "Shared images need a model that can see images. Download one in Settings > Models."
            } else {
                "Shared files need Gemma 3 or Gemini. Download one in Settings > Models."
            }
            return@LaunchedEffect
        }
        if (target.id != current?.id) viewModel.selectModel(chatId, target)
        uris.forEach { attachFile(it, combine = true) }
        if (target.id != current?.id && attachmentError == null) {
            attachmentInfo = listOfNotNull(attachmentInfo, "Using ${target.displayName} to read it").joinToString(" · ")
        }
    }

    val isTemporary = remember(chatId) { viewModel.isTemporaryChat(chatId) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    val enabledCommands = remember { viewModel.enabledCommandNames() }
    uiState.deviceActionRequest?.takeIf { it.chatId == chatId }?.let { request ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeviceAction,
            title = { Text(request.action.tool.title) },
            text = { Text(request.action.confirmation()) },
            confirmButton = { TextButton(onClick = viewModel::confirmDeviceAction) { Text("Do it") } },
            dismissButton = { TextButton(onClick = viewModel::cancelDeviceAction) { Text("Cancel") } },
        )
    }
    var openSource by remember { mutableStateOf<com.example.orbitai.feature.chat.MessageSource?>(null) }
    openSource?.let { source ->
        SourceSheet(
            source = source,
            onOpenDocument = {
                scope.launch {
                    val uri = viewModel.documentUri(source.docId)?.let(Uri::parse)
                    val opened = uri != null && runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }.isSuccess
                    if (!opened) Toast.makeText(context, "This file can't be opened anymore.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { openSource = null },
        )
    }
    val voiceSettings = remember { com.example.orbitai.feature.voice.VoiceSettingsStore(context) }
    val speaker = remember { com.example.orbitai.feature.voice.ReplySpeaker(context, voiceSettings) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    val speakingId by speaker.speakingId.collectAsState()
    // Free the chat's open model conversation when leaving the chat or the app.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(chatId, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) viewModel.onChatHidden(chatId)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onChatHidden(chatId)
        }
    }
    val showGenerationDetails = remember { com.example.orbitai.feature.chat.ChatSettingsStore(context).showGenerationDetails }
    val generationStats by viewModel.generationStats.collectAsState()
    chat?.takeIf { showExport }?.let { current ->
        val modelName = (availableModels + com.example.orbitai.core.model.allLocalModels())
            .find { it.id == current.modelId }?.displayName
        ExportChatDialog(current, modelName, onDismiss = { showExport = false })
    }
    val leave = {
        if (isTemporary) viewModel.discardTemporaryChat(chatId)
        onBack()
    }
    val handleBack = { if (isTemporary && messages.isNotEmpty()) confirmDiscard = true else leave() }
    BackHandler(onBack = handleBack)
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard temporary chat?") },
            text = { Text("Temporary chats aren't saved. This conversation will be gone.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; leave() }) { Text("Discard", color = Destructive) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep chatting") } },
        )
    }


    LaunchedEffect(viewModel) {
        viewModel.refreshAvailableModels()
        viewModel.events.collect { event ->
            when (event) {
                ChatUiEvent.RequestCallContactsPermission -> callContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                ChatUiEvent.RequestCallPermission -> callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                ChatUiEvent.RequestContactsPermission -> {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
                ChatUiEvent.RequestNotificationsPermission -> {
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    var focusPending by rememberSaveable(focusMessageId) { mutableStateOf(focusMessageId != null) }
    var highlightedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(messages.size) {
        if (focusPending) {
            val index = messages.indexOfFirst { it.id == focusMessageId }
            if (index < 0) return@LaunchedEffect
            focusPending = false
            listState.scrollToItem(index)
            highlightedId = focusMessageId
            kotlinx.coroutines.delay(2500)
            highlightedId = null
            return@LaunchedEffect
        }
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChatTopBar(
                    chat          = chat,
                    modes         = modes,
                    activeModeId  = activeModeId,
                    spaces        = spaces,
                    activeSpaceIds = activeSpaceIds,
                    availableModels = availableModels,
                    onBack         = handleBack,
                    onModelSelected = { model -> viewModel.selectModel(chatId, model) },
                    onSelectMode    = { viewModel.selectMode(chatId, it) },
                    onToggleSpace   = { viewModel.toggleSpace(it) },
                    onNavigateToSettings = onNavigateToSettings,
                    onExport = { showExport = true },
                )
            },
            bottomBar = {
                key(chatId) {
                    Column {
                    CommandPalette.query(inputText)?.let { query ->
                        CommandPaletteMenu(CommandPalette.items(query, savedPrompts, enabledCommands)) { item ->
                            val insertion = CommandPalette.insertion(item)
                            cursorRequest = insertion.cursor
                            inputText = insertion.text
                        }
                    }
                    ChatInputBar(
                        voiceCallPrompt = voiceCallPrompt?.takeIf { it.chatId == chatId },
                        onVoiceCall = { viewModel.handleVoiceCall(chatId, it) },
                        onCancelVoiceCall = { viewModel.cancelVoiceCall(chatId) },
                        onVoicePromptSpoken = viewModel::onVoiceCallPromptSpoken,
                        onVoiceUnavailable = { viewModel.showCallReviewForVoiceFailure(chatId) },
                        text         = inputText,
                        onTextChange = { inputText = it; cursorRequest = null },
                        cursorRequest = cursorRequest,
                        onSend       = {
                            val text = inputText.trim()
                            val finalPrompt = buildString {
                                if (text.isNotEmpty()) append(text)
                                pendingAttachment?.let { attachment ->
                                    if (isNotEmpty()) append("\n\n")
                                    append(attachment.promptText)
                                }
                            }.trim()

                            if ((finalPrompt.isNotEmpty() || selectedImageUris.isNotEmpty()) && !uiState.isGenerating && !uiState.isModelLoading) {
                                inputText = ""
                                val imagesToPass = selectedImageUris.toList()
                                pendingAttachment = null
                                selectedImageUris = emptyList()
                                attachmentInfo = null
                                attachmentError = null
                                viewModel.sendMessage(chatId, finalPrompt, imagesToPass)
                                scope.launch {
                                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                                }
                            }
                        },
                        onStop       = { viewModel.stopGeneration() },
                        isGenerating = uiState.isGenerating,
                        isLoading    = uiState.isModelLoading,
                        selectedModelSupportsAttachments = selectedModel?.supportsDocumentAttachments() == true,
                        pendingAttachment = pendingAttachment,
                        selectedImageUris = selectedImageUris,
                        onPickAttachment = {
                            attachmentError = null
                            attachmentInfo = null
                            documentPicker.launch(SUPPORTED_DOCUMENT_MIME_TYPES)
                        },
                        visionAvailable = selectedModel?.supportsVision == true,
                        onTakePhoto = { attachmentError = null; launchCamera() },
                        onPickPhoto = {
                            attachmentError = null
                            pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onClearAttachment = {
                            pendingAttachment = null
                            attachmentInfo = null
                        },
                        onClearImage = { uriToRemove ->
                            selectedImageUris = selectedImageUris.filter { it != uriToRemove }
                        },
                    )
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Status banners
                if (isTemporary) {
                    StatusBanner("Temporary chat · not saved", TextMuted)
                }
                if (uiState.isModelLoading) {
                    StatusBanner("Loading model…", VioletCore)
                }
                attachmentInfo?.let {
                    StatusBanner(it, Warning)
                }
                attachmentError?.let {
                    StatusBanner(it, Warning)
                }
                uiState.infoMessage?.let {
                    StatusBanner(it, Color(0xFF34D399))
                }
                uiState.loadError?.let {
                    StatusBanner(it, Destructive)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Chat text size and density (Settings > Appearance) apply to messages only.
                    val appearance = com.example.orbitai.ui.theme.LocalAppearance.current
                    val baseDensity = androidx.compose.ui.platform.LocalDensity.current
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                            baseDensity.density,
                            baseDensity.fontScale * appearance.chatTextScale,
                        ),
                    ) {
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(if (appearance.compactMessages) 4.dp else 12.dp),
                    ) {
                        val latestReplyId = messages.lastOrNull { it.role == Role.ASSISTANT }?.id
                        val actionsEnabled = !uiState.isGenerating && !uiState.isModelLoading
                        items(messages, key = { it.id }) { msg ->
                          Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (msg.id == highlightedId) VioletGlowSoft else Color.Transparent),
                          ) {
                            MessageBubble(
                                msg = msg,
                                isLatestReply = msg.id == latestReplyId,
                                actionsEnabled = actionsEnabled,
                                onRegenerate = { viewModel.regenerate(chatId) },
                                onEditAndResend = { text -> viewModel.editAndResend(chatId, msg.id, text) },
                                onDelete = { viewModel.deleteMessage(msg.id) },
                                isSpeaking = speakingId == msg.id,
                                details = if (showGenerationDetails) generationStats[msg.id]?.label() else null,
                                onSourceClick = { openSource = it },
                                onRemind = { viewModel.remindAbout(chatId, msg.content) },
                                onReadAloud = {
                                    if (speakingId == msg.id) speaker.stop() else speaker.speakMessage(msg.id, msg.content)
                                },
                            )
                            uiState.memorySuggestion?.takeIf { it.chatId == chatId && it.messageId == msg.id }?.let { suggestion ->
                                MemorySuggestionChip(
                                    fact = suggestion.fact,
                                    onSave = viewModel::saveMemorySuggestion,
                                    onDismiss = viewModel::dismissMemorySuggestion,
                                )
                            }
                          }
                        }
                    }
                    }

                    if (messages.isEmpty() && !uiState.isGenerating && !uiState.isModelLoading) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Image(
                                painter            = painterResource(R.drawable.vector_logo),
                                contentDescription = "OrbitAI",
                                modifier           = Modifier.size(64.dp),
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text       = "How can I help you today?",
                                style      = MaterialTheme.typography.headlineMedium,
                                color      = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text  = "Ask anything, generate code, or explore ideas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                            Spacer(Modifier.height(20.dp))
                            StarterSuggestions(hasActiveSpaces = activeSpaceIds.isNotEmpty()) { suggestion ->
                                cursorRequest = null
                                inputText = suggestion
                            }
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TOP BAR — OrbitAI header + context strip
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chat:           Chat?,
    modes:          List<Mode>,
    activeModeId:   String,
    spaces:         List<Space>,
    activeSpaceIds: Set<String>,
    availableModels: List<LlmModel>,
    onBack:         () -> Unit,
    onModelSelected: (LlmModel) -> Unit,
    onSelectMode:   (String) -> Unit,
    onToggleSpace:  (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onExport: () -> Unit = {},
) {
    val activeMode = modes.find { it.id == activeModeId } ?: modes.firstOrNull()
    val title = remember(chat?.title) { displayChatTitle(chat) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceDeep),
    ) {
        // ── Row 1: back + OrbitAI title + on-device pill ──────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize      = 17.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // On-device / model status pill
            if (chat != null && availableModels.isNotEmpty()) {
                OnDeviceModelPill(
                    chat            = chat,
                    availableModels = availableModels,
                    onModelSelected = onModelSelected,
                )
            }

            if (chat != null && chat.messages.isNotEmpty()) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Chat options", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Export chat") }, onClick = { menuOpen = false; onExport() })
                    }
                }
            } else {
                Spacer(Modifier.width(4.dp))
            }
        }

        // ── Row 2: context strip — mode chip + space chips ─────────────────
        if (modes.isNotEmpty() || spaces.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (modes.isNotEmpty() && activeMode != null) {
                    ModeDropdownChip(
                        modes        = modes,
                        activeMode   = activeMode,
                        activeModeId = activeModeId,
                        onSelectMode = onSelectMode,
                    )
                }
                spaces.forEach { space ->
                    SpaceToggleChip(
                        space         = space,
                        selected      = space.id in activeSpaceIds,
                        onToggleSpace = onToggleSpace,
                    )
                }
            }
        }

        // Bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassBorder),
        )
    }
}

private fun displayChatTitle(chat: Chat?): String {
    val title = chat?.title?.trim().orEmpty()
    return if (title.isBlank() || title.equals("New Chat", ignoreCase = true)) {
        "Untitled chat"
    } else {
        title
    }
}

// ── On-device model pill ───────────────────────────────────────────────────────

@Composable
private fun OnDeviceModelPill(
    chat:            Chat,
    availableModels: List<LlmModel>,
    onModelSelected: (LlmModel) -> Unit,
) {
    if (availableModels.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val selectedModel = availableModels.find { it.id == chat.modelId }
    val isLocal = selectedModel?.provider == com.example.orbitai.core.model.ModelProvider.LOCAL

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OnDevicePillBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(OnDevicePillText),
            )
            Text(
                text  = if (isLocal) "On-device" else selectedModel?.displayName ?: "Cloud",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = OnDevicePillText,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Select model",
                tint     = OnDevicePillText.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp),
            )
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = SpaceNebula,
        ) {
            availableModels.forEach { model ->
                val isSelected = model.id == chat.modelId
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                model.displayName,
                                color      = if (isSelected) VioletBright else TextPrimary,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Text(
                                model.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    },
                    onClick = { onModelSelected(model); expanded = false },
                    trailingIcon = if (isSelected) ({
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(VioletCore)
                        )
                    }) else null,
                )
            }
        }
    }
}

// ── Mode dropdown chip ────────────────────────────────────────────────────────

@Composable
private fun ModeDropdownChip(
    modes:         List<Mode>,
    activeMode:    Mode,
    activeModeId:  String,
    onSelectMode:  (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SpaceDust)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val accent = activeMode.preset.accentColor?.let { Color(0xFF000000 or it.toLong()) }
            Icon(
                Icons.Outlined.Layers,
                contentDescription = null,
                tint     = accent ?: TextSecondary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                activeMode.name,
                style      = MaterialTheme.typography.labelMedium,
                color      = TextPrimary,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Switch mode",
                tint     = TextMuted,
                modifier = Modifier.size(14.dp),
            )
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = SpaceNebula,
        ) {
            modes.forEach { mode ->
                val isSelected = mode.id == activeModeId
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    mode.name,
                                    color      = if (isSelected) VioletCore else TextPrimary,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                            Text(
                                mode.systemPrompt,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick      = { onSelectMode(mode.id); expanded = false },
                    trailingIcon = if (isSelected) ({
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = VioletCore,
                            modifier = Modifier.size(16.dp),
                        )
                    }) else null,
                )
            }
        }
    }
}

// ── Space toggle chip ─────────────────────────────────────────────────────────

@Composable
private fun SpaceToggleChip(
    space:         Space,
    selected:      Boolean,
    onToggleSpace: (String) -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue   = if (selected) VioletGlow else SpaceDust,
        animationSpec = tween(200),
        label         = "space_chip_bg",
    )
    val textColor by animateColorAsState(
        targetValue   = if (selected) VioletCore else TextSecondary,
        animationSpec = tween(200),
        label         = "space_chip_text",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (selected) VioletFrost else Color.Transparent,
        animationSpec = tween(200),
        label         = "space_chip_border",
    )

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
            ) { onToggleSpace(space.id) }
            .padding(horizontal = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(VioletCore),
        )
        Text(
            text  = space.name,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint     = VioletCore,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MESSAGE BUBBLE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    msg: Message,
    isLatestReply: Boolean,
    actionsEnabled: Boolean,
    onRegenerate: () -> Unit,
    onEditAndResend: (String) -> Unit,
    onDelete: () -> Unit,
    isSpeaking: Boolean = false,
    onReadAloud: () -> Unit = {},
    /** Optional generation stats line (Settings > Chat > Show generation details). */
    details: String? = null,
    onSourceClick: (com.example.orbitai.feature.chat.MessageSource) -> Unit = {},
    onRemind: () -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var reportedResponse by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    reportedResponse?.let { response ->
        ContentReportDialog(response, "chat", onDismiss = { reportedResponse = null })
    }
    val isUser = msg.role == Role.USER
    val copy = {
        clipboard.setText(AnnotatedString(markdownToPlainText(msg.content)))
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }
    val share = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, markdownToPlainText(msg.content))
        }
        context.startActivity(Intent.createChooser(send, "Share message"))
    }

    if (editing) {
        EditMessageDialog(
            initialText = msg.content,
            onDismiss = { editing = false },
            onSend = { text ->
                editing = false
                onEditAndResend(text)
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete message?") },
            text = { Text("This removes the message from the chat. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = Destructive) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    if (isUser) {
        // User: right-aligned ink bubble
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 296.dp)
                    .semantics {
                        customActions = listOfNotNull(
                            CustomAccessibilityAction("Copy") { copy(); true },
                            if (actionsEnabled) CustomAccessibilityAction("Edit and resend") { editing = true; true } else null,
                            if (actionsEnabled) CustomAccessibilityAction("Delete") { confirmDelete = true; true } else null,
                        )
                    }
                    .combinedClickable(onClick = {}, onLongClick = { menuOpen = true }, onLongClickLabel = "Message options")
                    .clip(
                        RoundedCornerShape(
                            topStart    = 20.dp,
                            topEnd      = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd   = 4.dp,
                        )
                    )
                    .background(UserBubbleFill)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Column {
                    if (msg.imageUris.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(bottom = if (msg.content.isNotEmpty()) 8.dp else 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            msg.imageUris.forEach { _ ->
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = "Image preview",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (msg.content.isNotEmpty()) {
                        MarkdownMessageText(
                            content   = msg.content,
                            textColor = if (IsOrbitDarkTheme) Color(0xFF141413) else Color.White,
                        )
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = { menuOpen = false; copy() },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit & resend") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        enabled = actionsEnabled,
                        onClick = { menuOpen = false; editing = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Remind me about this") },
                        leadingIcon = { Icon(Icons.Outlined.Alarm, contentDescription = null) },
                        onClick = { menuOpen = false; onRemind() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        enabled = actionsEnabled,
                        onClick = { menuOpen = false; confirmDelete = true },
                    )
                }
            }
        }
    } else {
        // AI: no bubble — ORBIT badge header + content directly on background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            // ORBIT badge row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(bottom = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(VioletGlow)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text(
                        text  = assistantModeBadgeLabel(msg),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily    = FontFamily.Monospace,
                            fontSize      = 9.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight    = FontWeight.Medium,
                        ),
                        color = VioletCore,
                    )
                }
            }

            if (msg.isStreaming && msg.content.isEmpty()) {
                TypingIndicator()
            } else {
                SelectionContainer {
                    MarkdownMessageText(msg.content)
                }
                if (msg.isStreaming) {
                    Spacer(Modifier.height(4.dp))
                    StreamingCursor()
                }
            }
            if (!msg.isStreaming && msg.content.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    MessageActionIcon(Icons.Outlined.ContentCopy, "Copy reply", onClick = copy)
                    if (isLatestReply) {
                        MessageActionIcon(Icons.Outlined.Refresh, "Regenerate reply", enabled = actionsEnabled, onClick = onRegenerate)
                    }
                    MessageActionIcon(
                        if (isSpeaking) Icons.Outlined.StopCircle else Icons.AutoMirrored.Outlined.VolumeUp,
                        if (isSpeaking) "Stop reading" else "Read aloud",
                        onClick = onReadAloud,
                    )
                    MessageActionIcon(Icons.Outlined.Share, "Share reply", onClick = share)
                    MessageActionIcon(Icons.Outlined.Alarm, "Remind me about this", onClick = onRemind)
                    MessageActionIcon(Icons.Outlined.Flag, "Report reply") { reportedResponse = msg.content }
                    if (msg.usedMemories.isNotEmpty()) {
                        var showMemories by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = { showMemories = true },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Icon(Icons.Outlined.Psychology, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Memory used", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        if (showMemories) {
                            AlertDialog(
                                onDismissRequest = { showMemories = false },
                                title = { Text("Memories used") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        msg.usedMemories.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                                        Text(
                                            "Manage memories in Settings > Memory.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                        )
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showMemories = false }) { Text("OK") } },
                            )
                        }
                    }
                }
                if (msg.sources.isNotEmpty() && !msg.isStreaming) SourcesRow(msg.sources, onSourceClick)
                details?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) TextMuted else TextMuted.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun EditMessageDialog(initialText: String, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), maxLines = 8)
                Text("Replies after this message will be replaced.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(text) }, enabled = text.isNotBlank()) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun assistantModeBadgeLabel(msg: Message): String =
    msg.modeName?.trim().orEmpty().ifBlank { "Orbit" }

// ── Typing dots ───────────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.padding(vertical = 2.dp),
    ) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue  = 0.25f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 140, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse,
                ),
                label = "dot$i",
            )
            val scale by infiniteTransition.animateFloat(
                initialValue  = 0.8f,
                targetValue   = 1.2f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 140, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse,
                ),
                label = "dot_scale$i",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(CircleShape)
                    .background(TextMuted.copy(alpha = alpha))
            )
        }
    }
}

// ── Streaming cursor ──────────────────────────────────────────────────────────

@Composable
private fun StreamingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label         = "cursor_alpha",
    )
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .size(width = 2.dp, height = 14.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(TextMuted.copy(alpha = alpha))
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// INPUT BAR — floating bar
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ChatInputBar(
    voiceCallPrompt: com.example.orbitai.viewmodel.VoiceCallPrompt?,
    onVoiceCall: (String) -> Boolean,
    onCancelVoiceCall: () -> Unit,
    onVoicePromptSpoken: (Long) -> Unit,
    onVoiceUnavailable: () -> Unit,
    text:         String,
    onTextChange: (String) -> Unit,
    cursorRequest: Int? = null,
    onSend:       () -> Unit,
    onStop:       () -> Unit,
    isGenerating: Boolean,
    isLoading:    Boolean,
    selectedModelSupportsAttachments: Boolean,
    pendingAttachment: PendingAttachment?,
    selectedImageUris: List<Uri>,
    onPickAttachment: () -> Unit,
    visionAvailable: Boolean,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onClearAttachment: () -> Unit,
    onClearImage: (Uri) -> Unit,
) {
    val (voiceState, toggleVoice) = rememberVoiceInput(
        onTextChange, voiceCallPrompt, onVoiceCall, onCancelVoiceCall, onVoicePromptSpoken, onVoiceUnavailable,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceDeep)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Pending attachment preview
        pendingAttachment?.let { attachment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpaceDust)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text     = attachment.name,
                    color    = TextPrimary,
                    style    = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    tint     = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onClearAttachment,
                        ),
                )
            }
        }

        // Selected Images preview
        if (selectedImageUris.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedImageUris.forEach { uri ->
                    Box(modifier = Modifier.size(64.dp)) {
                        // We use a basic placeholder box since Coil is not installed
                        // The true image data will pass to the model
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpaceDust)
                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "IMG",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }

                        // Remove icon overlay
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { onClearImage(uri) }
                        )
                    }
                }
            }
        }

        // Input row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Warm rounded input field
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SpaceDust)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Attach icon: Camera / Photos / Files for vision models, otherwise straight to Files.
                var attachMenu by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = { if (visionAvailable) attachMenu = true else onPickAttachment() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    DropdownMenu(expanded = attachMenu, onDismissRequest = { attachMenu = false }) {
                        DropdownMenuItem(text = { Text("Camera") }, onClick = { attachMenu = false; onTakePhoto() })
                        DropdownMenuItem(text = { Text("Photos") }, onClick = { attachMenu = false; onPickPhoto() })
                        DropdownMenuItem(text = { Text("Files") }, onClick = { attachMenu = false; onPickAttachment() })
                    }
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint     = if (selectedModelSupportsAttachments) TextSecondary else TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Text field (keeps its own cursor; external text changes place it at cursorRequest or the end)
                var fieldValue by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
                if (fieldValue.text != text) {
                    fieldValue = TextFieldValue(text, TextRange((cursorRequest ?: text.length).coerceIn(0, text.length)))
                }
                BasicTextField(
                    value         = fieldValue,
                    onValueChange = { value ->
                        fieldValue = value
                        if (value.text != text) onTextChange(value.text)
                    },
                    modifier      = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    textStyle     = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush   = SolidColor(TextPrimary),
                    singleLine    = false,
                    maxLines      = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Default,
                    ),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    when (voiceState) {
                                        VoiceState.Listening -> "Listening…"
                                        VoiceState.Responding -> "One moment…"
                                        VoiceState.Idle -> "Ask anything…"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextMuted,
                                )
                            }
                            inner()
                        }
                    },
                )

                // Mic button (inside bar, right side)
                if (voiceState == VoiceState.Idle && text.isEmpty() && pendingAttachment == null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = toggleVoice,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint     = TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Send / Stop / Mic-active button
            when {
                isGenerating -> StopButton(onClick = onStop)
                voiceState != VoiceState.Idle -> MicButton(isListening = true, onClick = toggleVoice)
                text.trim().isNotEmpty() || pendingAttachment != null || selectedImageUris.isNotEmpty() ->
                    SendButton(enabled = !isLoading, isLoading = isLoading, onClick = onSend)
                else -> Unit
            }
        }
    }
}


@Composable
private fun SendButton(
    enabled:   Boolean,
    isLoading: Boolean,
    onClick:   () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) TextPrimary else SpaceDust)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                enabled           = enabled,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                color       = SpaceDeep,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint     = if (enabled) SpaceDeep else TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(DestructiveSoft)
            .border(1.dp, Destructive.copy(alpha = 0.3f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Stop,
            contentDescription = "Stop generation",
            tint     = Destructive,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MicButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = if (isListening) 1.10f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic_scale",
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(if (isListening) Destructive else TextPrimary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = if (isListening) "Stop voice input" else "Voice input",
            tint     = SpaceDeep,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STATUS BANNER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun StatusBanner(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text  = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// animateColorAsState alias (avoids import conflicts)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun animateColorAsState(
    targetValue:   androidx.compose.ui.graphics.Color,
    animationSpec: androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.graphics.Color> = tween(200),
    label:         String,
): State<androidx.compose.ui.graphics.Color> =
    androidx.compose.animation.animateColorAsState(
        targetValue   = targetValue,
        animationSpec = animationSpec,
        label         = label,
    )

private fun LlmModel.supportsDocumentAttachments(): Boolean {
    val normalizedId = id.lowercase()
    return provider == com.example.orbitai.core.model.ModelProvider.GEMINI ||
        normalizedId.startsWith("gemma3") ||
        normalizedId.startsWith("gemma-3")
}

private suspend fun readAttachmentForPrompt(
    context: android.content.Context,
    uri: Uri,
): PendingAttachment? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        if (nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: "document"
    val mimeType = normalizeDocumentMimeType(resolver.getType(uri), name)
    val content = extractDocumentText(context, uri, mimeType)
        .replace("\r\n", "\n")
        .trim()
        .take(6000)

    if (content.isBlank()) return@withContext null

    PendingAttachment(
        name = name,
        promptText = buildString {
            append("Attached file: ").append(name).append('\n')
            append("Please use the file below while answering.\n\n")
            append(content)
        }
    )
}

/** "Sources" chips under an answer: which document excerpts it was given. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SourcesRow(sources: List<com.example.orbitai.feature.chat.MessageSource>, onClick: (com.example.orbitai.feature.chat.MessageSource) -> Unit) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
        Text("Sources", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            sources.forEach { source ->
                val label = if (source.available) com.example.orbitai.feature.spaces.Citations.label(source) else "${source.rank} · Removed document"
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (source.available) TextSecondary else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SpaceDust)
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Show source") { onClick(source) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .widthIn(max = 220.dp),
                )
            }
        }
    }
}

/** The excerpt an answer was given, with a way to open the document. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceSheet(source: com.example.orbitai.feature.chat.MessageSource, onOpenDocument: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SpaceNebula) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (source.available) source.docName else "Removed document",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = listOfNotNull("Source ${source.rank}", source.page?.let { "page $it" }).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
            Text(
                text = source.excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(VioletGlowSoft)
                    .padding(12.dp),
            )
            if (source.available) {
                OutlinedButton(onClick = onOpenDocument) { Text("Open document") }
            } else {
                Text("This document was removed from its Space. The excerpt above is what Orbit used.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

/** "Remember: Lives in Pune?" under the user message, with Save, Edit and dismiss. */
@Composable
private fun MemorySuggestionChip(fact: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var editing by remember(fact) { mutableStateOf(false) }
    var edited by remember(fact) { mutableStateOf(fact) }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Remember") },
            text = { OutlinedTextField(value = edited, onValueChange = { edited = it }, maxLines = 3) },
            confirmButton = {
                TextButton(enabled = edited.isNotBlank(), onClick = { editing = false; onSave(edited.trim()) }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, top = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VioletGlowSoft)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Psychology, contentDescription = null, tint = TextAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Remember: $fact?",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { onSave(fact) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Save") }
        TextButton(onClick = { editing = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Edit") }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Don't remember", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CommandPaletteMenu(items: List<PaletteItem>, onSelect: (PaletteItem) -> Unit) {
    if (items.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SpaceDust)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        items.forEach { item ->
            val (title, subtitle) = when (item) {
                is PaletteItem.Command -> "/${item.command.name} · ${item.command.title}" to item.command.example
                is PaletteItem.Prompt -> item.prompt.title to item.prompt.text
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun StarterSuggestions(hasActiveSpaces: Boolean, onPick: (String) -> Unit) {
    val suggestions = buildList {
        if (hasActiveSpaces) add("Summarize the key points of my documents")
        add("Explain how vaccines work in simple words")
        add("Write a polite reply declining a meeting")
        add("/remind in 10 minutes to stretch")
        add("Type / for commands and saved prompts")
    }.take(4)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { onPick(if (suggestion.startsWith("Type /")) "/" else suggestion) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(suggestion, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
