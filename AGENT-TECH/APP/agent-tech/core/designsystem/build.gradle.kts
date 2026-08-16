plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.confused.agenttech.designsystem"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    api("androidx.compose.foundation:foundation:1.7.0")
    api("androidx.compose.ui:ui:1.7.0")
    api("androidx.compose.ui:ui-graphics:1.7.0")
    api("androidx.compose.ui:ui-tooling-preview:1.7.0")
    api("androidx.compose.runtime:runtime:1.7.0")
    api("androidx.compose.animation:animation:1.7.0")
    api("androidx.compose.animation:animation-core:1.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")
    api("dev.chrisbanes.haze:haze:1.1.1")
    implementation("androidx.core:core-ktx:1.15.0")
}
