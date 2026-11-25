plugins {
    alias(libs.plugins.prostuti.android.library)
    alias(libs.plugins.prostuti.android.compose)
}

dependencies {
    api(libs.compose.material3)
    api(libs.compose.foundation)
    api(libs.compose.animation)
    api(libs.compose.ui)
    api(libs.compose.material.icons)
    implementation(libs.androidx.core.ktx)
}
