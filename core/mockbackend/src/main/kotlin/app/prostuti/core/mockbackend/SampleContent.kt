package app.prostuti.core.mockbackend

import app.prostuti.core.model.*
import java.time.Instant
import java.time.LocalDate

/*
 * Sample content for development builds. Exam structures follow publicly known patterns, but this is
 * NOT authoritative data: production content always comes from the backend's editorial pipeline.
 * Exam references deliberately omit years so no question is falsely attributed to a specific paper.
 */
internal fun t(en: String, bn: String? = null) = LocalizedText(en, bn?.bnDigits())

/** Sample Bengali text uses Bengali numerals, as the real backend's localized content does. */
internal fun String.bnDigits(): String = map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")

internal object SampleContent {
    val now: Instant = Instant.now()
    val today: LocalDate = LocalDate.now()

    private fun src(title: String, publisher: String, url: String?, daysAgo: Long) =
        SourceReference(title, publisher, url, now.minusSeconds(daysAgo * 86_400))

    val bpsc = src("BCS examination rules and syllabus", "Bangladesh Public Service Commission", "https://bpsc.gov.bd", 40)
    val ntrca = src("Teacher registration examination notice", "NTRCA", "http://ntrca.gov.bd", 25)
    val dpe = src("Assistant teacher recruitment circular", "Directorate of Primary Education", "https://dpe.gov.bd", 18)

    // ---------------- Exams ----------------
    data class SampleExam(val summary: ExamSummary, val detail: ExamDetail)

    private fun stage(id: String, name: LocalizedText, format: LocalizedText, marks: Double, minutes: Int?, negative: Double, pass: Double?, subjects: List<SubjectMarks>) =
        ExamStage(id, name, format, marks, minutes, negative, pass, subjects)

    private fun sm(id: String, en: String, bn: String, marks: Double) = SubjectMarks(id, t(en, bn), marks, marks.toInt())

    val bcsPrelimSubjects = listOf(
        sm("bangla", "Bangla language & literature", "বাংলা ভাষা ও সাহিত্য", 35.0),
        sm("english", "English language & literature", "ইংরেজি ভাষা ও সাহিত্য", 35.0),
        sm("bd-affairs", "Bangladesh affairs", "বাংলাদেশ বিষয়াবলি", 30.0),
        sm("intl-affairs", "International affairs", "আন্তর্জাতিক বিষয়াবলি", 20.0),
        sm("geography", "Geography, environment & disaster management", "ভূগোল, পরিবেশ ও দুর্যোগ ব্যবস্থাপনা", 10.0),
        sm("science", "General science", "সাধারণ বিজ্ঞান", 15.0),
        sm("ict", "Computer & information technology", "কম্পিউটার ও তথ্যপ্রযুক্তি", 15.0),
        sm("math", "Mathematical reasoning", "গাণিতিক যুক্তি", 15.0),
        sm("mental", "Mental ability", "মানসিক দক্ষতা", 15.0),
        sm("ethics", "Ethics, values & good governance", "নৈতিকতা, মূল্যবোধ ও সুশাসন", 10.0),
    )

