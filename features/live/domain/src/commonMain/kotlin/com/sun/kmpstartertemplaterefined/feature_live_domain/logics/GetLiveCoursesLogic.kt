package com.sun.kmpstartertemplaterefined.feature_live_domain.logics

import com.sun.kmpstartertemplaterefined.feature_live_domain.models.LiveCourse
import com.sun.kmpstartertemplaterefined.feature_live_domain.repository.LiveRepository

class GetLiveCoursesLogic(
    private val repository: LiveRepository,
) {
    suspend operator fun invoke(status: String = "scheduled,live"): List<LiveCourse> =
        repository.getLiveCourses(status)
}