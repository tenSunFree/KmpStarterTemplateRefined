package com.sun.kmpstartertemplaterefined.feature_live_data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class JoinLiveRequestDto(
    val clientType: String,
)

@Serializable
data class JoinLiveResponseDto(
    val status: Boolean,
    val message: String,
    val data: JoinLiveDataDto,
)

@Serializable
data class JoinLiveDataDto(
    val agora: JoinLiveAgoraDto,
    val chat: JoinLiveChatDto,
    val courseId: String,
    val features: JoinLiveFeaturesDto,
    val liveId: String,
    val streamLayout: JoinLiveStreamLayoutDto,
    val teacher: JoinLiveTeacherDto,
)

@Serializable
data class JoinLiveAgoraDto(
    val appId: String,
    val channelName: String,
    val role: String,
    val rtcToken: String,
    val tokenExpireAt: String,
    val uid: Int,
)

@Serializable
data class JoinLiveChatDto(
    val enabled: Boolean,
    val provider: String,
    val roomId: String,
    val wsUrl: String,
)

@Serializable
data class JoinLiveFeaturesDto(
    val canPublishAudio: Boolean = false,
    val canPublishVideo: Boolean = false,
    val canRaiseHand: Boolean = false,
    val canSendMessage: Boolean = false,
    val canSendReaction: Boolean = false,
)

@Serializable
data class JoinLiveStreamLayoutDto(
    val mainType: String,
    val mainUid: Int,
    val pipType: String,
    val pipUid: Int,
)

@Serializable
data class JoinLiveTeacherDto(
    val teacherId: String,
    val name: String,
    val avatarUrl: String? = null,
    val cameraUid: Int,
    val screenUid: Int,
)