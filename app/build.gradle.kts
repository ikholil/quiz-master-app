import java.util.Properties

plugins {
    alias(libs.plugins.prostuti.android.application)
    alias(libs.plugins.prostuti.android.compose)
    alias(libs.plugins.prostuti.hilt)
    alias(libs.plugins.kotlin.serialization)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "app.prostuti.app"

    defaultConfig {
        applicationId = "app.prostuti"
        versionCode = 1
        versionName = "1.0.0"
        // Only ship the two UI languages we translate; trims unused library resources.
        resourceConfigurations += listOf("en", "bn")
    }

    buildFeatures.buildConfig = true
    testOptions.unitTests.isIncludeAndroidResources = true

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    flavorDimensions += "backend"
    productFlavors {
        /** Real NestJS backend. The only flavor distributed to students. */
        create("live") {
            dimension = "backend"
            buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("prostuti.apiBaseUrl").getOrElse("https://api.prostuti.app/")}\"")
            buildConfigField("boolean", "MOCK_BACKEND", "false")
        }
        /** Development only: in-memory sample data from :core:mockbackend. */
        create("mock") {
            dimension = "backend"
            applicationIdSuffix = ".mock"
            versionNameSuffix = "-mock"
            buildConfigField("String", "API_BASE_URL", "\"https://mock.invalid/\"")
            buildConfigField("boolean", "MOCK_BACKEND", "true")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystoreProps.isNotEmpty()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
}

// Never produce a mock release build.
androidComponents {
    beforeVariants { variant ->
        if (variant.buildType == "release" && variant.productFlavors.any { it.second == "mock" }) variant.enable = false
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    "mockImplementation"(project(":core:mockbackend"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:exams"))
    implementation(project(":feature:learn"))
    implementation(project(":feature:questionbank"))
    implementation(project(":feature:practice"))
    implementation(project(":feature:mockexam"))
    implementation(project(":feature:analytics"))
    implementation(project(":feature:assistant"))
    implementation(project(":feature:planner"))
    implementation(project(":feature:revision"))
    implementation(project(":feature:offline"))
    implementation(project(":feature:community"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
