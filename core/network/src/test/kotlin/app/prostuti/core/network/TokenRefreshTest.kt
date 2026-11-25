package app.prostuti.core.network

import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.datastore.TokenStore
import app.prostuti.core.network.api.ProfileApi
import app.prostuti.core.network.api.RefreshApi
import app.prostuti.core.network.auth.AuthInterceptor
import app.prostuti.core.network.auth.RetryInterceptor
import app.prostuti.core.network.auth.SessionEvents
import app.prostuti.core.network.auth.TokenAuthenticator
import app.prostuti.core.network.auth.TokenRefresher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TokenRefreshTest {
    private lateinit var server: MockWebServer
    private val store = InMemoryTokenStore()
    private val events = SessionEvents()

    private class InMemoryTokenStore : TokenStore {
        var tokens: TokenStore.Tokens? = null
        override suspend fun read() = tokens
        override suspend fun write(tokens: TokenStore.Tokens) { this.tokens = tokens }
        override suspend fun clear() { tokens = null }
    }

    private fun retrofit(client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(client)
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .build()

    private fun profileApi(): ProfileApi {
        val refreshApi = retrofit(OkHttpClient()).create(RefreshApi::class.java)
        val refresher = TokenRefresher(store, refreshApi, events)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store))
            .addInterceptor(RetryInterceptor(sleeper = {}))
            .authenticator(TokenAuthenticator(refresher))
            .build()
        return retrofit(client).create(ProfileApi::class.java)
    }

    private val profileJson = """{"data":{"id":"u1","fullName":"Rahim","language":"bn","targets":[],"dailyStudyMinutes":60,"level":"BEGINNER","strongSubjectIds":[],"weakSubjectIds":[],"onboardingCompleted":true}}"""

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        store.tokens = TokenStore.Tokens("old-access", "refresh-1", Instant.parse("2026-01-01T00:00:00Z"))
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `expired access token is refreshed and the request retried`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"data":{"accessToken":"new-access","refreshToken":"refresh-2","accessExpiresAt":"2027-01-01T00:00:00Z"}}"""))
        server.enqueue(MockResponse().setBody(profileJson))

        val result = safeApiCall { profileApi().profile() }

        assertIs<AppResult.Success<*>>(result)
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("/v1/auth/refresh", server.takeRequest().path)
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("refresh-2", store.tokens?.refreshToken)
    }

    @Test fun `rejected refresh token clears the session`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val result = safeApiCall { profileApi().profile() }

        assertEquals(AppError.SessionExpired, (result as AppResult.Failure).error)
        assertNull(store.tokens)
    }

    @Test fun `idempotent GET is retried after a dropped connection`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody(profileJson))

        val result = safeApiCall { profileApi().profile() }

        assertIs<AppResult.Success<*>>(result)
        assertEquals(3, server.requestCount)
    }

    @Test fun `validation errors carry the server message and field errors`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"message":"Phone number is invalid","fieldErrors":{"phone":"invalid"}}"""))
        val error = (safeApiCall { profileApi().profile() } as AppResult.Failure).error
        val validation = assertIs<AppError.Validation>(error)
        assertEquals("Phone number is invalid", validation.message)
        assertEquals("invalid", validation.fieldErrors["phone"])
    }
}
