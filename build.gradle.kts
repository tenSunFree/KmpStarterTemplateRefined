plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.android) apply false

    alias(libs.plugins.kover) // Apply to the root as an aggregation module
}

// Apply the Kover plugin to every submodule
// (build-logic is included via includeBuild, so it is not a subproject and is unaffected)
subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

dependencies {
    subprojects.forEach { sub ->
        kover(sub)
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R\$*",
                    "*ComposableSingletons*",
                    "*_Impl", // Room(KSP)-generated classes end with "_Impl", so handwritten XxxRepositoryImpl classes won't be excluded by mistake
                    "*.MainActivity",
                    "*.MyApplication",
                )
                packages("*.generated.resources")
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
        total {
            xml {
                onCheck = false
                xmlFile.set(layout.buildDirectory.file("reports/kover/report.xml"))
            }
            html {
                onCheck = false
                htmlDir.set(layout.buildDirectory.dir("reports/kover/html"))
            }
        }
    }
}