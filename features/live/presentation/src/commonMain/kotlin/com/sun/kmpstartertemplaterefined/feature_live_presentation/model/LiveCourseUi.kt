package com.sun.kmpstartertemplaterefined.feature_live_presentation.model

data class LiveCourseUi(
    val courseId: String,
    val liveId: String,
    val teacherName: String,
    val teacherAvatarUrl: String?,
    val title: String,
    val category: String,
    val level: String,
    val isRequired: Boolean,
    val scheduledTime: String,
    val thumbnailUrl: String?,
    val textbookUrl: String?,
    val viewerCount: Int,
    val canJoin: Boolean,
)