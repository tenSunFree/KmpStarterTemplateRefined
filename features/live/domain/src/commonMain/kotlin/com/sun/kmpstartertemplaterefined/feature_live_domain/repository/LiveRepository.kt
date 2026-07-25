package com.sun.kmpstartertemplaterefined.feature_live_domain.repository

import com.sun.kmpstartertemplaterefined.feature_live_domain.models.JoinLiveConfig
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.LiveCourse

interface LiveRepository {
    suspend fun getLiveCourses(status: String = "scheduled,live"): List<LiveCourse>
    suspend fun joinLive(liveId: String, clientType: String): JoinLiveConfig
}