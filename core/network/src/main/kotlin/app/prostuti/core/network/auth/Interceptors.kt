package app.prostuti.core.network.auth

import app.prostuti.core.datastore.TokenStore
import app.prostuti.core.network.ApiConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Adds the bearer token. Requests marked with [NO_AUTH_HEADER] (login, OTP) are left untouched. */
@Singleton
class AuthInterceptor @Inject constructor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(NO_AUTH_HEADER) != null) {
            return chain.proceed(original.newBuilder().removeHeader(NO_AUTH_HEADER).build())
        }
        val token = runBlocking { tokenStore.read()?.accessToken }
        val request = if (token != null) original.withBearer(token) else original
        return chain.proceed(request)
    }

    companion object {
        const val NO_AUTH_HEADER = "X-No-Auth"
    }
}

/** OkHttp calls this on 401: refresh once, then retry with the new token. */
@Singleton
class TokenAuthenticator @Inject constructor(private val refresher: TokenRefresher) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 2) return null // refreshed token was also rejected
        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        return when (val outcome = runBlocking { refresher.refresh(failedToken) }) {
            is TokenRefresher.Outcome.Refreshed -> response.request.withBearer(outcome.accessToken)
            else -> null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}

internal fun Request.withBearer(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()

/** Identifies the client and asks for content in the user's language. */
class ClientHeadersInterceptor(
    private val config: ApiConfig,
    private val languageTag: () -> String = { Locale.getDefault().language },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().newBuilder()
            .header("Accept-Language", languageTag())
            .header("X-Client", "android")
            .header("X-App-Version", config.appVersion)
            .build(),
    )
}

/**
 * Retries idempotent requests (GET/PUT/DELETE) on network failures and 502/503/504 with
 * exponential backoff — useful on unstable mobile connections. POSTs are never retried here;
 * operations like mock submission handle retries explicitly with idempotency on the server.
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
    private val baseDelayMs: Long = 400,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val idempotent = request.method in setOf("GET", "HEAD", "PUT", "DELETE")
        var attempt = 0
        var lastError: IOException? = null
        while (true) {
            try {
                val response = chain.proceed(request)
                if (!idempotent || response.code !in RETRYABLE_CODES || attempt >= maxRetries) return response
                response.close()
            } catch (e: IOException) {
                if (!idempotent || attempt >= maxRetries || chain.call().isCanceled()) throw e
                lastError = e
            }
            sleeper(baseDelayMs * (1L shl attempt))
            attempt++
            if (attempt > maxRetries) throw lastError ?: IOException("Retries exhausted")
        }
    }

    companion object {
        private val RETRYABLE_CODES = setOf(502, 503, 504)
    }
}
