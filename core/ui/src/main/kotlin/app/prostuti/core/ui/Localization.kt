package app.prostuti.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import app.prostuti.core.model.AppLanguage
import app.prostuti.core.model.LocalizedText
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** The language the student chose for content. Provided once at the app root. */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.BENGALI }

@Composable
@ReadOnlyComposable
fun LocalizedText.text(): String = resolve(LocalAppLanguage.current)

private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

/** Bengali UI shows Bengali numerals (১২৩) — small detail, big difference in feeling native. */
fun String.localizeDigits(language: AppLanguage): String =
    if (language != AppLanguage.BENGALI) this
    else buildString(length) { this@localizeDigits.forEach { c -> append(if (c in '0'..'9') BENGALI_DIGITS[c - '0'] else c) } }

@Composable
@ReadOnlyComposable
fun Number.loc(): String {
    val s = when (this) {
        is Double, is Float -> toDouble().let { if (it % 1.0 == 0.0) it.toLong().toString() else String.format(Locale.US, "%.2f", it).trimEnd('0').trimEnd('.') }
        else -> toString()
    }
    return s.localizeDigits(LocalAppLanguage.current)
}

@Composable
@ReadOnlyComposable
fun String.loc(): String = localizeDigits(LocalAppLanguage.current)

@Composable
@ReadOnlyComposable
fun percent(value: Float): String = "${(value * 100).roundToInt()}%".localizeDigits(LocalAppLanguage.current)

@Composable
fun minutesLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) stringResource(R.string.ui_hours_minutes_short, h.loc(), m.loc())
    else stringResource(R.string.ui_minutes_short, m.loc())
}

/** mm:ss or h:mm:ss with tabular-friendly zero padding. */
fun Duration.clock(language: AppLanguage): String {
    val total = seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    val raw = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
    return raw.localizeDigits(language)
}

@Composable
fun LocalDate.pretty(pattern: String = "d MMM yyyy"): String {
    val lang = LocalAppLanguage.current
    val locale = if (lang == AppLanguage.BENGALI) Locale.forLanguageTag("bn-BD") else Locale.ENGLISH
    return format(DateTimeFormatter.ofPattern(pattern, locale)).localizeDigits(lang)
}

fun daysUntil(date: LocalDate, today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(today, date)

/** Build facts screens may need, e.g. to show the development OTP in mock builds. */
data class BuildInfo(val isMockBackend: Boolean, val versionName: String)

val LocalBuildInfo = staticCompositionLocalOf { BuildInfo(isMockBackend = false, versionName = "") }
