package app.prostuti.core.mockbackend

import app.prostuti.core.model.*

internal object SampleMaterials {
    fun blocksFor(topicId: String): List<ContentBlock> = when (topicId) {
        "t-1971" -> listOf(
            ContentBlock.Paragraph(t(
                "The Liberation War began after the Pakistani army's crackdown on the night of 25 March 1971 and ended with the surrender of Pakistani forces in Dhaka on 16 December 1971.",
                "১৯৭১ সালের ২৫ মার্চ রাতে পাকিস্তানি সেনাবাহিনীর গণহত্যার পর মুক্তিযুদ্ধ শুরু হয় এবং ১৬ ডিসেম্বর ঢাকায় পাকিস্তানি বাহিনীর আত্মসমর্পণের মধ্য দিয়ে শেষ হয়।",
            )),
            ContentBlock.Heading(t("Key dates", "গুরুত্বপূর্ণ তারিখ")),
            ContentBlock.Table(
                header = listOf(t("Date", "তারিখ"), t("Event", "ঘটনা")),
                rows = listOf(
                    listOf(t("7 March 1971", "৭ মার্চ ১৯৭১"), t("Historic speech at the Racecourse", "রেসকোর্স ময়দানে ঐতিহাসিক ভাষণ")),
                    listOf(t("26 March 1971", "২৬ মার্চ ১৯৭১"), t("Declaration of independence", "স্বাধীনতার ঘোষণা")),
                    listOf(t("17 April 1971", "১৭ এপ্রিল ১৯৭১"), t("Mujibnagar Government takes oath", "মুজিবনগর সরকারের শপথ")),
                    listOf(t("16 December 1971", "১৬ ডিসেম্বর ১৯৭১"), t("Victory Day", "বিজয় দিবস")),
                ),
            ),
            ContentBlock.Callout(CalloutKind.EXAM_TRAP, t(
                "Do not confuse the date the Mujibnagar Government was formed (10 April) with the oath (17 April).",
                "মুজিবনগর সরকার গঠনের তারিখ (১০ এপ্রিল) আর শপথের তারিখ (১৭ এপ্রিল) গুলিয়ে ফেলবেন না।",
            )),
            ContentBlock.Bullets(listOf(
                t("The war zone was divided into 11 sectors.", "যুদ্ধক্ষেত্রকে ১১টি সেক্টরে ভাগ করা হয়।"),
                t("Gallantry awards: Bir Sreshtho, Bir Uttom, Bir Bikrom, Bir Protik.", "বীরত্বসূচক খেতাব: বীরশ্রেষ্ঠ, বীর উত্তম, বীর বিক্রম, বীর প্রতীক।"),
            )),
            ContentBlock.Callout(CalloutKind.REMEMBER, t("Seven freedom fighters received the Bir Sreshtho award.", "সাতজন মুক্তিযোদ্ধা বীরশ্রেষ্ঠ খেতাব পেয়েছেন।")),
        )
        "t-interest" -> listOf(
            ContentBlock.Paragraph(t(
                "Simple interest is charged only on the principal. Compound interest is charged on the principal plus interest already added.",
                "সরল মুনাফা কেবল আসলের ওপর হিসাব হয়। চক্রবৃদ্ধি মুনাফা আসল ও আগে যোগ হওয়া মুনাফার ওপর হিসাব হয়।",
            )),
            ContentBlock.Formula("I = P × r × t", t("Interest equals principal times rate times time", "মুনাফা সমান আসল গুণ হার গুণ সময়")),
            ContentBlock.Formula("A = P(1 + r)ⁿ", t("Amount equals principal times one plus rate, to the power n", "সবৃদ্ধিমূল সমান আসল গুণ এক যোগ হার এর n ঘাত")),
            ContentBlock.Callout(CalloutKind.TIP, t(
                "Convert the rate to a decimal first: 8% = 0.08. Most mistakes come from skipping this.",
                "প্রথমে হারকে দশমিকে নিন: ৮% = ০.০৮। বেশিরভাগ ভুল এখান থেকেই হয়।",
            )),
            ContentBlock.Heading(t("Worked example", "উদাহরণ"), 3),
            ContentBlock.Paragraph(t(
                "Tk 5,000 at 8% for 3 years: I = 5000 × 0.08 × 3 = Tk 1,200. Amount = 5000 + 1200 = Tk 6,200.",
                "৫,০০০ টাকা, ৮% হারে ৩ বছর: I = ৫০০০ × ০.০৮ × ৩ = ১,২০০ টাকা। সবৃদ্ধিমূল = ৫০০০ + ১২০০ = ৬,২০০ টাকা।",
            )),
        )
        else -> {
            val questions = SampleQuestions.all.filter { it.topicId == topicId && it.solution != null }
            listOf(
                ContentBlock.Paragraph(t(
                    "Key points for this topic, distilled from frequently tested questions.",
                    "এই টপিকের মূল বিষয়গুলো, প্রায়ই আসা প্রশ্ন থেকে সংকলিত।",
                )),
                ContentBlock.Bullets(questions.map { it.solution!!.explanation }),
                ContentBlock.Callout(CalloutKind.TIP, t("Practise the related questions right after reading — recall beats re-reading.", "পড়ার পরপরই সংশ্লিষ্ট প্রশ্ন অনুশীলন করুন — বারবার পড়ার চেয়ে মনে করার চেষ্টা বেশি কার্যকর।")),
            )
        }
    }
}
