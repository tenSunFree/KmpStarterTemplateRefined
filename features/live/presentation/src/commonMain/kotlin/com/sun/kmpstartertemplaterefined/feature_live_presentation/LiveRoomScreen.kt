package com.sun.kmpstartertemplaterefined.feature_live_presentation

import LiveCourseUi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.DisposableEffect
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.LivePipController
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.LivePipNotificationController
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.isInPipMode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc.AgoraLocalConfig
import com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc.LiveRtcClassroomView
import com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc.LiveRtcSession
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sun.kmpstartertemplaterefined.ui_utils.datastore.rememberMutableDataStoreState
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.PipDisplayMode
import com.sun.kmpstartertemplaterefined.ui_components.cards.SelectableListCard
import com.sun.kmpstartertemplaterefined.ui_utils.popups.bottom_sheets.BaseBottomSheet
import com.sun.kmpstartertemplaterefined.ui_utils.theme.Dimens

private val PIP_DISPLAY_MODE_KEY = stringPreferencesKey("live_pip_display_mode")

private val LivePink = Color(0xFFFF3F68)
private val LiveBg = Color.Black
private val PanelBg = Color(0xFF1B1B1B)
private val ControlBg = Color(0xFF3A3A3A)
private val MutedBadge = Color(0xFF555555)

// The UID shared by the teacher's screen (agreed upon with the teacher)
private const val TEACHER_SCREEN_UID = 2000

// The teacher's camera UID (agreed upon with the teacher)
private const val TEACHER_CAMERA_UID = 1000

data class LiveParticipantUi(
    val id: String,
    val name: String,
    val avatarEmoji: String? = null,
    val isMuted: Boolean = true,
    val isTeacher: Boolean = false,
)

data class LiveChatMessageUi(
    val id: String,
    val userName: String,
    val message: String,
)

enum class LiveRoomTab { Chat, Participants }

private val mockParticipants = listOf(
    LiveParticipantUi(
        id = "teacher", name = "KarolChin", avatarEmoji = "👩🏻", isMuted = false, isTeacher = true
    ),
    LiveParticipantUi(id = "jeffery", name = "Jeffery", avatarEmoji = "🌿", isMuted = true),
    LiveParticipantUi(id = "sun", name = "Sun", avatarEmoji = null, isMuted = true),
    LiveParticipantUi(id = "evan", name = "Evan", avatarEmoji = "👦🏻", isMuted = true),
    LiveParticipantUi(id = "jack", name = "Jack", avatarEmoji = null, isMuted = true),
)

private val mockChatMessages = listOf(
    LiveChatMessageUi(id = "1", userName = "Sun", message = "has joined the stream"),
    LiveChatMessageUi(id = "2", userName = "Jack", message = "has joined the stream"),
)

