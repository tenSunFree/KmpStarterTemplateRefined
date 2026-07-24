package com.sun.kmpstartertemplaterefined.feature_live_presentation.mapper

import com.sun.kmpstartertemplaterefined.feature_live_domain.models.LiveCourse
import com.sun.kmpstartertemplaterefined.feature_live_presentation.model.LiveCourseUi
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun LiveCourse.toUi(): LiveCourseUi {
    val timeText = runCatching {
        val dt = Instant.parse(scheduledStartAt).toLocalDateTime(TimeZone.currentSystemDefault())
        "預計${dt.monthNumber}/${dt.dayOfMonth} " +
                "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')} 開始直播"
    }.getOrDefault(scheduledStartAt)

    return LiveCourseUi(
        courseId = courseId,
        liveId = liveId,
        teacherName = teacherName,
        teacherAvatarUrl = teacherAvatarUrl,
        title = title,
        category = category,
        level = level,
        isRequired = courseType == "required",
        scheduledTime = timeText,
        thumbnailUrl = thumbnailUrl,
        textbookUrl = textbookUrl,
        viewerCount = viewerCount,
        canJoin = canJoin,
    )
}