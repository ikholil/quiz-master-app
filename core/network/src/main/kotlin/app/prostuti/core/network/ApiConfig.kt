package app.prostuti.core.network

/** Provided by :app from BuildConfig so each flavor/build type can point to its own backend. */
data class ApiConfig(
    val baseUrl: String,
    val appVersion: String,
    val enableHttpLogging: Boolean,
)
