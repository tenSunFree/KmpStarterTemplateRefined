package com.sun.kmpstartertemplaterefined.feature_live_domain.models

data class LiveCourse(
    val courseId: String,
    val liveId: String,
    val title: String,
    val category: String,
    val level: String,
    val courseType: String,
    val status: String,
    val scheduledStartAt: String,
    val startedAt: String?,
    val endedAt: String?,
    val teacherId: String,
    val teacherName: String,
    val teacherAvatarUrl: String?,
    val thumbnailUrl: String?,
    val textbookUrl: String?,
    val viewerCount: Int,
    val isReminderEnabled: Boolean,
    val canJoin: Boolean,
)