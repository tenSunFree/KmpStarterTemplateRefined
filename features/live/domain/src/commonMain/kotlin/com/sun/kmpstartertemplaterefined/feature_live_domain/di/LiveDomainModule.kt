package com.sun.kmpstartertemplaterefined.feature_live_domain.di

import com.sun.kmpstartertemplaterefined.feature_live_domain.logics.GetLiveCoursesLogic
import com.sun.kmpstartertemplaterefined.feature_live_domain.logics.JoinLiveLogic
import org.koin.dsl.module

val liveDomainModule = module {
    factory { GetLiveCoursesLogic(repository = get()) }
    factory { JoinLiveLogic(repository = get()) }
}