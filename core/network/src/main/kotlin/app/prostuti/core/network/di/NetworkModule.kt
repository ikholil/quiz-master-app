package app.prostuti.core.network.di

import app.prostuti.core.network.ApiConfig
import app.prostuti.core.network.NetworkJson
import app.prostuti.core.network.api.*
import app.prostuti.core.network.auth.AuthInterceptor
import app.prostuti.core.network.auth.ClientHeadersInterceptor
import app.prostuti.core.network.auth.RetryInterceptor
import app.prostuti.core.network.auth.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun json(): Json = NetworkJson

    /** Base client: shared connection pool, small HTTP cache, generous timeouts for slow networks. */
    @Provides @Singleton @Named("base")
    fun baseClient(config: ApiConfig, @Named("cacheDir") cacheDir: File): OkHttpClient =
        OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http"), 10L * 1024 * 1024))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(ClientHeadersInterceptor(config))
            .apply {
                if (config.enableHttpLogging) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    })
                }
            }
            .build()

    @Provides @Singleton @Named("auth")
    fun authClient(
        @Named("base") base: OkHttpClient,
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient = base.newBuilder()
        .addInterceptor(authInterceptor)
        .addInterceptor(RetryInterceptor())
        .authenticator(authenticator)
        .build()

    @Provides @Singleton
    fun retrofit(config: ApiConfig, @Named("auth") client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun refreshApi(config: ApiConfig, @Named("base") client: OkHttpClient, json: Json): RefreshApi =
        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RefreshApi::class.java)

    @Provides @Singleton fun authApi(r: Retrofit): AuthApi = r.create(AuthApi::class.java)
    @Provides @Singleton fun profileApi(r: Retrofit): ProfileApi = r.create(ProfileApi::class.java)
    @Provides @Singleton fun examApi(r: Retrofit): ExamApi = r.create(ExamApi::class.java)
    @Provides @Singleton fun learningApi(r: Retrofit): LearningApi = r.create(LearningApi::class.java)
    @Provides @Singleton fun questionApi(r: Retrofit): QuestionApi = r.create(QuestionApi::class.java)
    @Provides @Singleton fun practiceApi(r: Retrofit): PracticeApi = r.create(PracticeApi::class.java)
    @Provides @Singleton fun mockExamApi(r: Retrofit): MockExamApi = r.create(MockExamApi::class.java)
    @Provides @Singleton fun plannerApi(r: Retrofit): PlannerApi = r.create(PlannerApi::class.java)
    @Provides @Singleton fun revisionApi(r: Retrofit): RevisionApi = r.create(RevisionApi::class.java)
    @Provides @Singleton fun tutorApi(r: Retrofit): TutorApi = r.create(TutorApi::class.java)
    @Provides @Singleton fun communityApi(r: Retrofit): CommunityApi = r.create(CommunityApi::class.java)
    @Provides @Singleton fun accountApi(r: Retrofit): AccountApi = r.create(AccountApi::class.java)
}
