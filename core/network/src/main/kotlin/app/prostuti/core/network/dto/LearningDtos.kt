package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubjectDto(
    val id: String,
    val name: LocalizedDto,
    val marks: Double? = null,
    val chapterCount: Int,
    val topicCount: Int,
    val completedTopicCount: Int,
    val mastery: Float,
)

@Serializable
data class TopicSummaryDto(
    val id: String,
    val name: LocalizedDto,
    val estimatedMinutes: Int,
    val questionCount: Int,
    val isCompleted: Boolean,
    val mastery: Float,
    val sharedWithExamIds: List<String> = emptyList(),
)

@Serializable data class ChapterDto(val id: String, val subjectId: String, val name: LocalizedDto, val topics: List<TopicSummaryDto>)

@Serializable
data class MaterialSummaryDto(
    val id: String,
    val topicId: String,
    val title: LocalizedDto,
    val type: String,
    val durationMinutes: Int? = null,
    val sizeBytes: Long? = null,
    val isPremium: Boolean = false,
)

@Serializable
data class TopicDetailDto(
    val summary: TopicSummaryDto,
    val subjectName: LocalizedDto,
    val chapterName: LocalizedDto,
    val prerequisites: List<TopicSummaryDto>,
    val materials: List<MaterialSummaryDto>,
    val note: String? = null,
    val isBookmarked: Boolean = false,
)

/** Flat discriminated block; [type] selects which fields are meaningful. */
@Serializable
data class ContentBlockDto(
    val type: String,
    val text: LocalizedDto? = null,
    val level: Int? = null,
    val items: List<LocalizedDto>? = null,
    val kind: String? = null,
    val url: String? = null,
    val caption: LocalizedDto? = null,
    val altText: LocalizedDto? = null,
    val header: List<LocalizedDto>? = null,
    val rows: List<List<LocalizedDto>>? = null,
    val expression: String? = null,
    val durationSeconds: Int? = null,
    val thumbnailUrl: String? = null,
    val sizeBytes: Long? = null,
)

@Serializable
data class MaterialDto(
    val summary: MaterialSummaryDto,
    val blocks: List<ContentBlockDto>,
    val sources: List<SourceDto>,
    val updatedAt: String,
)

@Serializable data class TopicProgressDto(val completed: Boolean, val updatedAt: String)
@Serializable data class NoteBodyDto(val note: String)

/** Everything needed to study and practise a topic offline. */
@Serializable
data class TopicBundleDto(
    val topic: TopicDetailDto,
    val materials: List<MaterialDto>,
    val questions: List<QuestionDto>,
)
