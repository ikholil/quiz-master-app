package app.prostuti.core.model

import java.time.Instant
import java.time.LocalDate

enum class Entitlement { FULL_MOCKS, UNLIMITED_AI, OFFLINE_DOWNLOADS, VIDEO_LESSONS, ADVANCED_ANALYTICS }

data class SubscriptionPlan(
    val id: String,
    val name: LocalizedText,
    val priceBdt: Int,
    val periodMonths: Int,
    val entitlements: Set<Entitlement>,
    val isPopular: Boolean,
)

data class SubscriptionStatus(
    val planName: LocalizedText?,
    val entitlements: Set<Entitlement>,
    val expiresAt: LocalDate?,
    val isActive: Boolean,
) {
    fun has(entitlement: Entitlement) = isActive && entitlement in entitlements
    companion object {
        val Free = SubscriptionStatus(null, emptySet(), null, false)
    }
}

data class Achievement(
    val id: String,
    val title: LocalizedText,
    val description: LocalizedText,
    val unlockedAt: Instant?,
    val progress: Float,
)

enum class TicketStatus { OPEN, IN_PROGRESS, RESOLVED }

data class SupportTicket(
    val id: String,
    val subject: String,
    val status: TicketStatus,
    val createdAt: Instant,
    val lastReply: String?,
)

data class DataExportRequest(val id: String, val requestedAt: Instant, val readyAt: Instant?, val downloadUrl: String?)

data class AppNotification(
    val id: String,
    val title: LocalizedText,
    val body: LocalizedText,
    val createdAt: Instant,
    val isRead: Boolean,
)
