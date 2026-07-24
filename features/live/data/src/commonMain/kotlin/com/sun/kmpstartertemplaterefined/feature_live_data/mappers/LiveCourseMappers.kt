package com.sun.kmpstartertemplaterefined.feature_live_data.mappers

import com.sun.kmpstartertemplaterefined.feature_live_data.remote.dto.LiveCourseDto
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.LiveCourse

fun LiveCourseDto.toDomain(): LiveCourse = LiveCourse(
    courseId = courseId,
    liveId = liveId,
    title = title,
    category = category,
    level = level,
    courseType = courseType,
    status = status,
    scheduledStartAt = scheduledStartAt,
    startedAt = startedAt,
    endedAt = endedAt,
    teacherId = teacher.teacherId,
    teacherName = teacher.name,
    teacherAvatarUrl = teacher.avatarUrl,
    thumbnailUrl = thumbnailUrl,
    textbookUrl = textbookUrl,
    viewerCount = viewerCount,
    isReminderEnabled = isReminderEnabled,
    canJoin = canJoin,
)