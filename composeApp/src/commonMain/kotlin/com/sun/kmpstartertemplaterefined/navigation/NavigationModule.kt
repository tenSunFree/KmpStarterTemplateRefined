package com.sun.kmpstartertemplaterefined.navigation

import com.sun.kmpstartertemplaterefined.feature_auth_presentation.screens.LoginScreen
import com.sun.kmpstartertemplaterefined.core.ui.screens.main.MainScreen
import com.sun.kmpstartertemplaterefined.core.ui.screens.welcome.WelcomeScreen
import com.sun.kmpstartertemplaterefined.feature_core_presentation.screens.OnboardingV1Screen
import com.sun.kmpstartertemplaterefined.feature_core_presentation.screens.SplashScreen
import com.sun.kmpstartertemplaterefined.feature_navigation.StarterNavigator
import com.sun.kmpstartertemplaterefined.feature_navigation.di.navigationCoreModule
import com.sun.kmpstartertemplaterefined.feature_navigation.screens.StarterScreens
import com.sun.kmpstartertemplaterefined.feature_purchases_presentation.ui.screens.PurchasesScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import com.sun.kmpstartertemplaterefined.feature_lessons_presentation.screens.LessonPlayerScreen
import com.sun.kmpstartertemplaterefined.feature_live_presentation.LiveRoomRoute

@OptIn(KoinExperimentalAPI::class)
val navigationModule = module {
    includes(navigationCoreModule)
    navigation<StarterScreens.Welcome> { _ ->
        val navigator = StarterNavigator.getCurrent()
        WelcomeScreen(
            onGetStartedClick = {
                navigator.navigateTo(
                    route = StarterScreens.Purchases
                )
            }
        )
    }
    navigation<StarterScreens.Splash> { _ ->
        val navigator = StarterNavigator.getCurrent()
        SplashScreen(
            onNavigateToLogin = {
                navigator.popAndNavigate(route = StarterScreens.Login)
            },
            onNavigateToMain = {
                // popAllAndNavigate clears the entire backstack; pressing back will not return to Splash/Login.
                navigator.popAllAndNavigate(route = StarterScreens.Main)
            },
            onNavigateToOnboarding = {
                navigator.popAndNavigate(route = StarterScreens.Onboarding)
            }
        )
    }
    navigation<StarterScreens.Onboarding> { _ ->
        val navigator = StarterNavigator.getCurrent()
        OnboardingV1Screen(
            onNavigate = {
                navigator.popAndNavigate(
                    route = StarterScreens.Welcome
                )
            }
        )
    }
    navigation<StarterScreens.Purchases> { _ ->
        val navigator = StarterNavigator.getCurrent()
        PurchasesScreen(
            onNavigate = {
                navigator.navigateUp()
            }
        )
    }
    // After successful login, navigate to Main and clear the entire backstack
    // to prevent redirection to the login page
    navigation<StarterScreens.Login> { _ ->
        val navigator = StarterNavigator.getCurrent()
        LoginScreen(
            onNavigateToMain = {
                navigator.popAllAndNavigate(route = StarterScreens.Main)
            }
        )
    }
    // Main route
    navigation<StarterScreens.Main> { _ ->
        MainScreen()
    }
    navigation<StarterScreens.LessonPlayer> { route ->
        LessonPlayerScreen(lessonId = route.lessonId)
    }
    // LiveRoom route
    navigation<StarterScreens.LiveRoom> { route ->
        val navigator = StarterNavigator.getCurrent()
        LiveRoomRoute(
            liveId = route.liveId,
            onExit = { navigator.navigateUp() },
        )
    }
}