    val exams: List<SampleExam> = listOf(
        exam(
            id = "bcs", short = "BCS", category = ExamCategory.BCS,
            name = t("Bangladesh Civil Service (BCS) Examination", "বাংলাদেশ সিভিল সার্ভিস (বিসিএস) পরীক্ষা"),
            authority = t("Bangladesh Public Service Commission", "বাংলাদেশ সরকারি কর্ম কমিশন"),
            description = t(
                "Recruitment to cadre posts of the civil service through preliminary, written and viva voce stages.",
                "প্রিলিমিনারি, লিখিত ও মৌখিক পরীক্ষার মাধ্যমে সিভিল সার্ভিসের ক্যাডার পদে নিয়োগ।",
            ),
            eligibility = listOf(
                t("Bachelor's degree (4 years) or equivalent", "স্নাতক (৪ বছর) বা সমমান ডিগ্রি"),
                t("Age 21–32 years (relaxations apply for specific quotas)", "বয়স ২১–৩২ বছর (নির্দিষ্ট ক্ষেত্রে শিথিলযোগ্য)"),
                t("Bangladeshi citizen", "বাংলাদেশের নাগরিক"),
            ),
            stages = listOf(
                stage("bcs-pre", t("Preliminary", "প্রিলিমিনারি"), t("200 MCQ", "২০০ এমসিকিউ"), 200.0, 120, 0.5, null, bcsPrelimSubjects),
                stage("bcs-written", t("Written", "লিখিত"), t("Descriptive papers", "বর্ণনামূলক"), 900.0, null, 0.0, null, emptyList()),
                stage("bcs-viva", t("Viva voce", "মৌখিক"), t("Interview", "সাক্ষাৎকার"), 200.0, null, 0.0, null, emptyList()),
            ),
            deadlines = listOf(ExamDeadline(t("Application closes", "আবেদনের শেষ তারিখ"), today.plusDays(21)), ExamDeadline(t("Preliminary exam", "প্রিলিমিনারি পরীক্ষা"), today.plusDays(96))),
            source = bpsc, papers = 24, target = true,
        ),
        exam(
            id = "ntrca", short = "NTRCA", category = ExamCategory.NTRCA,
            name = t("NTRCA Teacher Registration Examination", "এনটিআরসিএ শিক্ষক নিবন্ধন পরীক্ষা"),
            authority = t("Non-Government Teacher Registration & Certification Authority", "বেসরকারি শিক্ষক নিবন্ধন ও প্রত্যয়ন কর্তৃপক্ষ"),
            description = t("Registration examination for teachers of non-government schools, colleges and madrasas.", "বেসরকারি স্কুল, কলেজ ও মাদ্রাসার শিক্ষক নিবন্ধন পরীক্ষা।"),
            eligibility = listOf(t("Relevant graduate degree", "সংশ্লিষ্ট বিষয়ে স্নাতক ডিগ্রি")),
            stages = listOf(
                stage("ntrca-pre", t("Preliminary", "প্রিলিমিনারি"), t("100 MCQ", "১০০ এমসিকিউ"), 100.0, 60, 0.25, 40.0, listOf(
                    sm("bangla", "Bangla", "বাংলা", 25.0), sm("english", "English", "ইংরেজি", 25.0),
                    sm("math", "Mathematics", "গণিত", 25.0), sm("bd-affairs", "General knowledge", "সাধারণ জ্ঞান", 25.0),
                )),
                stage("ntrca-written", t("Written", "লিখিত"), t("Subject-based", "বিষয়ভিত্তিক"), 100.0, 180, 0.0, 40.0, emptyList()),
            ),
            deadlines = listOf(ExamDeadline(t("Preliminary exam", "প্রিলিমিনারি পরীক্ষা"), today.plusDays(54))),
            source = ntrca, papers = 12, target = false,
        ),
        exam(
            id = "primary", short = "Primary", category = ExamCategory.PRIMARY_TEACHER,
            name = t("Government Primary Assistant Teacher Recruitment", "সরকারি প্রাথমিক সহকারী শিক্ষক নিয়োগ"),
            authority = t("Directorate of Primary Education", "প্রাথমিক শিক্ষা অধিদপ্তর"),
            description = t("Recruitment of assistant teachers for government primary schools.", "সরকারি প্রাথমিক বিদ্যালয়ে সহকারী শিক্ষক নিয়োগ।"),
            eligibility = listOf(t("Graduate degree", "স্নাতক ডিগ্রি")),
            stages = listOf(
                stage("primary-written", t("Written (MCQ)", "লিখিত (এমসিকিউ)"), t("80 MCQ", "৮০ এমসিকিউ"), 80.0, 70, 0.0, null, listOf(
                    sm("bangla", "Bangla", "বাংলা", 20.0), sm("english", "English", "ইংরেজি", 20.0),
                    sm("math", "Mathematics", "গণিত", 20.0), sm("bd-affairs", "General knowledge", "সাধারণ জ্ঞান", 20.0),
                )),
                stage("primary-viva", t("Viva", "মৌখিক"), t("Interview", "সাক্ষাৎকার"), 20.0, null, 0.0, null, emptyList()),
            ),
            deadlines = listOf(ExamDeadline(t("Application closes", "আবেদনের শেষ তারিখ"), today.plusDays(9))),
            source = dpe, papers = 8, target = false,
        ),
        exam(
            id = "bank-so", short = "Bank SO", category = ExamCategory.BANK,
            name = t("Combined Bank Senior Officer Recruitment", "সমন্বিত ব্যাংক সিনিয়র অফিসার নিয়োগ"),
            authority = t("Bankers' Selection Committee", "ব্যাংকার্স সিলেকশন কমিটি"),
            description = t("Combined recruitment for state-owned banks.", "রাষ্ট্রায়ত্ত ব্যাংকসমূহের সমন্বিত নিয়োগ।"),
            eligibility = listOf(t("Graduate degree with no third division", "কোনো পর্যায়ে তৃতীয় বিভাগ নয়")),
            stages = listOf(
                stage("bank-pre", t("Preliminary", "প্রিলিমিনারি"), t("100 MCQ", "১০০ এমসিকিউ"), 100.0, 60, 0.25, null, listOf(
                    sm("bangla", "Bangla", "বাংলা", 20.0), sm("english", "English", "ইংরেজি", 30.0),
                    sm("math", "Mathematics", "গণিত", 30.0), sm("bd-affairs", "General knowledge", "সাধারণ জ্ঞান", 10.0),
                    sm("ict", "Computer", "কম্পিউটার", 10.0),
                )),
            ),
            deadlines = emptyList(),
            source = src("Recruitment notice", "Bangladesh Bank", "https://erecruitment.bb.org.bd", 12), papers = 15, target = false,
        ),
        exam(
            id = "du-admission", short = "DU", category = ExamCategory.UNIVERSITY_ADMISSION,
            name = t("University of Dhaka Admission Test", "ঢাকা বিশ্ববিদ্যালয় ভর্তি পরীক্ষা"),
            authority = t("University of Dhaka", "ঢাকা বিশ্ববিদ্যালয়"),
            description = t("Undergraduate admission test, unit-based.", "ইউনিটভিত্তিক স্নাতক ভর্তি পরীক্ষা।"),
            eligibility = listOf(t("HSC or equivalent with required GPA", "প্রয়োজনীয় জিপিএসহ এইচএসসি বা সমমান")),
            stages = listOf(stage("du-mcq", t("MCQ + written", "এমসিকিউ + লিখিত"), t("Unit-specific", "ইউনিটভিত্তিক"), 100.0, 90, 0.25, 40.0, emptyList())),
            deadlines = emptyList(),
            source = src("Admission circular", "University of Dhaka", "https://admission.eis.du.ac.bd", 60), papers = 10, target = false,
        ),
    )

