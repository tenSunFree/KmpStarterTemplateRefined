package com.sun.kmpstartertemplaterefined.feature_live_presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.JoinLiveConfig
import com.sun.kmpstartertemplaterefined.feature_live_presentation.engine.ChatClient
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.isInPipMode
import com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc.LiveRtcClassroomView
import com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc.LiveRtcSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val Pink = Color(0xFFFF3F68)
private val DarkBg = Color(0xFF111111)

/**
 * Live room screen.
 *
 * @param config Complete configuration returned by the join API.
 * @param chatClient WebSocket chat room client (platform-specific or shared implementation).
 * @param onRejoin Re-call joinLive before expiry to fetch a new config; returns null on failure.
 * @param onExit Called when the user leaves the live room.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun LiveRoomScreen(
    config: JoinLiveConfig,
    chatClient: ChatClient,
    onRejoin: suspend () -> JoinLiveConfig?,
    onExit: () -> Unit,
) {
    var current by remember(config) { mutableStateOf(config) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var isReconnecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val messages by chatClient.messages.collectAsState()
    val chatConnected by chatClient.isConnected.collectAsState()
    // Chat connection: Agora join/leave is managed inside LiveRtcClassroomView
    DisposableEffect(current.liveId) {
        scope.launch {
            runCatching {
                if (current.chat.enabled) {
                    chatClient.connect(current.chat.wsUrl, current.chat.roomId)
                }
            }.onFailure {
                connectionError = "聊天室連線失敗：${it.message ?: "未知錯誤"}"
            }
        }
        onDispose {
            scope.launch { runCatching { chatClient.disconnect() } }
        }
    }
    // Refresh config before token expiry (see "Known limitations" below)
    LaunchedEffect(current.agora.tokenExpireAt) {
        val expireAt = runCatching { Instant.parse(current.agora.tokenExpireAt) }.getOrNull()
            ?: return@LaunchedEffect
        val delayMs = (expireAt - Clock.System.now()).inWholeMilliseconds - 30_000
        if (delayMs > 0) delay(delayMs)
        isReconnecting = true
        val refreshed = runCatching { onRejoin() }.getOrNull()
        isReconnecting = false
        if (refreshed != null) {
            current = refreshed
        } else {
            connectionError = "Token 續期失敗，畫面可能隨時斷線"
        }
    }
    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(current.teacher.name, color = Color.White, fontSize = 16.sp)
                        if (isReconnecting) {
                            Text("重新連線中...", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Filled.Close, contentDescription = "離開", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            connectionError?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFB00020)).padding(8.dp),
                )
            }
            // Video area: the single RTC entry point
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
            ) {
                LiveRtcClassroomView(
                    modifier = Modifier.fillMaxSize(),
                    session = LiveRtcSession(
                        appId = current.agora.appId,
                        token = current.agora.rtcToken,
                        channelName = current.agora.channelName,
                        uid = current.agora.uid,
                    ),
                    screenUid = current.teacher.screenUid,       // Comes from teacher, not agora
                    cameraUid = current.teacher.cameraUid,       // Comes from teacher, not agora
                    showCamera = current.streamLayout.pipUid != current.streamLayout.mainUid,
                    speakerEnabled = true,
                    isInPip = isInPipMode(),
                )
            }
            // Interaction bar
            if (current.features.canRaiseHand || current.features.canSendReaction) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (current.features.canRaiseHand) {
                        OutlinedButton(onClick = { /* TODO: Raise hand */ }) {
                            Icon(
                                Icons.Filled.PanTool,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("舉手")
                        }
                    }
                    if (current.features.canSendReaction) {
                        listOf("👍", "❤️", "😂", "👏").forEach { emoji ->
                            OutlinedButton(
                                onClick = { scope.launch { chatClient.sendReaction(emoji) } },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) { Text(emoji, fontSize = 16.sp) }
                        }
                    }
                }
            }
            // Chat room
            if (current.chat.enabled) {
                ChatPanel(
                    messages = messages,
                    isConnected = chatConnected,
                    canSendMessage = current.features.canSendMessage,
                    onSend = { text -> scope.launch { chatClient.sendMessage(text) } },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatPanel(
    messages: List<com.sun.kmpstartertemplaterefined.feature_live_presentation.engine.ChatMessage>,
    isConnected: Boolean,
    canSendMessage: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .padding(8.dp),
    ) {
        if (!isConnected) {
            Text("聊天室連線中...", color = Color(0xFFAAAAAA), fontSize = 12.sp)
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "${msg.senderName}：",
                        color = Pink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = msg.content, color = Color.White, fontSize = 13.sp)
                }
            }
        }
        if (canSendMessage) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("輸入訊息...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            onSend(input)
                            input = ""
                        }
                    },
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "送出", tint = Pink)
                }
            }
        } else {
            Text(
                text = "目前無法發言",
                color = Color(0xFF777777),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}