/**
 * Single call-site principle
 *
 * LiveVideoArea appears only once in the codebase (the same call site).
 * Whether or not we are in PiP only changes the appearance through parameters
 * (modifier size and whether other UI is shown), rather than switching the whole
 * Composable branch. This keeps remember state, the RtcEngine, and the FrameLayout
 * all as the same instance, so the view tree is not torn down and rebuilt when PiP changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomScreen(
    course: LiveCourseUi,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(LiveRoomTab.Participants) }
    var showTeacherVideo by remember { mutableStateOf(true) }
    var speakerEnabled by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf("") }
    var showPipSettingsSheet by remember { mutableStateOf(false) }

    // Persisted PiP display preference, defaulting to ScreenOnly (conservative and stable, aligned with mainstream meeting apps)
    val pipDisplayModePref = rememberMutableDataStoreState(
        key = PIP_DISPLAY_MODE_KEY,
        defaultValue = PipDisplayMode.ScreenOnly.name,
    )
    val pipDisplayMode = remember(pipDisplayModePref.value) {
        PipDisplayMode.entries.firstOrNull { it.name == pipDisplayModePref.value }
            ?: PipDisplayMode.ScreenOnly
    }

    // Unified exit point: Whether it's returning from within the app or clicking "End Viewing" in a PiP notification,
    // this path must be used to immediately synchronize the state (without waiting for the asynchronous dispose of Compose).
    // Note: This does not replace the cleanup in onDispose, but rather adds an extra layer of "immediate effect" protection.
    // Repeatedly calling setLiveRoomActive(false) on both sides is a safe idempotent operation.
    val exitLiveRoom = remember(onBack) {
        {
            LivePipNotificationController.unregisterActions()
            LivePipController.setLiveRoomActive(false)
            LivePipController.setVideoPlaying(false)
            onBack()
        }
    }

    DisposableEffect(Unit) {
        LivePipController.setLiveRoomActive(true)
        LivePipController.setCourseTitle(course.title)
        onDispose {
            LivePipController.setLiveRoomActive(false)
            LivePipController.setVideoPlaying(false)
        }
    }

    DisposableEffect(Unit) {
        LivePipNotificationController.registerActions(
            onToggleMuteRequested = { speakerEnabled = !speakerEnabled },
            onStopRequested = exitLiveRoom, // Originally it was onBack, changed to exitLiveRoom
        )
        onDispose {
            LivePipNotificationController.unregisterActions()
        }
    }

    LaunchedEffect(speakerEnabled) {
        LivePipNotificationController.reportMuteState(!speakerEnabled)
    }

    val inPip = isInPipMode()

    // Normal live view: follow the user's eye-toggle switch.
    // PiP: show camera only when both are true: user's eye-toggle is on AND ScreenWithCameraOverlay is selected.
    val showCameraInVideoArea = if (inPip) {
        showTeacherVideo && pipDisplayMode == PipDisplayMode.ScreenWithCameraOverlay
    } else {
        showTeacherVideo
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LiveBg)
            .then(if (inPip) Modifier else Modifier.systemBarsPadding()),
    ) {
        if (!inPip) {
            LiveRoomHeader(
                showTeacherVideo = showTeacherVideo,
                speakerEnabled = speakerEnabled,
                pipDisplayMode = pipDisplayMode,
                onBack = exitLiveRoom, // Originally onBack
                onToggleTeacherVideo = { showTeacherVideo = !showTeacherVideo },
                onToggleSpeaker = { speakerEnabled = !speakerEnabled },
                onOpenPipSettings = { showPipSettingsSheet = true },
            )
        }

        LiveVideoArea(
            course = course,
            showTeacherVideo = showCameraInVideoArea,
            speakerEnabled = speakerEnabled,
            isInPip = inPip,
            modifier = if (inPip) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxWidth().height(240.dp)
            },
        )

        if (!inPip) {
            LiveRoomTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(PanelBg)) {
                when (selectedTab) {
                    LiveRoomTab.Chat -> LiveChatPanel(messages = mockChatMessages)
                    LiveRoomTab.Participants -> LiveParticipantsPanel(participants = mockParticipants)
                }
            }
            LiveBottomBar(
                selectedTab = selectedTab,
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onSend = { inputText = "" },
            )
        }
    }

    if (showPipSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true, // Must be fully expanded to avoid clipping the second card in half-expanded state
        )

        BaseBottomSheet(
            sheetState = sheetState,
            onDismiss = { showPipSettingsSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // Safety guard: prevents clipping if content becomes taller than expected
                    .padding(Dimens.paddingMedium)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = "縮小畫面時的顯示方式",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "選擇進入 PiP 小視窗後，要如何顯示直播畫面。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Dimens.paddingMedium))

                PipDisplayMode.entries.forEachIndexed { index, mode ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    PipModeOptionRow(
                        mode = mode,
                        isSelected = pipDisplayMode == mode,
                        onSelect = {
                            pipDisplayModePref.value = mode.name
                            showPipSettingsSheet = false
                        },
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.paddingMedium))
            }
        }
    }
}

@Composable
private fun PipModeOptionRow(
    mode: PipDisplayMode,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    SelectableListCard(
        modifier = Modifier.fillMaxWidth(),
        isSelected = isSelected,
        onClick = onSelect,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveRoomHeader(
    showTeacherVideo: Boolean,
    speakerEnabled: Boolean,
    pipDisplayMode: PipDisplayMode,
    onBack: () -> Unit,
    onToggleTeacherVideo: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenPipSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "LumaLang",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 32.sp
            )
            Text(
                text = "AI English Academy",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        CircleIconButton(
            onClick = onToggleTeacherVideo,
            backgroundColor = if (showTeacherVideo) Color.White else ControlBg,
        ) {
            Icon(
                imageVector = if (showTeacherVideo) Icons.Filled.RemoveRedEye else Icons.Filled.VisibilityOff,
                contentDescription = "切換老師視訊",
                tint = if (showTeacherVideo) Color.Black else Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        // PiP display mode
        CircleIconButton(
            onClick = onOpenPipSettings,
            backgroundColor = ControlBg,
        ) {
            Icon(
                imageVector = Icons.Filled.PictureInPictureAlt,
                contentDescription = "縮小視窗顯示設定 (${pipDisplayMode.label})",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        CircleIconButton(
            onClick = onToggleSpeaker,
            backgroundColor = ControlBg,
        ) {
            Icon(
                imageVector = if (speakerEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                contentDescription = "喇叭",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun LiveVideoArea(
    course: LiveCourseUi,
    showTeacherVideo: Boolean,
    speakerEnabled: Boolean,
    isInPip: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth().height(240.dp),
) {
    val session = remember(course.roomId) {
        LiveRtcSession(
            appId = AgoraLocalConfig.appId,
            token = AgoraLocalConfig.token,
            channelName = course.roomId,
            uid = 10000,
        )
    }
    LiveRtcClassroomView(
        modifier = modifier.background(Color.Black),
        session = session,
        screenUid = TEACHER_SCREEN_UID,
        cameraUid = TEACHER_CAMERA_UID,
        showCamera = showTeacherVideo,
        speakerEnabled = speakerEnabled,
        isInPip = isInPip,
    )
}

// Chat / Participants Tab column
@Composable
private fun LiveRoomTabs(
    selectedTab: LiveRoomTab,
    onTabSelected: (LiveRoomTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(PanelBg),
    ) {
        LiveTabItem(
            text = "Chat",
            selected = selectedTab == LiveRoomTab.Chat,
            onClick = { onTabSelected(LiveRoomTab.Chat) },
            modifier = Modifier.weight(1f),
        )
        LiveTabItem(
            text = "Participants",
            selected = selectedTab == LiveRoomTab.Participants,
            onClick = { onTabSelected(LiveRoomTab.Participants) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LiveTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF888888),
            fontSize = 20.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(3.dp)
                .background(if (selected) LivePink else Color.Transparent),
        )
    }
}

// Participants panel
@Composable
private fun LiveParticipantsPanel(participants: List<LiveParticipantUi>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(participants) { participant ->
            ParticipantItem(participant = participant)
        }
    }
}

@Composable
private fun ParticipantItem(participant: LiveParticipantUi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier.size(82.dp).clip(CircleShape).background(Color(0xFFEFEFEF))
                    .then(
                        if (participant.isTeacher) Modifier.border(4.dp, LivePink, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = participant.avatarEmoji ?: "👤", fontSize = 38.sp)
            }
            // Mute badge (bottom right corner)
            if (participant.isMuted) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(MutedBadge)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = "已靜音",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = participant.name,
            color = Color(0xFFDDDDDD),
            fontSize = 16.sp,
            maxLines = 1,
        )
    }
}

// Chat panel
@Composable
private fun LiveChatPanel(messages: List<LiveChatMessageUi>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(messages) { message ->
            LiveChatMessageBubble(message = message)
        }
    }
}

@Composable
private fun LiveChatMessageBubble(message: LiveChatMessageUi) {
    Row(
        modifier = Modifier.wrapContentWidth().clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF444444)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "👤", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message.userName,
            color = Color(0xFF1E88E5),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = message.message,
            color = Color(0xFFBBBBBB),
            fontSize = 16.sp,
        )
    }
}

// Bottom Operation Column
@Composable
private fun LiveBottomBar(
    selectedTab: LiveRoomTab,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hand button
        CircleIconButton(onClick = {}, backgroundColor = ControlBg) {
            Icon(
                imageVector = Icons.Filled.WavingHand,
                contentDescription = "舉手",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (selectedTab == LiveRoomTab.Chat) {
            TextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = { Text(text = "輸入...", color = Color(0xFFAAAAAA)) },
                modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ControlBg,
                    unfocusedContainerColor = ControlBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
                trailingIcon = {
                    IconButton(onClick = onSend) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "送出",
                            tint = Color.White,
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        CircleIconButton(onClick = {}, backgroundColor = ControlBg) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "愛心",
                tint = LivePink,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier.size(52.dp).clip(CircleShape).background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}