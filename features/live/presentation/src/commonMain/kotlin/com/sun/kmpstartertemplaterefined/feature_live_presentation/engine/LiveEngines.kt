package com.sun.kmpstartertemplaterefined.feature_live_presentation.engine

interface AgoraEngine {
    suspend fun joinChannel(
        appId: String,
        token: String,
        channelName: String,
        uid: Int,
    )

    suspend fun renewToken(token: String)

    suspend fun leaveChannel()

    /** 用來讓 UI 顯示某個 uid 的畫面（screen share 或 camera） */
    @androidx.compose.runtime.Composable
    fun VideoView(uid: Int, modifier: androidx.compose.ui.Modifier)
}

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