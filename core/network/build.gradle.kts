plugins {
    alias(libs.plugins.prostuti.android.library)
    alias(libs.plugins.prostuti.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    buildFeatures.buildConfig = false
}

dependencies {
    api(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    api(libs.retrofit)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.kotlinx.serialization)
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.okhttp.mockwebserver)
}
