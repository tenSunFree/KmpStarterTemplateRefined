plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
    id(libs.plugins.build.koin.core.get().pluginId)
    id(libs.plugins.build.common.get().pluginId)
}

val packageName = "com.sun.kmpstartertemplaterefined.feature_live_domain"

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

    val xcfName = "starter:featureLiveDomainKit"

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
                implementation(projects.starter.core)
            }
        }
        androidMain { dependencies { } }
        iosMain { dependencies { } }
    }
}