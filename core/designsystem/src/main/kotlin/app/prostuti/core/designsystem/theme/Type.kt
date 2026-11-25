package app.prostuti.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * System fonts only: Roboto for Latin and the platform's Noto Sans Bengali for Bangla — zero
 * download, zero APK cost. Line heights are deliberately generous because Bengali conjuncts and
 * vowel signs (e.g. ু ূ ৃ) need vertical room to stay legible on small screens.
 */
private val Sans = FontFamily.Default

internal val ProstutiTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.25).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
)

/** Tabular figures so timers and scores do not jitter as digits change. */
val TextStyle.tabular: TextStyle get() = copy(fontFeatureSettings = "tnum")

/** Small uppercase-free overline used for section eyebrows. */
val TextStyle.eyebrow: TextStyle get() = copy(letterSpacing = 0.06.em, fontWeight = FontWeight.SemiBold)
