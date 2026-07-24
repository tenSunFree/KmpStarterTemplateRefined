package com.sun.kmpstartertemplaterefined.feature_live_domain.models

data class JoinLiveConfig(
    val courseId: String,
    val liveId: String,
    val agora: AgoraConfig,
    val chat: LiveChatConfig,
    val features: LiveFeatures,
    val streamLayout: StreamLayout,
    val teacher: LiveTeacherConfig,
)

data class AgoraConfig(
    val appId: String,
    val channelName: String,
    val rtcToken: String,
    val tokenExpireAt: String, // ISO8601
    val uid: Int,
    val role: String,
)

data class LiveChatConfig(
    val enabled: Boolean,
    val provider: String,
    val roomId: String,
    val wsUrl: String,
)

data class LiveFeatures(
    val canPublishAudio: Boolean,
    val canPublishVideo: Boolean,
    val canRaiseHand: Boolean,
    val canSendMessage: Boolean,
    val canSendReaction: Boolean,
)

enum class StreamType { TeacherScreen, TeacherCamera, Unknown }

data class StreamLayout(
    val mainType: StreamType,
    val mainUid: Int,
    val pipType: StreamType,
    val pipUid: Int,
)

data class LiveTeacherConfig(
    val teacherId: String,
    val name: String,
    val avatarUrl: String?,
    val cameraUid: Int,
    val screenUid: Int,
)