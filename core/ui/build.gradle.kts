plugins {
    alias(libs.plugins.prostuti.android.library)
    alias(libs.plugins.prostuti.android.compose)
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    api(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
