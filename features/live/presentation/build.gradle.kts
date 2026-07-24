import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
    id(libs.plugins.build.koin.compose.get().pluginId)
    id(libs.plugins.build.common.get().pluginId)
    id(libs.plugins.build.compose.multiplatform.get().pluginId)
}

val generatedAgoraConfigDir = layout.buildDirectory.dir("generated/agoraConfig/androidMain/kotlin")

val generateAgoraLocalConfig by tasks.registering {
    val localPropertiesFile = rootProject.file("local.properties")
    inputs.file(localPropertiesFile).optional()
    outputs.dir(generatedAgoraConfigDir)
    doLast {
        val props = Properties()
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { props.load(it) }
        }
        fun String.esc() = replace("\\", "\\\\").replace("\"", "\\\"")
        val appId = props.getProperty("AGORA_APP_ID", "").esc()
        val token = props.getProperty("AGORA_TOKEN", "").esc()
        val outputDir = generatedAgoraConfigDir.get().asFile.resolve(
            "com/sun/kmpstartertemplaterefined/feature_live_presentation/rtc"
        )
        outputDir.mkdirs()
        outputDir.resolve("AgoraLocalConfig.android.kt").writeText(
            """
            package com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc
        
            actual object AgoraLocalConfig {
                actual val appId: String = "$appId"
                actual val token: String = "$token"
            }
            """.trimIndent()
        )
    }
}

compose.resources {
    generateResClass = never
}

val packageName = "com.sun.kmpstartertemplaterefined.feature_live_presentation"

kotlin {
    androidLibrary {
        namespace = packageName
        compileSdk {
            version = release(version = libs.versions.android.compileSdk.get().toInt())
        }
        minSdk {
            version = release(libs.versions.android.minSdk.get().toInt())
        }
    }

    val xcfName = "starter:featureLivePresentationKit"

    iosArm64 {
        binaries.framework { baseName = xcfName }
    }
    iosSimulatorArm64 {
        binaries.framework { baseName = xcfName }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(projects.features.live.domain)
                implementation(projects.features.navigation)
                implementation(projects.starter.utils)
                implementation(projects.starter.ui.utils)
                implementation(projects.starter.ui.components)
                implementation(libs.kotlinx.datetime)
            }
        }
        androidMain {
            kotlin.srcDir(generatedAgoraConfigDir)
            dependencies {
                api("io.agora.rtc:full-rtc-basic:4.6.3")
            }
        }
        iosMain {
            dependencies {

            }
        }
    }
}

afterEvaluate {
    tasks.named("compileAndroidMain") {
        dependsOn(generateAgoraLocalConfig)
    }
}