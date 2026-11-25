plugins {
    `kotlin-dsl`
}

group = "app.prostuti.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "prostuti.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "prostuti.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "prostuti.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "prostuti.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidRoom") {
            id = "prostuti.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("hilt") {
            id = "prostuti.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "prostuti.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
