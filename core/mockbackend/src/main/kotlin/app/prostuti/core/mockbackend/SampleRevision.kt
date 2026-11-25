package app.prostuti.core.mockbackend

import app.prostuti.core.model.*
import java.time.Instant

internal object SampleRevision {
    val decks = listOf(
        Triple("deck-facts", t("Important facts", "গুরুত্বপূর্ণ তথ্য"), DeckKind.FACTS),
        Triple("deck-formula", t("Math formulas", "গণিতের সূত্র"), DeckKind.FORMULA),
        Triple("deck-vocab", t("English vocabulary", "ইংরেজি শব্দভাণ্ডার"), DeckKind.VOCABULARY),
    )

    fun cards(): List<Flashcard> {
        val now = Instant.now()
        fun card(id: String, deck: String, f: LocalizedText, b: LocalizedText, origin: FlashcardOrigin = FlashcardOrigin.EDITORIAL, dueOffsetH: Long = -1) =
            Flashcard(id, deck, f, b, origin, null, SrsState(0, 0, 2.5, now.plusSeconds(dueOffsetH * 3600), null))
        return listOf(
            card("f1", "deck-facts", t("Victory Day of Bangladesh", "বাংলাদেশের বিজয় দিবস"), t("16 December 1971", "১৬ ডিসেম্বর ১৯৭১")),
            card("f2", "deck-facts", t("Mujibnagar Government oath", "মুজিবনগর সরকারের শপথ"), t("17 April 1971, Baidyanathtala, Meherpur", "১৭ এপ্রিল ১৯৭১, বৈদ্যনাথতলা, মেহেরপুর")),
            card("f3", "deck-facts", t("Number of sectors in 1971", "১৯৭১-এ সেক্টর সংখ্যা"), t("11", "১১টি")),
            card("f4", "deck-facts", t("Only coral island of Bangladesh", "বাংলাদেশের একমাত্র প্রবাল দ্বীপ"), t("St. Martin's Island", "সেন্ট মার্টিন'স দ্বীপ")),
            card("f5", "deck-formula", t("Simple interest", "সরল মুনাফা"), t("I = P × r × t")),
            card("f6", "deck-formula", t("x² + 1/x² in terms of x + 1/x", "x + 1/x দিয়ে x² + 1/x²"), t("(x + 1/x)² − 2")),
            card("f7", "deck-formula", t("Compound amount", "চক্রবৃদ্ধি সবৃদ্ধিমূল"), t("A = P(1 + r)ⁿ"), dueOffsetH = 30),
            card("f8", "deck-vocab", t("Abundant", "Abundant"), t("Plentiful; existing in large quantities", "প্রচুর")),
            card("f9", "deck-vocab", t("Call it a day", "Call it a day"), t("Stop working for now", "আপাতত কাজ বন্ধ করা"), FlashcardOrigin.AI_APPROVED),
            card("f10", "deck-vocab", t("Scarce", "Scarce"), t("Insufficient; in short supply", "দুষ্প্রাপ্য"), dueOffsetH = 48),
        )
    }

    fun notes(): List<Note> = listOf(
        Note("n1", "t-1971", null, "1971 quick list", "25 Mar crackdown → 26 Mar independence → 10 Apr govt formed → 17 Apr oath → 16 Dec victory", Instant.now().minusSeconds(7200), true),
        Note("n2", "t-interest", null, "Interest mistakes", "Always convert % to decimal. Check if time is in months!", Instant.now().minusSeconds(86_400 * 2), true),
    )
}
