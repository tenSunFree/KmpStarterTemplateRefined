package com.sun.kmpstartertemplaterefined.feature_live_presentation

import com.sun.kmpstartertemplaterefined.feature_live_presentation.model.LiveCourseUi

data class LiveStreamingState(
    val isLoading: Boolean = false,
    val courses: List<LiveCourseUi> = emptyList(),
    val errorMessage: String? = null,
)