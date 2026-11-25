import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            // Derive a namespace from the module path: :feature:mockexam -> app.prostuti.feature.mockexam
            namespace = "app.prostuti" + path.replace(':', '.')
            defaultConfig.consumerProguardFiles("consumer-rules.pro")
            testOptions.targetSdk = ProstutiSdk.TARGET
            lint.targetSdk = ProstutiSdk.TARGET
        }
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions.applyProstutiDefaults()
        }
        dependencies {
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("kotlin-test").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("turbine").get())
        }
    }
}
