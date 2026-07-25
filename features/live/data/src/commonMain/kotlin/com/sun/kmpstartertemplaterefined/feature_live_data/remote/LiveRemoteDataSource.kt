package com.sun.kmpstartertemplaterefined.feature_live_data.remote

import com.sun.kmpstartertemplaterefined.feature_live_data.remote.dto.LiveCourseDto
import com.sun.kmpstartertemplaterefined.feature_live_data.remote.dto.JoinLiveDataDto

interface LiveRemoteDataSource {
    suspend fun getLiveCourses(status: String = "scheduled,live"): List<LiveCourseDto>
    suspend fun joinLive(liveId: String, clientType: String): JoinLiveDataDto
}