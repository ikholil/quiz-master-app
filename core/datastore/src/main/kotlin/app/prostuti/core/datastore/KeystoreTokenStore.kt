package app.prostuti.core.datastore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.secureStore: DataStore<Preferences> by preferencesDataStore(name = "secure_session")

/**
 * Tokens are AES-256-GCM encrypted with a non-exportable Android Keystore key before they touch
 * disk. The decrypted value is cached in memory so interceptors do not decrypt per request.
 * The DataStore file is excluded from cloud backup (see backup_rules.xml in :app).
 */
@Singleton
class KeystoreTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : TokenStore {

    private val mutex = Mutex()
    @Volatile private var cached: TokenStore.Tokens? = null
    @Volatile private var loaded = false

    override suspend fun read(): TokenStore.Tokens? {
        if (loaded) return cached
        return mutex.withLock {
            if (!loaded) {
                cached = runCatching {
                    context.secureStore.data.first()[KEY]?.let { decode(decrypt(it)) }
                }.getOrNull()
                loaded = true
            }
            cached
        }
    }

    override suspend fun write(tokens: TokenStore.Tokens) = mutex.withLock {
        context.secureStore.edit { it[KEY] = encrypt(encode(tokens)) }
        cached = tokens
        loaded = true
    }

    override suspend fun clear() = mutex.withLock {
        context.secureStore.edit { it.remove(KEY) }
        cached = null
        loaded = true
    }

    private fun encode(t: TokenStore.Tokens) = listOf(t.accessToken, t.refreshToken, t.accessExpiresAt.toString()).joinToString(SEP)

    private fun decode(raw: String): TokenStore.Tokens {
        val (a, r, e) = raw.split(SEP)
        return TokenStore.Tokens(a, r, Instant.parse(e))
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        }
        return String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
    }

    private companion object {
        val KEY = stringPreferencesKey("tokens")
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "prostuti_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val SEP = "\u0001"
    }
}
