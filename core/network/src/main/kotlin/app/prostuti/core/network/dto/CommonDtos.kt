package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

/** Standard NestJS response envelope used by every endpoint. */
@Serializable
data class Envelope<T>(val data: T, val meta: MetaDto? = null)

@Serializable
data class MetaDto(val nextCursor: String? = null, val serverTime: String? = null)

@Serializable
data class ErrorBodyDto(
    val statusCode: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val fieldErrors: Map<String, String>? = null,
    val entitlement: String? = null,
)

@Serializable
data class LocalizedDto(val en: String, val bn: String? = null)

@Serializable
data class SourceDto(val title: String, val publisher: String, val url: String? = null, val publishedAt: String? = null)

@Serializable
data class RecommendationDto(
    val id: String,
    val title: LocalizedDto,
    val reason: LocalizedDto,
    val actionType: String,
    val targetId: String? = null,
)

@Serializable
data class ServerTimeDto(val serverTime: String)
