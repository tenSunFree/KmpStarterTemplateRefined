package com.sun.kmpstartertemplaterefined.feature_live_presentation.di

import com.sun.kmpstartertemplaterefined.feature_live_presentation.LiveRoomViewModel
import com.sun.kmpstartertemplaterefined.feature_live_presentation.LiveStreamingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val livePresentationModule = module {
    viewModel {
        LiveStreamingViewModel(getLiveCoursesLogic = get())
    }
    viewModel {
        LiveRoomViewModel(joinLiveLogic = get())
    }
}