    private fun exam(
        id: String, short: String, category: ExamCategory, name: LocalizedText, authority: LocalizedText,
        description: LocalizedText, eligibility: List<LocalizedText>, stages: List<ExamStage>,
        deadlines: List<ExamDeadline>, source: SourceReference, papers: Int, target: Boolean,
    ): SampleExam {
        val summary = ExamSummary(id, name, short, category, authority, deadlines.minByOrNull { it.date }, isBookmarked = target, isTarget = target)
        return SampleExam(
            summary,
            ExamDetail(
                summary, description, eligibility, stages, deadlines,
                notices = listOf(ExamNotice("$id-n1", t("Latest circular published", "সর্বশেষ বিজ্ঞপ্তি প্রকাশিত"), source)),
                previousPaperCount = papers, officialSource = source, lastVerifiedAt = now.minusSeconds(3 * 86_400),
            ),
        )
    }

    // ---------------- Syllabus ----------------
    data class SampleTopic(val id: String, val subjectId: String, val chapterId: String, val name: LocalizedText, val minutes: Int, val exams: List<String>)
    data class SampleChapter(val id: String, val subjectId: String, val name: LocalizedText)

    val subjects = bcsPrelimSubjects.associate { it.subjectId to it.subjectName }

    val chapters = listOf(
        SampleChapter("ch-liberation", "bd-affairs", t("History of Bangladesh", "বাংলাদেশের ইতিহাস")),
        SampleChapter("ch-constitution", "bd-affairs", t("Constitution", "সংবিধান")),
        SampleChapter("ch-lit-ancient", "bangla", t("Ancient & medieval literature", "প্রাচীন ও মধ্যযুগের সাহিত্য")),
        SampleChapter("ch-lit-modern", "bangla", t("Modern literature", "আধুনিক যুগের সাহিত্য")),
        SampleChapter("ch-vocab", "english", t("Vocabulary", "শব্দভাণ্ডার")),
        SampleChapter("ch-reading", "english", t("Reading comprehension", "পাঠ অনুধাবন")),
        SampleChapter("ch-arith", "math", t("Arithmetic", "পাটিগণিত")),
        SampleChapter("ch-algebra", "math", t("Algebra", "বীজগণিত")),
        SampleChapter("ch-life", "science", t("Life science", "জীববিজ্ঞান")),
        SampleChapter("ch-physical", "science", t("Physical science", "ভৌত বিজ্ঞান")),
        SampleChapter("ch-ict-basics", "ict", t("Computer fundamentals", "কম্পিউটারের মৌলিক বিষয়")),
        SampleChapter("ch-orgs", "intl-affairs", t("International organisations", "আন্তর্জাতিক সংস্থা")),
        SampleChapter("ch-bd-geo", "geography", t("Geography of Bangladesh", "বাংলাদেশের ভূগোল")),
        SampleChapter("ch-series", "mental", t("Number series", "সংখ্যা ধারা")),
        SampleChapter("ch-governance", "ethics", t("Good governance", "সুশাসন")),
    )

