package app.prostuti.core.model

import java.time.LocalDate

enum class PlanTaskType { LEARN, PRACTICE, REVISE, MOCK, FLASHCARDS }
enum class PlanTaskStatus { PENDING, DONE, MISSED, RESCHEDULED }

data class PlanTask(
    val id: String,
    val date: LocalDate,
    val type: PlanTaskType,
    val title: LocalizedText,
    val topicId: String?,
    val mockId: String?,
    val minutes: Int,
    /** Explains why the engine scheduled this task. */
    val reason: LocalizedText,
    val status: PlanTaskStatus,
)

data class StudyDay(val date: LocalDate, val tasks: List<PlanTask>) {
    val plannedMinutes: Int get() = tasks.sumOf { it.minutes }
    val completedMinutes: Int get() = tasks.filter { it.status == PlanTaskStatus.DONE }.sumOf { it.minutes }
}

data class StudyPlan(
    val id: String,
    val examId: String,
    val examName: LocalizedText,
    val examDate: LocalDate?,
    val days: List<StudyDay>,
    val missedTaskCount: Int,
    val adjustments: List<PlanAdjustment>,
)

/** A change the engine made, e.g. after a mock exam, shown to the student with its reason. */
data class PlanAdjustment(val id: String, val summary: LocalizedText, val date: LocalDate)
