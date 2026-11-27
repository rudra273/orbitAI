package com.example.orbitai.viewmodel

import com.example.orbitai.feature.automation.executor.CallContact
import com.example.orbitai.feature.automation.executor.CallExecutor
import com.example.orbitai.feature.automation.executor.ContactResolver
import com.example.orbitai.feature.automation.executor.callableNumber
import com.example.orbitai.feature.automation.parser.CallConversation
import com.example.orbitai.feature.automation.parser.CallReply
import com.example.orbitai.feature.automation.parser.DeviceCommand
import com.example.orbitai.feature.automation.parser.DeviceCommandParser
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Contact lookup, call review and the spoken call-confirmation conversation. */
internal class CallFlowController(
    private val ctx: ChatControllerContext,
    private val stopGeneration: () -> Unit,
) {

    private val _uiState get() = ctx.uiState
    private val _events get() = ctx.events
    private val viewModelScope get() = ctx.scope
    private val chatRepo = ctx.container.chatRepository

    private val _callReview = MutableStateFlow<CallReviewRequest?>(null)
    val callReview = _callReview.asStateFlow()
    private val _voiceCallPrompt = MutableStateFlow<VoiceCallPrompt?>(null)
    val voiceCallPrompt = _voiceCallPrompt.asStateFlow()
    private var voicePromptId = 0L
    private var spokenVoicePromptId: Long? = null
    private var pendingCallLookup: CallLookup? = null
    private var callLookupJob: Job? = null
    private var pendingCall: CallContact? = null

    fun dismissCallReview() {
        callLookupJob?.cancel()
        pendingCallLookup = null
        _callReview.value = null
        _voiceCallPrompt.value = null
        spokenVoicePromptId = null
        pendingCall = null
    }

    fun cancelVoiceCall(chatId: String) {
        if (_callReview.value?.let { it.chatId == chatId && it.voice } == true ||
            pendingCallLookup?.let { it.chatId == chatId && it.voice } == true ||
            _voiceCallPrompt.value?.chatId == chatId) dismissCallReview()
    }

    fun onVoiceCallPromptSpoken(id: Long) {
        if (_voiceCallPrompt.value?.id == id) spokenVoicePromptId = id
    }

    fun showCallReviewForVoiceFailure(chatId: String) {
        _voiceCallPrompt.value = null
        spokenVoicePromptId = null
        _callReview.value?.takeIf { it.chatId == chatId }?.let {
            _callReview.value = it.copy(voice = false)
        }
    }

    private fun sayCall(chatId: String, text: String, voice: Boolean, listenAfter: Boolean = true) {
        if (voice) {
            spokenVoicePromptId = null
            _voiceCallPrompt.value = VoiceCallPrompt(++voicePromptId, chatId, text, listenAfter)
        }
        recordCallMessage(chatId, Role.ASSISTANT, text)
    }

    private fun recordCallMessage(chatId: String, role: Role, text: String) {
        viewModelScope.launch {
            try {
                chatRepo.addMessage(chatId, Message(role = role, content = text))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't save this call exchange to chat history.") }
            }
        }
    }

    private fun reviewCall(lookup: CallLookup, contacts: List<CallContact>) {
        val review = CallReviewRequest(lookup.chatId, contacts, lookup.voice)
        _callReview.value = review
        sayCall(lookup.chatId, CallConversation.question(contacts, review.selected), lookup.voice)
    }

    fun resolveCall(chatId: String, recipient: String, voice: Boolean = false) {
        dismissCallReview()
        val lookup = CallLookup(chatId, recipient, voice)
        val app = ctx.app
        val number = callableNumber(recipient)
        if (number != null) {
            reviewCall(lookup, listOf(CallContact(recipient, number)))
            return
        }
        pendingCallLookup = lookup
        if (androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModelScope.launch { _events.emit(ChatUiEvent.RequestCallContactsPermission) }
            return
        }
        callLookupJob = viewModelScope.launch {
            try {
                val contacts = withContext(Dispatchers.IO) { ContactResolver(app).findCallContacts(recipient) }
                pendingCallLookup = null
                if (contacts.isEmpty()) {
                    val message = "I couldn't find a close contact match for $recipient. Say call followed by the full name or a phone number."
                    _uiState.update { it.copy(loadError = message) }
                    sayCall(chatId, message, voice)
                } else reviewCall(lookup, contacts)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                pendingCallLookup = null
                val message = "Couldn't read contacts. Check contacts permission in Android Settings."
                _uiState.update { it.copy(loadError = message) }
                sayCall(chatId, message, voice, listenAfter = false)
            }
        }
    }

    fun onCallContactsPermissionResult(granted: Boolean) {
        val lookup = pendingCallLookup ?: return
        pendingCallLookup = null
        if (granted) resolveCall(lookup.chatId, lookup.recipient, lookup.voice)
        else {
            val message = "Contacts permission was denied. Say call followed by a phone number instead."
            _uiState.update { it.copy(loadError = message) }
            sayCall(lookup.chatId, message, lookup.voice)
        }
    }

    /** Returns true when a finalized utterance belongs to the call conversation. */
    fun handleVoiceCall(chatId: String, text: String): Boolean {
        val command = DeviceCommandParser.parse(text) as? DeviceCommand.Call
        val review = _callReview.value?.takeIf { it.chatId == chatId && it.voice }
        if (command == null && review == null) return false
        _uiState.update { it.copy(loadError = null, infoMessage = null) }
        if (command != null) {
            stopGeneration()
            recordCallMessage(chatId, Role.USER, text)
            resolveCall(chatId, command.recipient, voice = true)
            return true
        }
        review ?: return false
        if (pendingCall != null) return true
        // A confirmation is valid only after the current question has finished speaking.
        if (spokenVoicePromptId != _voiceCallPrompt.value?.id) return true
        spokenVoicePromptId = null
        recordCallMessage(chatId, Role.USER, text)
        when (val reply = CallConversation.reply(text, review.contacts, review.selected)) {
            CallReply.Cancel -> {
                dismissCallReview()
                sayCall(chatId, "Call cancelled.", voice = true, listenAfter = false)
            }
            CallReply.Confirm -> review.selected?.let { confirmCall(it) }
            CallReply.OpenDialer -> review.selected?.let { confirmCall(it, dialOnly = true) }
            is CallReply.Select -> {
                _callReview.value = review.copy(selected = reply.contact)
                sayCall(chatId, CallConversation.question(review.contacts, reply.contact), voice = true)
            }
            CallReply.Repeat -> sayCall(chatId, CallConversation.question(review.contacts, review.selected), voice = true)
        }
        return true
    }

    fun confirmCall(contact: CallContact, dialOnly: Boolean = false) {
        val review = _callReview.value ?: return
        if (contact !in review.contacts || (review.voice && review.selected != contact)) return
        if (pendingCall != null) return
        val app = ctx.app
        val executor = CallExecutor(app)
        if (executor.canCallDirectly(contact, dialOnly) &&
            androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingCall = contact
            viewModelScope.launch { _events.emit(ChatUiEvent.RequestCallPermission) }
            return
        }
        val error = executor.launch(contact, dialOnly)
        val message = if (error == null) "Phone app opened for ${contact.name}." else error
        _uiState.update { it.copy(loadError = error, infoMessage = if (error == null) message else null) }
        if (error == null) dismissCallReview()
        sayCall(review.chatId, message, review.voice, listenAfter = error != null)
    }

    fun onCallPermissionResult(granted: Boolean) {
        val contact = pendingCall ?: return
        pendingCall = null
        if (granted) confirmCall(contact)
        else {
            val review = _callReview.value ?: return
            val message = "Phone permission was denied. Say open dialer to continue manually, or cancel."
            _uiState.update { it.copy(loadError = message) }
            sayCall(review.chatId, message, review.voice)
        }
    }
}
