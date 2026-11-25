package app.prostuti.core.model

import java.time.Instant

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Instant,
)

data class OtpChallenge(
    val challengeId: String,
    val phone: String,
    val resendAvailableAt: Instant,
    val codeLength: Int = 6,
)

data class AuthSession(val userId: String, val isNewUser: Boolean)
