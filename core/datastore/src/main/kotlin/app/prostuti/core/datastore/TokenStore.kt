package app.prostuti.core.datastore

import java.time.Instant

/** Storage for auth tokens. Production implementation encrypts with the Android Keystore. */
interface TokenStore {
    data class Tokens(val accessToken: String, val refreshToken: String, val accessExpiresAt: Instant)

    suspend fun read(): Tokens?
    suspend fun write(tokens: Tokens)
    suspend fun clear()
}
