package com.sun.kmpstartertemplaterefined.feature_navigation.screens

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class StarterScreens : NavKey {
    @Serializable
    data object Welcome : StarterScreens()

    @Serializable
    data object Splash : StarterScreens()

    @Serializable
    data object Onboarding : StarterScreens()

    @Serializable
    data object Login : StarterScreens()

    @Serializable
    data object Purchases : StarterScreens()

    @Serializable
    data object Main : StarterScreens()

    @Serializable
    data class LessonPlayer(val lessonId: String) : StarterScreens()

    @Serializable
    data class LiveRoom(
        val liveId: String,
    ) : StarterScreens()
}