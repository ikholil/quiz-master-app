import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

object ProstutiSdk {
    const val COMPILE = 35
    const val TARGET = 35
    /** Android 7.0 — keeps the app installable on most affordable phones still in use in Bangladesh. */
    const val MIN = 24
}

internal fun Project.configureKotlinAndroid(extension: CommonExtension<*, *, *, *, *, *>) {
    extension.apply {
        compileSdk = ProstutiSdk.COMPILE
        defaultConfig {
            minSdk = ProstutiSdk.MIN
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables.useSupportLibrary = true
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
        packaging {
            resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
        }
    }
    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
    }
}

internal fun KotlinJvmCompilerOptions.applyProstutiDefaults() {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.addAll(
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-opt-in=kotlinx.coroutines.FlowPreview",
    )
}
