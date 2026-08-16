plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.confused.onlylist.designsystem"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose — explicit versions, NO BOM (to isolate resolution).
    // Using 1.7.0 (first stable 1.7.x — very well-tested).
    implementation("androidx.compose.foundation:foundation:1.7.0")
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.ui:ui-graphics:1.7.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
    implementation("androidx.compose.runtime:runtime:1.7.0")
    implementation("androidx.compose.animation:animation:1.7.0")
    implementation("androidx.compose.animation:animation-core:1.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")

    // Haze — true frosted glass backdrop blur (per R-9 research).
    // v1.7.2 stable; do NOT use v2-alpha (API not locked).
    implementation("dev.chrisbanes.haze:haze:1.7.2")

    implementation("androidx.core:core-ktx:1.15.0")
}
