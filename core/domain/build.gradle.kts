plugins {
    alias(libs.plugins.prostuti.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
}
