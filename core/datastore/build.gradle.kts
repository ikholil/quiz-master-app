plugins {
    alias(libs.plugins.prostuti.android.library)
    alias(libs.plugins.prostuti.hilt)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
