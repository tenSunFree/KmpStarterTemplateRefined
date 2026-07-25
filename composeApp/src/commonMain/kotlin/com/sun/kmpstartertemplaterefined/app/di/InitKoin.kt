package com.sun.kmpstartertemplaterefined.app.di

import com.sun.kmpstartertemplaterefined.app.KmpAppInitializer
import com.sun.kmpstartertemplaterefined.core.datastore.di.dataStoreModule
import com.sun.kmpstartertemplaterefined.core.events.di.eventsModule
import com.sun.kmpstartertemplaterefined.navigation.navigationModule
import com.sun.kmpstartertemplaterefined.feature_analytics_data.di.analyticsDataModule
import com.sun.kmpstartertemplaterefined.feature_auth_data.config.AuthConfig
import com.sun.kmpstartertemplaterefined.feature_auth_data.di.authDataModule
import com.sun.kmpstartertemplaterefined.feature_auth_domain.di.authDomainModule
import com.sun.kmpstartertemplaterefined.feature_auth_presentation.di.authPresentationModule
import com.sun.kmpstartertemplaterefined.feature_core_data.di.coreDataModule
import com.sun.kmpstartertemplaterefined.feature_core_domain.di.coreDomainModule
import com.sun.kmpstartertemplaterefined.feature_core_presentation.di.corePresentationModule
import com.sun.kmpstartertemplaterefined.feature_database.di.databaseModule
import com.sun.kmpstartertemplaterefined.feature_notifications_core.notificationsCoreModule
import com.sun.kmpstartertemplaterefined.feature_notifications_local.notificationsLocalModule
import com.sun.kmpstartertemplaterefined.feature_notifications_push.notificationsPushModule
import com.sun.kmpstartertemplaterefined.feature_purchases_data.di.purchasesDataModule
import com.sun.kmpstartertemplaterefined.feature_purchases_domain.di.purchasesDomainModule
import com.sun.kmpstartertemplaterefined.feature_purchases_presentation.di.purchasesPresentationModule
import com.sun.kmpstartertemplaterefined.feature_remote_config_data.di.remoteConfigDataModule
import com.sun.kmpstartertemplaterefined.feature_remote_config_domain.di.remoteConfigDomainModule
import com.sun.kmpstartertemplaterefined.starter_resources.di.resourceModule
import com.sun.kmpstartertemplaterefined.feature_your_feature_data.di.featureYourDataModule
import com.sun.kmpstartertemplaterefined.feature_your_feature_domain.di.featureYourDomainModule
import com.sun.kmpstartertemplaterefined.feature_your_feature_presentation.di.featureYourPresentationModule
import com.sun.kmpstartertemplaterefined.utils.di.utilsModule
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import com.sun.kmpstartertemplaterefined.feature_lessons_data.di.lessonsDataModule
import com.sun.kmpstartertemplaterefined.feature_lessons_domain.di.lessonsDomainModule
import com.sun.kmpstartertemplaterefined.feature_lessons_presentation.di.lessonsPresentationModule
import com.sun.kmpstartertemplaterefined.feature_live_domain.di.liveDomainModule
import com.sun.kmpstartertemplaterefined.feature_live_presentation.di.livePresentationModule
import com.sun.kmpstartertemplaterefined.feature_live_data.di.liveDataModule

private val starterModules = module {
    includes(
        /*Starter Core Modules*/
        coreDataModule,
        coreDomainModule,
        corePresentationModule,
        utilsModule,
        eventsModule,
        dataStoreModule,
        /*Feature: Database*/
        databaseModule,
        /*Feature: Purchases*/
        purchasesDataModule,
        purchasesDomainModule,
        purchasesPresentationModule,
        /*Feature: Analytics*/
        analyticsDataModule,
        /*Feature: Navigation*/
        navigationModule,
        /*Feature: RemoteConfig*/
        remoteConfigDataModule,
        remoteConfigDomainModule,
        resourceModule,
        /*Feature: Notifications*/
        notificationsCoreModule,
        notificationsLocalModule,
        notificationsPushModule,
        /*Feature: Auth*/
        authDomainModule,
        authPresentationModule,
        /*Feature: Lessons*/
        lessonsDomainModule,
        lessonsPresentationModule,
    )
}

private val kmpAppInitializerModule = module {
    singleOf(::KmpAppInitializer)
}

internal fun initKoin(
    authBaseUrl: String = "http://10.0.2.2:8080/api/v1",
    config: KoinAppDeclaration? = null,
) {
    startKoin {
        config?.invoke(this)
        modules(
            starterModules,
            kmpAppInitializerModule,
            /*Feature: Auth - data need baseUrl*/
            authDataModule(AuthConfig(baseUrl = authBaseUrl)),
            /*Feature: Lessons - data need baseUrl*/
            lessonsDataModule(baseUrl = authBaseUrl),
            /*Your Feature*/
            featureYourDataModule,
            featureYourDomainModule,
            featureYourPresentationModule,
            liveDataModule(baseUrl = authBaseUrl),   // ← 改成函式呼叫，帶 baseUrl
            liveDomainModule,      // ← 新增
            livePresentationModule, // ← 新增
        )
    }
}