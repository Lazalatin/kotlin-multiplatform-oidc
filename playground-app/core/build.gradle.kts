plugins {
    id("org.publicvalue.convention.kotlin.multiplatform")
    alias(libs.plugins.ksp)
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlin.inject.runtime)
                api(libs.kotlinx.coroutines.core)
            }
        }
    }
}

//addKspDependencyForAllTargets(libs.kotlin.inject.compiler)