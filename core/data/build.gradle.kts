plugins {
    alias(libs.plugins.prostuti.android.library)
    alias(libs.plugins.prostuti.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
}
