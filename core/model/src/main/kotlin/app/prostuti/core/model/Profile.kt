package app.prostuti.core.model

import java.time.LocalDate

enum class PreparationLevel { BEGINNER, INTERMEDIATE, ADVANCED }

data class TargetExam(
    val examId: String,
    val examName: LocalizedText,
    val examDate: LocalDate?,
    val isPrimary: Boolean,
)

data class StudentProfile(
    val id: String,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val avatarUrl: String?,
    val language: AppLanguage,
    val targets: List<TargetExam>,
    val dailyStudyMinutes: Int,
    val level: PreparationLevel,
    val strongSubjectIds: List<String>,
    val weakSubjectIds: List<String>,
    val onboardingCompleted: Boolean,
) {
    val primaryTarget: TargetExam? get() = targets.firstOrNull { it.isPrimary } ?: targets.firstOrNull()
}

/** Input collected during onboarding (and editable later from the profile). */
data class PreparationPreferences(
    val fullName: String,
    val language: AppLanguage,
    val targets: List<TargetExam>,
    val dailyStudyMinutes: Int,
    val level: PreparationLevel,
    val strongSubjectIds: List<String>,
    val weakSubjectIds: List<String>,
)

data class PrivacySettings(
    val showOnLeaderboards: Boolean,
    val shareProgressWithGroups: Boolean,
    val personalizedRecommendations: Boolean,
    val marketingNotifications: Boolean,
)
