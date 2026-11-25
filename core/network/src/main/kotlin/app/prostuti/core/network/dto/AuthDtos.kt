package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable data class OtpRequestDto(val phone: String)
@Serializable data class OtpChallengeDto(val challengeId: String, val phone: String, val resendAvailableAt: String, val codeLength: Int = 6)
@Serializable data class OtpVerifyDto(val challengeId: String, val code: String)
@Serializable data class EmailLoginDto(val email: String, val password: String)
@Serializable data class EmailRegisterDto(val name: String, val email: String, val password: String)
@Serializable data class ForgotPasswordDto(val email: String)
@Serializable data class RefreshRequestDto(val refreshToken: String)
@Serializable data class TokensDto(val accessToken: String, val refreshToken: String, val accessExpiresAt: String)
@Serializable data class AuthUserDto(val id: String, val onboardingCompleted: Boolean)
@Serializable data class AuthResponseDto(val tokens: TokensDto, val user: AuthUserDto, val isNewUser: Boolean)
