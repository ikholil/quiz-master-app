package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable data class SubscriptionDto(val planName: LocalizedDto? = null, val entitlements: List<String>, val expiresAt: String? = null, val isActive: Boolean)
@Serializable data class PlanDto(val id: String, val name: LocalizedDto, val priceBdt: Int, val periodMonths: Int, val entitlements: List<String>, val isPopular: Boolean = false)
@Serializable data class CheckoutRequestDto(val planId: String)
@Serializable data class CheckoutDto(val checkoutUrl: String)
@Serializable data class ExportDto(val id: String, val requestedAt: String, val readyAt: String? = null, val downloadUrl: String? = null)
@Serializable data class DeletionRequestDto(val reason: String)
@Serializable data class AchievementDto(val id: String, val title: LocalizedDto, val description: LocalizedDto, val unlockedAt: String? = null, val progress: Float)
@Serializable data class TicketDto(val id: String, val subject: String, val status: String, val createdAt: String, val lastReply: String? = null)
@Serializable data class CreateTicketDto(val subject: String, val message: String)
@Serializable data class NotificationDto(val id: String, val title: LocalizedDto, val body: LocalizedDto, val createdAt: String, val isRead: Boolean)
@Serializable data class DeviceDto(val pushToken: String, val platform: String = "android")

@Serializable data class GroupDto(val id: String, val name: String, val examShortName: String? = null, val memberCount: Int, val isMember: Boolean, val isPrivate: Boolean, val description: String)
@Serializable data class DiscussionDto(val id: String, val title: String, val body: String, val authorName: String, val questionId: String? = null, val examShortName: String? = null, val replyCount: Int, val upvotes: Int, val createdAt: String, val isResolved: Boolean)
@Serializable data class ReplyDto(val id: String, val authorName: String, val body: String, val upvotes: Int, val isAccepted: Boolean, val isFromEditor: Boolean, val createdAt: String)
@Serializable data class PostDiscussionDto(val title: String, val body: String, val questionId: String? = null)
@Serializable data class PostReplyDto(val body: String)
@Serializable data class ContentReportDto(val reason: String)
@Serializable data class LeaderboardEntryDto(val rank: Int, val displayName: String, val score: Int, val isMe: Boolean)
@Serializable data class LeaderboardDto(val groupId: String, val period: String, val entries: List<LeaderboardEntryDto>)
