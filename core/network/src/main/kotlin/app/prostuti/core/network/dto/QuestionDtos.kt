package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable data class OptionDto(val id: String, val text: LocalizedDto, val imageUrl: String? = null)

@Serializable
data class SolutionDto(
    val correctOptionIds: List<String>,
    val explanation: LocalizedDto,
    val optionExplanations: Map<String, LocalizedDto> = emptyMap(),
    val modelAnswer: LocalizedDto? = null,
)

@Serializable data class ExamRefDto(val examName: LocalizedDto, val year: Int? = null, val stage: String? = null)
@Serializable data class PassageDto(val id: String, val title: LocalizedDto? = null, val text: LocalizedDto)

@Serializable
data class QuestionDto(
    val id: String,
    val type: String,
    val stem: LocalizedDto,
    val options: List<OptionDto> = emptyList(),
    val passage: PassageDto? = null,
    val imageUrl: String? = null,
    val formula: String? = null,
    val difficulty: String,
    val provenance: String,
    val topicId: String,
    val topicName: LocalizedDto,
    val subjectName: LocalizedDto,
    val examReferences: List<ExamRefDto> = emptyList(),
    val sources: List<SourceDto> = emptyList(),
    /** Omitted by the server during active exams. */
    val solution: SolutionDto? = null,
    val isBookmarked: Boolean = false,
    val note: String? = null,
)

@Serializable data class ReportDto(val reason: String, val details: String)
@Serializable data class CollectionDto(val id: String, val name: String, val questionCount: Int, val updatedAt: String)
@Serializable data class CreateCollectionDto(val name: String)
@Serializable data class AddToCollectionDto(val questionId: String)

@Serializable
data class WrongAnswerDto(
    val question: QuestionDto,
    val selectedOptionIds: List<String>,
    val timesWrong: Int,
    val lastWrongAt: String,
    val resolved: Boolean,
)
