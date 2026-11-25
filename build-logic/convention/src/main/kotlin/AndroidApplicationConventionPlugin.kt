import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")
        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = ProstutiSdk.TARGET
        }
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions.applyProstutiDefaults()
        }
    }
}
