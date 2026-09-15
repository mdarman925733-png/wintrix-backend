package com.aicomp.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicomp.data.model.ChatMessage
import com.aicomp.data.model.Sender
import com.aicomp.data.repository.ChatRepository
import com.aicomp.data.repository.CoinRepository
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Coins deducted from the user per message sent. */
private const val COINS_PER_MESSAGE = 1L

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    private val _insufficientCoins = MutableStateFlow(false)
    val insufficientCoins: StateFlow<Boolean> = _insufficientCoins

    fun dismissInsufficientCoins() {
        _insufficientCoins.value = false
    }

    private var loadedCompanionId: String? = null
    private var loadedUid: String? = null

    private val functions = Firebase.functions

    /** Call once per screen open — starts listening to that companion's saved history. */
    fun loadHistory(companionId: String) {
        if (loadedCompanionId == companionId) return
        loadedCompanionId = companionId
        viewModelScope.launch {
            val uid = ChatRepository.ensureSignedIn()
            loadedUid = uid
            ChatRepository.observeMessages(uid, companionId).collect { history ->
                _messages.value = history
            }
        }
    }

    /**
     * Sends a user message and gets an AI reply via the sendChatMessage
     * Cloud Function. The server now builds the system prompt itself
     * (companion instructions + shared chat/call history) directly from
     * the database — so it's guaranteed to match what the live call uses,
     * and nothing needs to be composed client-side anymore.
     */
    fun sendMessage(companionId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val uid = loadedUid ?: ChatRepository.ensureSignedIn().also { loadedUid = it }

            val spent = CoinRepository.trySpend(uid, COINS_PER_MESSAGE)
            if (!spent) {
                _insufficientCoins.value = true
                return@launch
            }

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = text.trim(),
                sender = Sender.USER
            )
            ChatRepository.saveMessage(uid, companionId, userMessage)

            _isTyping.value = true
            try {
                val replyText = callSendChatMessage(companionId, text.trim())

                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = replyText,
                    sender = Sender.AI
                )
                ChatRepository.saveMessage(uid, companionId, aiMessage)

            } catch (e: Exception) {
                val errMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Error: ${e.message ?: e.javaClass.simpleName}",
                    sender = Sender.AI
                )
                ChatRepository.saveMessage(uid, companionId, errMsg)
            } finally {
                _isTyping.value = false
            }
        }
    }

    private suspend fun callSendChatMessage(companionId: String, text: String): String {
        val result = functions
            .getHttpsCallable("sendChatMessage")
            .call(
                mapOf(
                    "companionId" to companionId,
                    "text" to text
                )
            )
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as Map<String, Any?>
        return data["reply"] as? String
            ?: throw Exception("Empty reply from server")
    }
}