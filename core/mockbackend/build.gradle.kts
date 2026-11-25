/*
 * DEVELOPMENT ONLY. In-memory implementations of the repository interfaces with sample content,
 * used by the `mock` product flavor so the app can be built and demoed without the backend.
 * The `live` flavor (the only one shipped to users) does not depend on this module.
 */
plugins {
    alias(libs.plugins.prostuti.android.library)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)
}
