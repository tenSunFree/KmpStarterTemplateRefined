package com.sun.kmpstartertemplaterefined.feature_live_presentation.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NoOpChatClient : ChatClient {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages = _messages.asStateFlow()
    private val _isConnected = MutableStateFlow(false)
    override val isConnected = _isConnected.asStateFlow()

    override suspend fun connect(wsUrl: String, roomId: String) {
        _isConnected.value = false
    }

    override suspend fun disconnect() {
        _isConnected.value = false
    }

    override suspend fun sendMessage(content: String) = Unit

    override suspend fun sendReaction(emoji: String) = Unit
}