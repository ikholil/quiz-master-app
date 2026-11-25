package app.prostuti.core.mockbackend

import app.prostuti.core.model.*

internal object SampleQuestions {

    private class Opt(val en: String, val bn: String?, val why: Pair<String, String?>? = null)

    private fun o(en: String, bn: String? = null, whyEn: String? = null, whyBn: String? = null) =
        Opt(en, bn, whyEn?.let { it to whyBn })

    private fun q(
        id: String,
        topicId: String,
        stemEn: String,
        stemBn: String?,
        options: List<Opt>,
        correct: List<Int>,
        explainEn: String,
        explainBn: String?,
        difficulty: Difficulty = Difficulty.MEDIUM,
        provenance: QuestionProvenance = QuestionProvenance.VERIFIED_RECONSTRUCTED,
        type: QuestionType = QuestionType.MCQ,
        formula: String? = null,
        passage: Passage? = null,
        modelAnswer: LocalizedText? = null,
    ): Question {
        val topic = SampleContent.topics.first { it.id == topicId }
        val optionModels = options.mapIndexed { i, opt -> QuestionOption("$id-${'a' + i}", t(opt.en, opt.bn)) }
        val refs = topic.exams.take(2).mapNotNull { examId ->
            SampleContent.exams.firstOrNull { it.summary.id == examId }?.let { ExamReference(t("${it.summary.shortName} pattern", "${it.summary.shortName} প্যাটার্ন"), null, null) }
        }
        return Question(
            id = id,
            type = type,
            stem = t(stemEn, stemBn),
            options = optionModels,
            passage = passage,
            imageUrl = null,
            formula = formula,
            difficulty = difficulty,
            provenance = provenance,
            topicId = topicId,
            topicName = topic.name,
            subjectName = SampleContent.subjects[topic.subjectId] ?: t(topic.subjectId),
            examReferences = refs,
            sources = emptyList(),
            solution = QuestionSolution(
                correctOptionIds = correct.map { optionModels[it].id }.toSet(),
                explanation = t(explainEn, explainBn),
                optionExplanations = options.mapIndexedNotNull { i, opt -> opt.why?.let { optionModels[i].id to t(it.first, it.second) } }.toMap(),
                modelAnswer = modelAnswer,
            ),
        )
    }

    private val readingPassage = Passage(
        "p-river",
        t("Rivers of the delta", "ব-দ্বীপের নদী"),
        t(
            "Bangladesh lies on the largest river delta in the world, formed by the Ganges, the Brahmaputra and the Meghna. " +
                "Every monsoon these rivers carry enormous amounts of silt, which renews the fertility of the floodplains. " +
                "The same floods, however, can destroy homes and crops, so communities have long adapted by building on raised plinths.",
        ),
    )

