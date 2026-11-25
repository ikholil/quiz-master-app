package app.prostuti.core.model

import java.time.Instant

/** Mastery is a 0..1 score computed by the backend from accuracy, recency and difficulty. */
@JvmInline
value class Mastery(val value: Float) {
    init { require(value in 0f..1f) { "Mastery must be within 0..1" } }
    val level: MasteryLevel
        get() = when {
            value >= 0.8f -> MasteryLevel.MASTERED
            value >= 0.55f -> MasteryLevel.PROFICIENT
            value >= 0.25f -> MasteryLevel.LEARNING
            value > 0f -> MasteryLevel.WEAK
            else -> MasteryLevel.NOT_STARTED
        }
    val percent: Int get() = (value * 100).toInt()
}

enum class MasteryLevel { NOT_STARTED, WEAK, LEARNING, PROFICIENT, MASTERED }

data class Subject(
    val id: String,
    val name: LocalizedText,
    val marks: Double?,
    val chapterCount: Int,
    val topicCount: Int,
    val completedTopicCount: Int,
    val mastery: Mastery,
)

data class Chapter(
    val id: String,
    val subjectId: String,
    val name: LocalizedText,
    val topics: List<TopicSummary>,
)

/**
 * A topic can belong to several exam syllabuses (e.g. "Liberation War" in BCS and NTRCA).
 * Progress is keyed by [id] only, so the student keeps one learning record across exams.
 */
data class TopicSummary(
    val id: String,
    val name: LocalizedText,
    val estimatedMinutes: Int,
    val questionCount: Int,
    val isCompleted: Boolean,
    val mastery: Mastery,
    val sharedWithExamIds: List<String>,
    val isAvailableOffline: Boolean,
)

data class TopicDetail(
    val summary: TopicSummary,
    val subjectName: LocalizedText,
    val chapterName: LocalizedText,
    val prerequisites: List<TopicSummary>,
    val materials: List<LearningMaterialSummary>,
    val note: String?,
    val isBookmarked: Boolean,
)

enum class MaterialType { ARTICLE, AUDIO, VIDEO, PDF }

data class LearningMaterialSummary(
    val id: String,
    val topicId: String,
    val title: LocalizedText,
    val type: MaterialType,
    val durationMinutes: Int?,
    val sizeBytes: Long?,
    val isPremium: Boolean,
    val isDownloaded: Boolean,
)

data class LearningMaterial(
    val summary: LearningMaterialSummary,
    val blocks: List<ContentBlock>,
    val sources: List<SourceReference>,
    val updatedAt: Instant,
)

/** Rich learning content is delivered as typed blocks so it renders natively and works offline. */
sealed interface ContentBlock {
    data class Heading(val text: LocalizedText, val level: Int = 2) : ContentBlock
    data class Paragraph(val text: LocalizedText) : ContentBlock
    data class Bullets(val items: List<LocalizedText>) : ContentBlock
    data class Callout(val kind: CalloutKind, val text: LocalizedText) : ContentBlock
    data class Image(val url: String, val caption: LocalizedText?, val altText: LocalizedText) : ContentBlock
    data class Table(val header: List<LocalizedText>, val rows: List<List<LocalizedText>>) : ContentBlock
    /** Formula in a linear, readable notation (e.g. "A = P(1 + r/n)^(nt)") plus spoken description for TalkBack. */
    data class Formula(val expression: String, val description: LocalizedText) : ContentBlock
    data class Audio(val url: String, val title: LocalizedText, val durationSeconds: Int) : ContentBlock
    data class Video(val url: String, val title: LocalizedText, val durationSeconds: Int, val thumbnailUrl: String?) : ContentBlock
    data class Pdf(val url: String, val title: LocalizedText, val sizeBytes: Long) : ContentBlock
}

enum class CalloutKind { TIP, REMEMBER, EXAM_TRAP }
