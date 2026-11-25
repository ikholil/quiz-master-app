package app.prostuti.core.model

import java.time.Instant
import java.time.LocalDate

enum class ExamCategory { BCS, GOVERNMENT_JOB, BANK, NTRCA, PRIMARY_TEACHER, UNIVERSITY_ADMISSION, OTHER }

data class ExamSummary(
    val id: String,
    val name: LocalizedText,
    val shortName: String,
    val category: ExamCategory,
    val authority: LocalizedText,
    val nextDeadline: ExamDeadline?,
    val isBookmarked: Boolean,
    val isTarget: Boolean,
)

data class ExamDeadline(val label: LocalizedText, val date: LocalDate)

data class SubjectMarks(
    val subjectId: String,
    val subjectName: LocalizedText,
    val marks: Double,
    val questionCount: Int?,
)

data class ExamStage(
    val id: String,
    val name: LocalizedText,
    val format: LocalizedText,
    val totalMarks: Double,
    val durationMinutes: Int?,
    val negativeMarkPerWrong: Double,
    val passMarks: Double?,
    val subjects: List<SubjectMarks>,
)

data class ExamNotice(
    val id: String,
    val title: LocalizedText,
    val source: SourceReference,
)

data class ExamDetail(
    val summary: ExamSummary,
    val description: LocalizedText,
    val eligibility: List<LocalizedText>,
    val stages: List<ExamStage>,
    val deadlines: List<ExamDeadline>,
    val notices: List<ExamNotice>,
    val previousPaperCount: Int,
    val officialSource: SourceReference,
    val lastVerifiedAt: Instant,
)

data class ExamFilter(
    val query: String = "",
    val category: ExamCategory? = null,
    val bookmarkedOnly: Boolean = false,
)

/** Row in a side-by-side syllabus comparison; marks are null when a subject is absent from that exam. */
data class SyllabusComparisonRow(
    val subjectName: LocalizedText,
    val marksByExam: Map<String, Double?>,
)

data class SyllabusComparison(
    val exams: List<ExamSummary>,
    val rows: List<SyllabusComparisonRow>,
    val sharedTopicCount: Int,
)

data class Announcement(
    val id: String,
    val title: LocalizedText,
    val body: LocalizedText,
    val source: SourceReference?,
    val publishedAt: Instant,
)
