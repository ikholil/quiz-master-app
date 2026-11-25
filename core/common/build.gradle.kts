plugins {
    alias(libs.plugins.prostuti.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
}
