package com.sun.kmpstartertemplaterefined.feature_live_domain.logics

import com.sun.kmpstartertemplaterefined.feature_live_domain.models.JoinLiveConfig
import com.sun.kmpstartertemplaterefined.feature_live_domain.repository.LiveRepository

class JoinLiveLogic(
    private val repository: LiveRepository,
) {
    suspend operator fun invoke(liveId: String, clientType: String): JoinLiveConfig =
        repository.joinLive(liveId, clientType)
}