package com.sun.kmpstartertemplaterefined.feature_live_data.mappers

import com.sun.kmpstartertemplaterefined.feature_live_data.remote.dto.JoinLiveDataDto
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.*

private fun String.toStreamType(): StreamType = when (this) {
    "teacher_screen" -> StreamType.TeacherScreen
    "teacher_camera" -> StreamType.TeacherCamera
    else -> StreamType.Unknown
}

fun JoinLiveDataDto.toDomain(): JoinLiveConfig = JoinLiveConfig(
    courseId = courseId,
    liveId = liveId,
    agora = AgoraConfig(
        appId = agora.appId,
        channelName = agora.channelName,
        rtcToken = agora.rtcToken,
        tokenExpireAt = agora.tokenExpireAt,
        uid = agora.uid,
        role = agora.role,
    ),
    chat = LiveChatConfig(
        enabled = chat.enabled,
        provider = chat.provider,
        roomId = chat.roomId,
        wsUrl = chat.wsUrl,
    ),
    features = LiveFeatures(
        canPublishAudio = features.canPublishAudio,
        canPublishVideo = features.canPublishVideo,
        canRaiseHand = features.canRaiseHand,
        canSendMessage = features.canSendMessage,
        canSendReaction = features.canSendReaction,
    ),
    streamLayout = StreamLayout(
        mainType = streamLayout.mainType.toStreamType(),
        mainUid = streamLayout.mainUid,
        pipType = streamLayout.pipType.toStreamType(),
        pipUid = streamLayout.pipUid,
    ),
    teacher = LiveTeacherConfig(
        teacherId = teacher.teacherId,
        name = teacher.name,
        avatarUrl = teacher.avatarUrl,
        cameraUid = teacher.cameraUid,
        screenUid = teacher.screenUid,
    ),
)