    val topics = listOf(
        SampleTopic("t-1971", "bd-affairs", "ch-liberation", t("Liberation War of 1971", "১৯৭১ সালের মুক্তিযুদ্ধ"), 25, listOf("bcs", "ntrca", "primary", "bank-so")),
        SampleTopic("t-language", "bd-affairs", "ch-liberation", t("Language Movement 1952", "ভাষা আন্দোলন ১৯৫২"), 15, listOf("bcs", "ntrca", "primary")),
        SampleTopic("t-constitution", "bd-affairs", "ch-constitution", t("Constitution of Bangladesh", "বাংলাদেশের সংবিধান"), 30, listOf("bcs", "bank-so")),
        SampleTopic("t-charyapada", "bangla", "ch-lit-ancient", t("Charyapada", "চর্যাপদ"), 15, listOf("bcs", "ntrca")),
        SampleTopic("t-nazrul", "bangla", "ch-lit-modern", t("Kazi Nazrul Islam", "কাজী নজরুল ইসলাম"), 20, listOf("bcs", "ntrca", "primary")),
        SampleTopic("t-tagore", "bangla", "ch-lit-modern", t("Rabindranath Tagore", "রবীন্দ্রনাথ ঠাকুর"), 20, listOf("bcs", "ntrca", "primary")),
        SampleTopic("t-synonyms", "english", "ch-vocab", t("Synonyms & antonyms", "সমার্থক ও বিপরীত শব্দ"), 20, listOf("bcs", "bank-so", "primary")),
        SampleTopic("t-idioms", "english", "ch-vocab", t("Idioms & phrases", "বাগধারা"), 20, listOf("bcs", "bank-so")),
        SampleTopic("t-spelling", "english", "ch-vocab", t("Correct spelling", "শুদ্ধ বানান"), 10, listOf("bcs", "bank-so", "primary")),
        SampleTopic("t-reading", "english", "ch-reading", t("Passage comprehension", "অনুচ্ছেদ অনুধাবন"), 25, listOf("bcs", "bank-so")),
        SampleTopic("t-interest", "math", "ch-arith", t("Simple & compound interest", "সরল ও চক্রবৃদ্ধি মুনাফা"), 30, listOf("bcs", "bank-so", "primary")),
        SampleTopic("t-average", "math", "ch-arith", t("Average", "গড়"), 20, listOf("bcs", "bank-so", "primary")),
        SampleTopic("t-primes", "math", "ch-arith", t("Prime numbers", "মৌলিক সংখ্যা"), 15, listOf("bcs", "primary")),
        SampleTopic("t-identities", "math", "ch-algebra", t("Algebraic identities", "বীজগাণিতিক সূত্র"), 25, listOf("bcs", "bank-so", "du-admission")),
        SampleTopic("t-vitamins", "science", "ch-life", t("Vitamins & nutrition", "ভিটামিন ও পুষ্টি"), 15, listOf("bcs", "primary")),
        SampleTopic("t-sound", "science", "ch-physical", t("Sound", "শব্দ"), 15, listOf("bcs", "du-admission")),
        SampleTopic("t-chem-basics", "science", "ch-physical", t("Everyday chemistry", "দৈনন্দিন রসায়ন"), 15, listOf("bcs", "primary")),
        SampleTopic("t-memory", "ict", "ch-ict-basics", t("Memory & storage units", "মেমোরি ও স্টোরেজ একক"), 15, listOf("bcs", "bank-so")),
        SampleTopic("t-io", "ict", "ch-ict-basics", t("Input & output devices", "ইনপুট ও আউটপুট ডিভাইস"), 10, listOf("bcs", "bank-so")),
        SampleTopic("t-internet", "ict", "ch-ict-basics", t("Internet basics", "ইন্টারনেটের মৌলিক ধারণা"), 15, listOf("bcs", "bank-so")),
        SampleTopic("t-un", "intl-affairs", "ch-orgs", t("United Nations", "জাতিসংঘ"), 20, listOf("bcs", "bank-so")),
        SampleTopic("t-saarc", "intl-affairs", "ch-orgs", t("SAARC", "সার্ক"), 10, listOf("bcs")),
        SampleTopic("t-forests", "geography", "ch-bd-geo", t("Forests & islands", "বনভূমি ও দ্বীপ"), 15, listOf("bcs", "ntrca", "primary")),
        SampleTopic("t-series", "mental", "ch-series", t("Number series", "সংখ্যা ধারা"), 15, listOf("bcs", "bank-so")),
        SampleTopic("t-integrity", "ethics", "ch-governance", t("National Integrity Strategy", "জাতীয় শুদ্ধাচার কৌশল"), 15, listOf("bcs")),
    )
}
