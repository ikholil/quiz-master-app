package app.prostuti.core.network.dto

import kotlinx.serialization.Serializable

@Serializable data class DeadlineDto(val label: LocalizedDto, val date: String)

@Serializable
data class ExamSummaryDto(
    val id: String,
    val name: LocalizedDto,
    val shortName: String,
    val category: String,
    val authority: LocalizedDto,
    val nextDeadline: DeadlineDto? = null,
    val isBookmarked: Boolean = false,
    val isTarget: Boolean = false,
)

@Serializable data class SubjectMarksDto(val subjectId: String, val subjectName: LocalizedDto, val marks: Double, val questionCount: Int? = null)

@Serializable
data class ExamStageDto(
    val id: String,
    val name: LocalizedDto,
    val format: LocalizedDto,
    val totalMarks: Double,
    val durationMinutes: Int? = null,
    val negativeMarkPerWrong: Double = 0.0,
    val passMarks: Double? = null,
    val subjects: List<SubjectMarksDto> = emptyList(),
)

@Serializable data class NoticeDto(val id: String, val title: LocalizedDto, val source: SourceDto)

@Serializable
data class ExamDetailDto(
    val summary: ExamSummaryDto,
    val description: LocalizedDto,
    val eligibility: List<LocalizedDto>,
    val stages: List<ExamStageDto>,
    val deadlines: List<DeadlineDto>,
    val notices: List<NoticeDto>,
    val previousPaperCount: Int,
    val officialSource: SourceDto,
    val lastVerifiedAt: String,
)

@Serializable data class ComparisonRowDto(val subjectName: LocalizedDto, val marksByExam: Map<String, Double?>)
@Serializable data class ComparisonDto(val exams: List<ExamSummaryDto>, val rows: List<ComparisonRowDto>, val sharedTopicCount: Int)
@Serializable data class TargetsDto(val targets: List<TargetExamDto>)
@Serializable data class AnnouncementDto(val id: String, val title: LocalizedDto, val body: LocalizedDto, val source: SourceDto? = null, val publishedAt: String)
