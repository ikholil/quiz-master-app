package app.prostuti.core.model

import java.time.Instant

data class StudyGroup(
    val id: String,
    val name: String,
    val examShortName: String?,
    val memberCount: Int,
    val isMember: Boolean,
    val isPrivate: Boolean,
    val description: String,
)

data class Discussion(
    val id: String,
    val title: String,
    val body: String,
    val authorName: String,
    val questionId: String?,
    val examShortName: String?,
    val replyCount: Int,
    val upvotes: Int,
    val createdAt: Instant,
    val isResolved: Boolean,
)

data class DiscussionReply(
    val id: String,
    val authorName: String,
    val body: String,
    val upvotes: Int,
    val isAccepted: Boolean,
    val isFromEditor: Boolean,
    val createdAt: Instant,
)

data class LeaderboardEntry(val rank: Int, val displayName: String, val score: Int, val isMe: Boolean)

data class Leaderboard(val groupId: String, val period: String, val entries: List<LeaderboardEntry>)
