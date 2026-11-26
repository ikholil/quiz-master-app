package app.prostuti.app.di

import app.prostuti.app.BuildConfig
import app.prostuti.core.network.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun apiConfig() = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        appVersion = BuildConfig.VERSION_NAME,
        enableHttpLogging = BuildConfig.DEBUG,
    )
}
