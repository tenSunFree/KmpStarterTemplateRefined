package com.sun.kmpstartertemplaterefined.feature_live_presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.JoinLiveConfig
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.StreamType
import com.sun.kmpstartertemplaterefined.feature_live_presentation.engine.AgoraEngine
import com.sun.kmpstartertemplaterefined.feature_live_presentation.engine.ChatClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val Pink = Color(0xFFFF3F68)
private val DarkBg = Color(0xFF111111)

/**
 * 直播間畫面。
 *
 * @param config 從 join API 拿到的完整設定
 * @param agoraEngine Agora RTC 引擎（平台實作）
 * @param chatClient WebSocket 聊天室 client（平台或共用實作）
 * @param onRejoin 快過期時重新呼叫 joinLive 拿新 config；失敗時回傳 null
 * @param onExit 使用者離開直播間
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomScreen(
    config: JoinLiveConfig,
    agoraEngine: AgoraEngine,
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

    // ---------- 進場：加入 Agora 頻道 + 連上聊天室 ----------
    // ---------- 離場：確實釋放資源，避免記憶體洩漏 / Agora 頻道卡住 ----------
    DisposableEffect(current.liveId) {
        scope.launch {
            runCatching {
                agoraEngine.joinChannel(
                    appId = current.agora.appId,
                    token = current.agora.rtcToken,
                    channelName = current.agora.channelName,
                    uid = current.agora.uid,
                )
                if (current.chat.enabled) {
                    chatClient.connect(current.chat.wsUrl, current.chat.roomId)
                }
            }.onFailure {
                connectionError = "連線失敗：${it.message ?: "未知錯誤"}"
            }
        }

        onDispose {
            scope.launch {
                runCatching { agoraEngine.leaveChannel() }
                runCatching { chatClient.disconnect() }
            }
        }
    }

    // ---------- Token 快過期前自動續期 ----------
    LaunchedEffect(current.agora.tokenExpireAt) {
        val expireAt = runCatching { Instant.parse(current.agora.tokenExpireAt) }.getOrNull()
        if (expireAt == null) return@LaunchedEffect

        val now = Clock.System.now()
        val delayMs = (expireAt - now).inWholeMilliseconds - 30_000 // 提前 30 秒續期

        if (delayMs > 0) {
            delay(delayMs)
        }

        isReconnecting = true
        val refreshed = runCatching { onRejoin() }.getOrNull()
        isReconnecting = false

        if (refreshed != null) {
            runCatching { agoraEngine.renewToken(refreshed.agora.rtcToken) }
                .onSuccess {
                    // 只更新 token 相關欄位，避免整個 config 換掉造成 DisposableEffect 重新 join
                    current = current.copy(
                        agora = current.agora.copy(
                            rtcToken = refreshed.agora.rtcToken,
                            tokenExpireAt = refreshed.agora.tokenExpireAt,
                        ),
                    )
                }
                .onFailure {
                    connectionError = "Token 續期失敗，畫面可能隨時斷線"
                }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            connectionError?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB00020))
                        .padding(8.dp),
                )
            }

            // ---------- 影像區：主畫面 + PiP ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
            ) {
                val mainUid = current.streamLayout.mainUid
                val pipUid = current.streamLayout.pipUid

                // 主畫面：依 mainType 顯示對應 uid 的畫面
                when (current.streamLayout.mainType) {
                    StreamType.TeacherScreen,
                    StreamType.TeacherCamera,
                        -> agoraEngine.VideoView(
                        uid = mainUid,
                        modifier = Modifier.fillMaxSize(),
                    )
                    StreamType.Unknown -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("無法辨識的畫面版位", color = Color.White)
                        }
                    }
                }

                // PiP 小視窗：只有 mainUid != pipUid 時才顯示（避免主畫面和 PiP 重複）
                if (pipUid != mainUid) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(width = 100.dp, height = 140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray),
                    ) {
                        agoraEngine.VideoView(
                            uid = pipUid,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // ---------- 互動列：舉手 / 表情反應 ----------
            if (current.features.canRaiseHand || current.features.canSendReaction) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (current.features.canRaiseHand) {
                        OutlinedButton(
                            onClick = { /* TODO: 呼叫舉手 API 或透過 WS 傳送事件 */ },
                        ) {
                            Icon(Icons.Filled.PanTool, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("舉手")
                        }
                    }

                    if (current.features.canSendReaction) {
                        listOf("👍", "❤️", "😂", "👏").forEach { emoji ->
                            OutlinedButton(
                                onClick = { scope.launch { chatClient.sendReaction(emoji) } },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // ---------- 聊天室 ----------
            if (current.chat.enabled) {
                ChatPanel(
                    messages = messages,
                    isConnected = chatConnected,
                    canSendMessage = current.features.canSendMessage,
                    onSend = { text -> scope.launch { chatClient.sendMessage(text) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
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