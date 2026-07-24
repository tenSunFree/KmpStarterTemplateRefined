package com.sun.kmpstartertemplaterefined.feature_live_data.di

import com.sun.kmpstartertemplaterefined.feature_live_data.remote.LiveRemoteDataSource
import com.sun.kmpstartertemplaterefined.feature_live_data.remote.LiveRemoteDataSourceImpl
import com.sun.kmpstartertemplaterefined.feature_live_data.repository.LiveRepositoryImpl
import com.sun.kmpstartertemplaterefined.feature_live_domain.repository.LiveRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun liveDataModule(baseUrl: String) = module {
    single<LiveRemoteDataSource> {
        LiveRemoteDataSourceImpl(
            httpClient = get(named("authHttpClient")),
            baseUrl = baseUrl,
        )
    }
    single<LiveRepository> {
        LiveRepositoryImpl(remoteDataSource = get())
    }
}