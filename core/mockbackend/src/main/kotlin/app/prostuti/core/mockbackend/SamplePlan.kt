package app.prostuti.core.mockbackend

import app.prostuti.core.model.*
import java.time.Instant
import java.time.LocalDate

internal object SamplePlan {
    fun days(store: MockStore, dailyMinutes: Int, weakSubjects: List<String> = emptyList()): List<StudyDay> {
        val today = LocalDate.now()
        val weakTopics = SampleContent.topics.sortedBy { store.mastery(it.id).value }
            .sortedByDescending { it.subjectId in weakSubjects }
        return (-2..11).map { offset ->
            val date = today.plusDays(offset.toLong())
            val topic = weakTopics[(offset + 2).mod(weakTopics.size)]
            val next = weakTopics[(offset + 5).mod(weakTopics.size)]
            val budget = dailyMinutes.coerceAtLeast(30)
            val tasks = buildList {
                add(task("$date-learn", date, PlanTaskType.LEARN, t("Study: ${topic.name.en}", "পড়ুন: ${topic.name.bn}"), topic.id, budget * 4 / 10,
                    t("Mastery is ${store.mastery(topic.id).percent}% — one of your lowest.", "দক্ষতা ${store.mastery(topic.id).percent}% — আপনার সবচেয়ে কমগুলোর একটি।"), store, offset))
                add(task("$date-practice", date, PlanTaskType.PRACTICE, t("Practise 15 questions: ${next.name.en}", "১৫টি প্রশ্ন অনুশীলন: ${next.name.bn}"), next.id, budget * 3 / 10,
                    t("Shared by ${next.exams.size} of your target exams.", "আপনার ${next.exams.size}টি লক্ষ্য পরীক্ষায় এই টপিক আছে।"), store, offset))
                add(task("$date-revise", date, PlanTaskType.FLASHCARDS, t("Review due flashcards", "ডিউ ফ্ল্যাশকার্ড রিভিউ"), null, budget * 2 / 10,
                    t("Spaced repetition: these cards are due today.", "স্পেসড রিপিটিশন: আজ এই কার্ডগুলোর সময়।"), store, offset))
                if (date.dayOfWeek.value == 5) add(task("$date-mock", date, PlanTaskType.MOCK, t("Weekly model test", "সাপ্তাহিক মডেল টেস্ট"), null, 30,
                    t("A weekly full test keeps your timing sharp.", "সাপ্তাহিক পূর্ণ টেস্ট সময় ব্যবস্থাপনা ঠিক রাখে।"), store, offset, mockId = "mock-bcs-1"))
            }
            StudyDay(date, tasks)
        }
    }

    private fun task(
        id: String, date: LocalDate, type: PlanTaskType, title: LocalizedText, topicId: String?, minutes: Int,
        reason: LocalizedText, store: MockStore, offset: Int, mockId: String? = null,
    ): PlanTask {
        val default = when {
            offset < -1 -> PlanTaskStatus.DONE
            offset == -1 && type == PlanTaskType.PRACTICE -> PlanTaskStatus.MISSED
            offset == -1 -> PlanTaskStatus.DONE
            else -> PlanTaskStatus.PENDING
        }
        return PlanTask(id, date, type, title, topicId, mockId, minutes.coerceAtLeast(10), reason, store.planStatus[id] ?: default)
    }

    fun announcements() = listOf(
        Announcement("a1", t("Preliminary exam date published", "প্রিলিমিনারি পরীক্ষার তারিখ প্রকাশিত"),
            t("The commission has published the preliminary schedule. Check the official notice.", "কমিশন প্রিলিমিনারি পরীক্ষার সময়সূচি প্রকাশ করেছে। অফিসিয়াল বিজ্ঞপ্তি দেখুন।"),
            SampleContent.bpsc, Instant.now().minusSeconds(86_400 * 2)),
        Announcement("a2", t("New: Bangla literature question set", "নতুন: বাংলা সাহিত্য প্রশ্ন সেট"),
            t("120 verified questions on modern Bangla literature.", "আধুনিক বাংলা সাহিত্যের ১২০টি যাচাইকৃত প্রশ্ন।"), null, Instant.now().minusSeconds(86_400 * 4)),
    )

    fun sets(store: MockStore) = listOf(
        PracticeSetSummary("set-weak", t("Your weak spots", "আপনার দুর্বল জায়গা"), 10, 8,
            t("Built from topics where your accuracy is below 50%.", "যেসব টপিকে আপনার সঠিকতার হার ৫০%-এর কম, সেগুলো থেকে।"), false),
        PracticeSetSummary("set-1971", t("Liberation War essentials", "মুক্তিযুদ্ধ: অবশ্যই জানতে হবে"), 3, 3,
            t("Appears in all 4 of the exams you follow.", "আপনার অনুসরণ করা ৪টি পরীক্ষাতেই আসে।"), store.downloads.value.any { it.id == "t-1971" }),
        PracticeSetSummary("set-math", t("Math speed drill", "গণিত স্পিড ড্রিল"), 4, 4,
            t("Your average is 74s per math question; the exam allows about 36s.", "আপনার গড় সময় প্রতি প্রশ্নে ৭৪ সেকেন্ড; পরীক্ষায় পাওয়া যায় প্রায় ৩৬ সেকেন্ড।"), false),
    )

    fun pastAttempts() = listOf(
        MockAttemptHistoryItem("past-2", t("BCS preliminary model test (diagnostic)", "বিসিএস প্রিলিমিনারি মডেল টেস্ট (ডায়াগনস্টিক)"), Instant.now().minusSeconds(86_400 * 6), 104.5, 200.0, 17.5, 61f),
        MockAttemptHistoryItem("past-1", t("Bangladesh affairs sectional", "বাংলাদেশ বিষয়াবলি সেকশনাল"), Instant.now().minusSeconds(86_400 * 13), 18.0, 30.0, 3.0, 55f),
    )
}
