plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    // from build logic
    id(libs.plugins.build.koin.core.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    id(libs.plugins.build.common.get().pluginId)
}

kotlin {

    androidLibrary {
        namespace = "com.sun.kmpstartertemplaterefined.core"
        compileSdk = 36
        minSdk = 24
    }

    val xcfName = "starter:coreKit"

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // utils
                api(projects.starter.utils)
                // other
                implementation(libs.atomic.fu)
                api(libs.datastore.preferences)
                // network
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}