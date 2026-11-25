import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: Android library + Compose + Hilt + navigation, wired to the shared core modules.
 * Features never depend on each other; the :app module composes their navigation graphs.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("prostuti.android.library")
        pluginManager.apply("prostuti.android.compose")
        pluginManager.apply("prostuti.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))

            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("compose-material3").get())
            add("implementation", libs.findLibrary("compose-material-icons").get())
            add("implementation", libs.findLibrary("kotlinx-serialization-json").get())

            add("testImplementation", libs.findLibrary("robolectric").get())
            add("testImplementation", libs.findLibrary("androidx-test-core").get())
            add("testImplementation", libs.findLibrary("compose-ui-test-junit4").get())
            add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())
            add("androidTestImplementation", libs.findLibrary("compose-ui-test-junit4").get())
            add("androidTestImplementation", libs.findLibrary("androidx-test-ext-junit").get())
            add("androidTestImplementation", libs.findLibrary("androidx-test-runner").get())
        }
    }
}