    val all: List<Question> = listOf(
        // ---- Bangladesh affairs ----
        q("q-independence", "t-1971",
            "Which date is observed as the Independence Day of Bangladesh?", "বাংলাদেশের স্বাধীনতা দিবস কোনটি?",
            listOf(
                o("16 December", "১৬ ডিসেম্বর", "16 December is Victory Day, when Pakistani forces surrendered in 1971.", "১৬ ডিসেম্বর বিজয় দিবস — ১৯৭১ সালে পাকিস্তানি বাহিনী আত্মসমর্পণ করে।"),
                o("21 February", "২১ ফেব্রুয়ারি", "21 February is Shaheed Day and International Mother Language Day.", "২১ ফেব্রুয়ারি শহীদ দিবস ও আন্তর্জাতিক মাতৃভাষা দিবস।"),
                o("26 March", "২৬ মার্চ"),
                o("17 April", "১৭ এপ্রিল", "17 April 1971 is when the Mujibnagar government took oath.", "১৭ এপ্রিল ১৯৭১ মুজিবনগর সরকার শপথ নেয়।"),
            ), listOf(2),
            "Independence was declared on 26 March 1971, which is observed as Independence Day.",
            "১৯৭১ সালের ২৬ মার্চ স্বাধীনতা ঘোষণা করা হয়; এ দিনটি স্বাধীনতা দিবস হিসেবে পালিত হয়।",
            Difficulty.EASY, QuestionProvenance.OFFICIAL),
        q("q-mujibnagar", "t-1971",
            "When did the Mujibnagar Government take oath?", "মুজিবনগর সরকার কবে শপথ গ্রহণ করে?",
            listOf(o("10 April 1971", "১০ এপ্রিল ১৯৭১", "The government was formed on 10 April; the oath was on 17 April.", "১০ এপ্রিল সরকার গঠিত হয়; শপথ হয় ১৭ এপ্রিল।"), o("17 April 1971", "১৭ এপ্রিল ১৯৭১"), o("26 March 1971", "২৬ মার্চ ১৯৭১"), o("16 December 1971", "১৬ ডিসেম্বর ১৯৭১")),
            listOf(1), "The Mujibnagar Government took oath on 17 April 1971 at Baidyanathtala, Meherpur.",
            "১৭ এপ্রিল ১৯৭১ মেহেরপুরের বৈদ্যনাথতলায় মুজিবনগর সরকার শপথ গ্রহণ করে।", Difficulty.MEDIUM, QuestionProvenance.OFFICIAL),
        q("q-sectors", "t-1971",
            "Into how many sectors was Bangladesh divided during the Liberation War?", "মুক্তিযুদ্ধের সময় বাংলাদেশকে কয়টি সেক্টরে ভাগ করা হয়েছিল?",
            listOf(o("9", "৯"), o("10", "১০"), o("11", "১১"), o("12", "১২")),
            listOf(2), "The war zone was divided into 11 sectors, each led by a sector commander.",
            "যুদ্ধক্ষেত্রকে ১১টি সেক্টরে ভাগ করা হয়, প্রতিটির দায়িত্বে ছিলেন একজন সেক্টর কমান্ডার।", Difficulty.EASY),
        q("q-unesco", "t-language",
            "When did UNESCO recognise 21 February as International Mother Language Day?", "ইউনেস্কো কবে ২১ ফেব্রুয়ারিকে আন্তর্জাতিক মাতৃভাষা দিবস হিসেবে স্বীকৃতি দেয়?",
            listOf(o("17 November 1999", "১৭ নভেম্বর ১৯৯৯"), o("21 February 2000", "২১ ফেব্রুয়ারি ২০০০", "This is when the day was first observed internationally, not when it was declared.", "এ দিন প্রথম আন্তর্জাতিকভাবে পালিত হয়; স্বীকৃতির তারিখ নয়।"), o("16 December 1999", "১৬ ডিসেম্বর ১৯৯৯"), o("17 November 2000", "১৭ নভেম্বর ২০০০")),
            listOf(0), "UNESCO's General Conference proclaimed it on 17 November 1999; it was first observed on 21 February 2000.",
            "ইউনেস্কোর সাধারণ সম্মেলনে ১৭ নভেম্বর ১৯৯৯ এ স্বীকৃতি দেওয়া হয়; ২০০০ সালের ২১ ফেব্রুয়ারি প্রথম পালিত হয়।", Difficulty.HARD),
        q("q-constitution", "t-constitution",
            "When did the Constitution of Bangladesh come into effect?", "বাংলাদেশের সংবিধান কবে কার্যকর হয়?",
            listOf(o("4 November 1972", "৪ নভেম্বর ১৯৭২", "That is the date it was adopted by the Constituent Assembly.", "এটি গণপরিষদে গৃহীত হওয়ার তারিখ।"), o("16 December 1972", "১৬ ডিসেম্বর ১৯৭২"), o("26 March 1972", "২৬ মার্চ ১৯৭২"), o("10 April 1972", "১০ এপ্রিল ১৯৭২")),
            listOf(1), "Adopted on 4 November 1972 and effective from 16 December 1972.",
            "১৯৭২ সালের ৪ নভেম্বর গৃহীত এবং ১৬ ডিসেম্বর ১৯৭২ থেকে কার্যকর।", Difficulty.MEDIUM, QuestionProvenance.OFFICIAL),
        // ---- Bangla ----
        q("q-agnibina", "t-nazrul",
            "Who wrote the poetry collection ‘Agnibina’?", "‘অগ্নিবীণা’ কাব্যগ্রন্থের রচয়িতা কে?",
            listOf(o("Rabindranath Tagore", "রবীন্দ্রনাথ ঠাকুর"), o("Kazi Nazrul Islam", "কাজী নজরুল ইসলাম"), o("Jibanananda Das", "জীবনানন্দ দাশ"), o("Jasimuddin", "জসীমউদ্‌দীন")),
            listOf(1), "Agnibina (1922) is Kazi Nazrul Islam's first poetry collection; it opens with ‘Proloyollash’.",
            "‘অগ্নিবীণা’ (১৯২২) কাজী নজরুল ইসলামের প্রথম কাব্যগ্রন্থ; এর প্রথম কবিতা ‘প্রলয়োল্লাস’।", Difficulty.EASY, QuestionProvenance.OFFICIAL),
        q("q-charyapada", "t-charyapada",
            "What is the earliest known specimen of Bengali literature?", "বাংলা সাহিত্যের প্রাচীনতম নিদর্শন কোনটি?",
            listOf(o("Charyapada", "চর্যাপদ"), o("Shreekrishna Kirtan", "শ্রীকৃষ্ণকীর্তন", "A Middle Bengali work, later than Charyapada.", "মধ্যযুগের রচনা, চর্যাপদের পরে।"), o("Mangalkavya", "মঙ্গলকাব্য"), o("Padmavati", "পদ্মাবতী")),
            listOf(0), "Charyapada, discovered by Haraprasad Shastri in Nepal in 1907, is the oldest specimen.",
            "১৯০৭ সালে হরপ্রসাদ শাস্ত্রী নেপাল থেকে চর্যাপদ আবিষ্কার করেন; এটিই প্রাচীনতম নিদর্শন।", Difficulty.EASY),
        q("q-nobel", "t-tagore",
            "In which year did Rabindranath Tagore win the Nobel Prize in Literature?", "রবীন্দ্রনাথ ঠাকুর কোন সালে সাহিত্যে নোবেল পুরস্কার পান?",
            listOf(o("1911", "১৯১১"), o("1913", "১৯১৩"), o("1915", "১৯১৫"), o("1921", "১৯২১")),
            listOf(1), "He received the Nobel Prize in 1913, largely for the English Gitanjali (Song Offerings).",
            "১৯১৩ সালে ইংরেজি ‘গীতাঞ্জলি’ (Song Offerings)-এর জন্য তিনি নোবেল পান।", Difficulty.EASY, QuestionProvenance.OFFICIAL),
        // ---- English ----
        q("q-abundant", "t-synonyms",
            "Choose the synonym of ‘Abundant’.", "‘Abundant’ শব্দের সমার্থক নির্বাচন করুন।",
            listOf(o("Scarce", null, "Scarce is the antonym.", "Scarce বিপরীত শব্দ।"), o("Plentiful"), o("Rare", null, "Rare means uncommon — close to the opposite.", "Rare মানে বিরল — প্রায় বিপরীত অর্থ।"), o("Hidden")),
            listOf(1), "Abundant means existing in large quantities — plentiful.", "Abundant অর্থ প্রচুর — Plentiful।", Difficulty.EASY),
        q("q-spelling", "t-spelling",
            "Which spelling is correct?", "কোন বানানটি শুদ্ধ?",
            listOf(o("Accomodation"), o("Acommodation"), o("Accommodation"), o("Acomodation")),
            listOf(2), "Accommodation has double ‘c’ and double ‘m’.", "Accommodation বানানে দুটি ‘c’ ও দুটি ‘m’ আছে।", Difficulty.MEDIUM),
        q("q-callitaday", "t-idioms",
            "‘Call it a day’ means —", "‘Call it a day’ এর অর্থ —",
            listOf(o("To name a date"), o("To stop working for now"), o("To make a phone call"), o("To start early")),
            listOf(1), "To ‘call it a day’ is to decide to stop doing something, usually work.", "কাজ আপাতত বন্ধ করার সিদ্ধান্ত নেওয়া।", Difficulty.MEDIUM, QuestionProvenance.COMMUNITY),
        q("q-passage1", "t-reading",
            "According to the passage, what renews the fertility of the floodplains?", "অনুচ্ছেদ অনুসারে প্লাবনভূমির উর্বরতা কী নবায়ন করে?",
            listOf(o("Raised plinths"), o("Silt carried by the rivers"), o("Chemical fertiliser"), o("Dry winters")),
            listOf(1), "The passage states the rivers carry silt every monsoon, which renews fertility.", "অনুচ্ছেদে বলা হয়েছে বর্ষায় নদীবাহিত পলি উর্বরতা নবায়ন করে।",
            Difficulty.EASY, passage = readingPassage),
        q("q-passage2", "t-reading",
            "How have communities adapted to floods, according to the passage?", "অনুচ্ছেদ অনুসারে মানুষ কীভাবে বন্যার সাথে খাপ খাইয়েছে?",
            listOf(o("By moving to cities"), o("By building dams"), o("By building on raised plinths"), o("By avoiding farming")),
            listOf(2), "The last sentence mentions building on raised plinths.", "শেষ বাক্যে উঁচু ভিটায় ঘর তোলার কথা বলা হয়েছে।",
            Difficulty.EASY, passage = readingPassage),
        // ---- Math ----
        q("q-si", "t-interest",
            "What is the simple interest on Tk 5,000 at 8% per annum for 3 years?", "৫,০০০ টাকার ৮% হার মুনাফায় ৩ বছরের সরল মুনাফা কত?",
            listOf(o("Tk 1,000", "১,০০০ টাকা"), o("Tk 1,200", "১,২০০ টাকা"), o("Tk 1,250", "১,২৫০ টাকা", "This would be the result at 8.33%, a common rounding mistake.", "৮.৩৩% ধরলে এমন হয় — সাধারণ ভুল।"), o("Tk 1,500", "১,৫০০ টাকা")),
            listOf(1), "I = P × r × t = 5000 × 0.08 × 3 = 1200.", "মুনাফা = আসল × হার × সময় = ৫০০০ × ০.০৮ × ৩ = ১২০০।",
            Difficulty.EASY, formula = "I = P × r × t"),
        q("q-identity", "t-identities",
            "If x + 1/x = 3, what is x² + 1/x²?", "x + 1/x = 3 হলে x² + 1/x² এর মান কত?",
            listOf(o("9", "৯", "That is (x + 1/x)²; you must subtract 2.", "এটি (x + 1/x)²; ২ বিয়োগ করতে হবে।"), o("7", "৭"), o("11", "১১", "Adding 2 instead of subtracting.", "২ বিয়োগের বদলে যোগ করা হয়েছে।"), o("6", "৬")),
            listOf(1), "x² + 1/x² = (x + 1/x)² − 2 = 9 − 2 = 7.", "x² + 1/x² = (x + 1/x)² − 2 = ৯ − ২ = ৭।",
            Difficulty.MEDIUM, QuestionProvenance.OFFICIAL, formula = "x² + 1/x² = (x + 1/x)² − 2"),
        q("q-average", "t-average",
            "The average of 5 numbers is 20. If one number is removed, the average becomes 18. What number was removed?", "৫টি সংখ্যার গড় ২০। একটি সংখ্যা বাদ দিলে গড় হয় ১৮। বাদ দেওয়া সংখ্যাটি কত?",
            listOf(o("22", "২২"), o("26", "২৬"), o("28", "২৮"), o("30", "৩০")),
            listOf(2), "Total = 5 × 20 = 100; remaining total = 4 × 18 = 72; removed = 100 − 72 = 28.", "মোট = ৫ × ২০ = ১০০; বাকি ৪টির মোট = ৪ × ১৮ = ৭২; বাদ = ১০০ − ৭২ = ২৮।",
            Difficulty.MEDIUM),
        q("q-primes", "t-primes",
            "Which of the following are prime numbers?", "নিচের কোনগুলো মৌলিক সংখ্যা?",
            listOf(o("2", "২"), o("9", "৯", "9 = 3 × 3.", "৯ = ৩ × ৩।"), o("17", "১৭"), o("21", "২১", "21 = 3 × 7.", "২১ = ৩ × ৭।")),
            listOf(0, 2), "2 and 17 have exactly two divisors: 1 and themselves.", "২ ও ১৭ এর কেবল দুটি উৎপাদক: ১ ও সংখ্যাটি নিজে।",
            Difficulty.EASY, type = QuestionType.MULTI_SELECT),
        // ---- Science ----
        q("q-vitd", "t-vitamins",
            "Which vitamin is produced in human skin in sunlight?", "সূর্যালোকের উপস্থিতিতে মানুষের ত্বকে কোন ভিটামিন তৈরি হয়?",
            listOf(o("Vitamin A", "ভিটামিন এ"), o("Vitamin B12", "ভিটামিন বি১২"), o("Vitamin C", "ভিটামিন সি"), o("Vitamin D", "ভিটামিন ডি")),
            listOf(3), "UVB light converts 7-dehydrocholesterol in the skin into vitamin D3.", "অতিবেগুনি রশ্মি ত্বকের ৭-ডিহাইড্রোকোলেস্টেরলকে ভিটামিন ডি৩-তে রূপান্তর করে।", Difficulty.EASY),
        q("q-sound", "t-sound",
            "Sound travels faster in water than in air.", "বাতাসের চেয়ে পানিতে শব্দের বেগ বেশি।",
            listOf(o("True", "সত্য"), o("False", "মিথ্যা")),
            listOf(0), "Sound travels about 1,480 m/s in water versus about 343 m/s in air.", "পানিতে শব্দের বেগ প্রায় ১,৪৮০ মি/সে, বাতাসে প্রায় ৩৪৩ মি/সে।",
            Difficulty.EASY, type = QuestionType.TRUE_FALSE),
        q("q-water", "t-chem-basics",
            "What is the chemical formula of water?", "পানির রাসায়নিক সংকেত কী?",
            listOf(o("H₂O"), o("H₂O₂", null, "H₂O₂ is hydrogen peroxide.", "H₂O₂ হলো হাইড্রোজেন পার-অক্সাইড।"), o("HO"), o("CO₂")),
            listOf(0), "Each water molecule has two hydrogen atoms and one oxygen atom.", "পানির প্রতিটি অণুতে দুটি হাইড্রোজেন ও একটি অক্সিজেন পরমাণু থাকে।", Difficulty.EASY),
        // ---- ICT ----
        q("q-kb", "t-memory",
            "In binary convention, 1 kilobyte (KB) equals how many bytes?", "বাইনারি হিসাবে ১ কিলোবাইট (KB) = কত বাইট?",
            listOf(o("1000", "১০০০", "1000 is the decimal (SI) kilobyte; exams usually expect 1024.", "১০০০ দশমিক হিসাব; পরীক্ষায় সাধারণত ১০২৪ ধরা হয়।"), o("1024", "১০২৪"), o("512", "৫১২"), o("2048", "২০৪৮")),
            listOf(1), "1 KB = 2¹⁰ = 1024 bytes in the binary convention.", "বাইনারি হিসাবে ১ KB = ২¹⁰ = ১০২৪ বাইট।", Difficulty.EASY, QuestionProvenance.OFFICIAL),
        q("q-input", "t-io",
            "Which one is an input device?", "কোনটি ইনপুট ডিভাইস?",
            listOf(o("Monitor", "মনিটর"), o("Printer", "প্রিন্টার"), o("Keyboard", "কি-বোর্ড"), o("Speaker", "স্পিকার")),
            listOf(2), "A keyboard sends data into the computer; the others present output.", "কি-বোর্ড দিয়ে কম্পিউটারে তথ্য দেওয়া হয়; বাকিগুলো আউটপুট দেখায়।", Difficulty.EASY),
        q("q-url", "t-internet",
            "What does URL stand for?", "URL এর পূর্ণরূপ কী?",
            listOf(o("Universal Resource Link"), o("Uniform Resource Locator"), o("Uniform Retrieval Locator"), o("Unified Resource Line")),
            listOf(1), "URL = Uniform Resource Locator, the address of a resource on the web.", "URL = Uniform Resource Locator, ওয়েবে কোনো রিসোর্সের ঠিকানা।", Difficulty.MEDIUM),
        // ---- International ----
        q("q-un-hq", "t-un",
            "Where is the headquarters of the United Nations?", "জাতিসংঘের সদর দপ্তর কোথায়?",
            listOf(o("Geneva", "জেনেভা", "Geneva hosts the UN Office at Geneva, not the headquarters.", "জেনেভায় জাতিসংঘের একটি দপ্তর আছে, সদর দপ্তর নয়।"), o("New York", "নিউইয়র্ক"), o("Paris", "প্যারিস"), o("Vienna", "ভিয়েনা")),
            listOf(1), "UN headquarters is in New York City.", "জাতিসংঘের সদর দপ্তর নিউইয়র্কে।", Difficulty.EASY),
        q("q-saarc", "t-saarc",
            "How many member states does SAARC have?", "সার্কের সদস্য রাষ্ট্র কয়টি?",
            listOf(o("7", "৭", "SAARC started with 7 members in 1985.", "১৯৮৫ সালে ৭টি সদস্য নিয়ে শুরু হয়।"), o("8", "৮"), o("9", "৯"), o("10", "১০")),
            listOf(1), "Afghanistan joined in 2007, making 8 members.", "২০০৭ সালে আফগানিস্তান যোগ দেওয়ায় সদস্য ৮টি।", Difficulty.MEDIUM),
        // ---- Geography ----
        q("q-mangrove", "t-forests",
            "Which is the largest mangrove forest in the world?", "বিশ্বের বৃহত্তম ম্যানগ্রোভ বন কোনটি?",
            listOf(o("Amazon", "অ্যামাজন"), o("Sundarbans", "সুন্দরবন"), o("Bhawal", "ভাওয়াল"), o("Lawachara", "লাউয়াছড়া")),
            listOf(1), "The Sundarbans, shared by Bangladesh and India, is the largest mangrove forest.", "বাংলাদেশ ও ভারতে বিস্তৃত সুন্দরবন বিশ্বের বৃহত্তম ম্যানগ্রোভ বন।", Difficulty.EASY, QuestionProvenance.OFFICIAL),
        q("q-coral", "t-forests",
            "What is the only coral island of Bangladesh?", "বাংলাদেশের একমাত্র প্রবাল দ্বীপ কোনটি?",
            listOf(o("Bhola", "ভোলা"), o("Hatiya", "হাতিয়া"), o("St. Martin's Island", "সেন্ট মার্টিন'স দ্বীপ"), o("Sandwip", "সন্দ্বীপ")),
            listOf(2), "St. Martin's Island (Narikel Jinjira) is the only coral island.", "সেন্ট মার্টিন'স দ্বীপ (নারিকেল জিঞ্জিরা) একমাত্র প্রবাল দ্বীপ।", Difficulty.EASY),
        // ---- Mental ability ----
        q("q-series", "t-series",
            "Find the next number: 2, 6, 12, 20, 30, ?", "পরবর্তী সংখ্যা কত: ২, ৬, ১২, ২০, ৩০, ?",
            listOf(o("40", "৪০"), o("42", "৪২"), o("44", "৪৪"), o("36", "৩৬")),
            listOf(1), "Differences are 4, 6, 8, 10, so the next difference is 12: 30 + 12 = 42. (Also n(n+1).)", "পার্থক্য ৪, ৬, ৮, ১০ — পরেরটি ১২: ৩০ + ১২ = ৪২। (n(n+1) সূত্রও খাটে।)",
            Difficulty.MEDIUM, formula = "aₙ = n(n + 1)"),
        // ---- Ethics ----
        q("q-integrity", "t-integrity",
            "In which year was Bangladesh's National Integrity Strategy adopted?", "বাংলাদেশের জাতীয় শুদ্ধাচার কৌশল কত সালে প্রণীত হয়?",
            listOf(o("2008", "২০০৮"), o("2010", "২০১০"), o("2012", "২০১২"), o("2015", "২০১৫")),
            listOf(2), "The cabinet approved the National Integrity Strategy in 2012.", "২০১২ সালে মন্ত্রিসভা জাতীয় শুদ্ধাচার কৌশল অনুমোদন করে।", Difficulty.HARD),
        // ---- Written / viva ----
        q("q-written-language", "t-language",
            "Write a short note on the significance of the Language Movement of 1952.", "১৯৫২ সালের ভাষা আন্দোলনের তাৎপর্য সম্পর্কে একটি সংক্ষিপ্ত টীকা লিখুন।",
            emptyList(), emptyList(),
            "Cover: the demand for Bangla as a state language, the events of 21 February 1952, its role in building Bengali nationalism, and international recognition.",
            "উল্লেখ করুন: রাষ্ট্রভাষা বাংলার দাবি, ২১ ফেব্রুয়ারি ১৯৫২-এর ঘটনা, বাঙালি জাতীয়তাবাদ গঠনে ভূমিকা এবং আন্তর্জাতিক স্বীকৃতি।",
            Difficulty.MEDIUM, type = QuestionType.WRITTEN,
            modelAnswer = t(
                "The Language Movement demanded recognition of Bangla as a state language of Pakistan. On 21 February 1952 police fired on protesting students in Dhaka. The sacrifice united Bengalis around their linguistic identity and laid the foundation of the nationalism that led to independence in 1971. UNESCO later declared 21 February International Mother Language Day.",
                "ভাষা আন্দোলনে বাংলাকে পাকিস্তানের অন্যতম রাষ্ট্রভাষা করার দাবি ওঠে। ১৯৫২ সালের ২১ ফেব্রুয়ারি ঢাকায় বিক্ষোভরত ছাত্রদের ওপর পুলিশ গুলি চালায়। এই আত্মত্যাগ বাঙালিকে ভাষাভিত্তিক পরিচয়ে ঐক্যবদ্ধ করে এবং ১৯৭১-এর স্বাধীনতার পথে জাতীয়তাবাদের ভিত্তি গড়ে। পরে ইউনেস্কো ২১ ফেব্রুয়ারিকে আন্তর্জাতিক মাতৃভাষা দিবস ঘোষণা করে।",
            )),
        q("q-viva-why", "t-integrity",
            "Viva: Why do you want to join the civil service?", "মৌখিক: আপনি কেন সিভিল সার্ভিসে যোগ দিতে চান?",
            emptyList(), emptyList(),
            "Be specific and sincere: link a personal experience to public service, mention a cadre-relevant skill, and avoid generic answers.",
            "সুনির্দিষ্ট ও আন্তরিক হোন: ব্যক্তিগত অভিজ্ঞতাকে জনসেবার সাথে যুক্ত করুন, পছন্দের ক্যাডার-সংশ্লিষ্ট দক্ষতা উল্লেখ করুন, গতানুগতিক উত্তর এড়িয়ে চলুন।",
            Difficulty.MEDIUM, type = QuestionType.VIVA),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun objective() = all.filter { it.options.isNotEmpty() }
}
