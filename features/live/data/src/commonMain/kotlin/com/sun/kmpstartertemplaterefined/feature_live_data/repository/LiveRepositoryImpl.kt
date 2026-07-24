package com.sun.kmpstartertemplaterefined.feature_live_data.repository

import com.sun.kmpstartertemplaterefined.feature_live_data.mappers.toDomain
import com.sun.kmpstartertemplaterefined.feature_live_data.remote.LiveRemoteDataSource
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.JoinLiveConfig
import com.sun.kmpstartertemplaterefined.feature_live_domain.models.LiveCourse
import com.sun.kmpstartertemplaterefined.feature_live_domain.repository.LiveRepository

class LiveRepositoryImpl(
    private val remoteDataSource: LiveRemoteDataSource,
) : LiveRepository {

    override suspend fun getLiveCourses(status: String): List<LiveCourse> =
        remoteDataSource.getLiveCourses(status).map { it.toDomain() }

    override suspend fun joinLive(liveId: String, clientType: String): JoinLiveConfig =
        remoteDataSource.joinLive(liveId, clientType).toDomain()
}