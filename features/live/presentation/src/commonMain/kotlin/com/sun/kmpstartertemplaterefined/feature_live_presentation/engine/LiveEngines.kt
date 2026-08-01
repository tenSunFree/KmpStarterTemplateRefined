package com.sun.kmpstartertemplaterefined.feature_live_presentation.engine

data class ChatMessage(
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
)

interface ChatClient {
    suspend fun connect(wsUrl: String, roomId: String)
    suspend fun disconnect()
    suspend fun sendMessage(content: String)
    suspend fun sendReaction(emoji: String)
    val messages: kotlinx.coroutines.flow.StateFlow<List<ChatMessage>>
    val isConnected: kotlinx.coroutines.flow.StateFlow<Boolean>
}