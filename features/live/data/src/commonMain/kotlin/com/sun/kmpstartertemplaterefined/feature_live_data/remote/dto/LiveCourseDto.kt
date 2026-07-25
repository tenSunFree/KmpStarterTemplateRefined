package com.sun.kmpstartertemplaterefined.feature_live_data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveCoursesResponseDto(
    val status: Boolean,
    val message: String,
    val data: LiveCoursesDataDto,
)

@Serializable
data class LiveCoursesDataDto(
    val data: List<LiveCourseDto>,
)

@Serializable
data class LiveCourseDto(
    val courseId: String,
    val liveId: String,
    val title: String,
    val category: String,
    val level: String,
    val courseType: String,
    val status: String,
    val scheduledStartAt: String,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val teacher: LiveCourseTeacherDto,
    val thumbnailUrl: String? = null,
    val textbookUrl: String? = null,
    val viewerCount: Int = 0,
    val isReminderEnabled: Boolean = false,
    val canJoin: Boolean = false,
)

@Serializable
data class LiveCourseTeacherDto(
    val teacherId: String,
    val name: String,
    val avatarUrl: String? = null,
)