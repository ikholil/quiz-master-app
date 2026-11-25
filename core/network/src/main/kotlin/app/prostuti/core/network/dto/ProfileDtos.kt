package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TargetExamDto(val examId: String, val examName: LocalizedDto, val examDate: String? = null, val isPrimary: Boolean)

@Serializable
data class ProfileDto(
    val id: String,
    val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val language: String,
    val targets: List<TargetExamDto>,
    val dailyStudyMinutes: Int,
    val level: String,
    val strongSubjectIds: List<String>,
    val weakSubjectIds: List<String>,
    val onboardingCompleted: Boolean,
)

@Serializable
data class PreferencesDto(
    val fullName: String,
    val language: String,
    val targets: List<TargetExamDto>,
    val dailyStudyMinutes: Int,
    val level: String,
    val strongSubjectIds: List<String>,
    val weakSubjectIds: List<String>,
)

@Serializable
data class PrivacyDto(
    val showOnLeaderboards: Boolean,
    val shareProgressWithGroups: Boolean,
    val personalizedRecommendations: Boolean,
    val marketingNotifications: Boolean,
)
