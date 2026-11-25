pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Prostuti"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:mockbackend")
include(":core:designsystem")
include(":core:ui")

include(":feature:auth")
include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:exams")
include(":feature:learn")
include(":feature:questionbank")
include(":feature:practice")
include(":feature:mockexam")
include(":feature:analytics")
include(":feature:assistant")
include(":feature:planner")
include(":feature:revision")
include(":feature:offline")
include(":feature:community")
include(":feature:profile")
