package app.prostuti.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/* Brand palette. Deep emerald carries the brand; everything else stays quiet and neutral. */
internal object Palette {
    val Emerald900 = Color(0xFF00382A)
    val Emerald800 = Color(0xFF004D3A)
    val Emerald700 = Color(0xFF0A6B4E)
    val Emerald600 = Color(0xFF137F5E)
    val Emerald300 = Color(0xFF7DD8B2)
    val Emerald100 = Color(0xFFD4EEE3)
    val Emerald50 = Color(0xFFEBF6F1)

    val Paper = Color(0xFFF8F9F6)
    val Ink = Color(0xFF16201B)
    val InkMuted = Color(0xFF56615B)
    val Line = Color(0xFFDCE2DE)
    val LineStrong = Color(0xFF79827D)

    val Amber600 = Color(0xFFB7791F)
    val Amber100 = Color(0xFFFCEBCB)
    val Red600 = Color(0xFFC0362C)
    val Red100 = Color(0xFFFBE2DF)
    val Blue600 = Color(0xFF2F5FA7)
    val Blue100 = Color(0xFFDDE7F6)

    val Night = Color(0xFF0E1412)
    val NightSurface = Color(0xFF151C19)
    val NightRaised = Color(0xFF1C2521)
    val NightLine = Color(0xFF2D3833)
}

internal val LightColors = lightColorScheme(
    primary = Palette.Emerald700,
    onPrimary = Color.White,
    primaryContainer = Palette.Emerald100,
    onPrimaryContainer = Palette.Emerald900,
    inversePrimary = Palette.Emerald300,
    secondary = Color(0xFF47605A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EBE7),
    onSecondaryContainer = Color(0xFF14201C),
    tertiary = Palette.Amber600,
    onTertiary = Color.White,
    tertiaryContainer = Palette.Amber100,
    onTertiaryContainer = Color(0xFF3D2700),
    error = Palette.Red600,
    onError = Color.White,
    errorContainer = Palette.Red100,
    onErrorContainer = Color(0xFF410002),
    background = Palette.Paper,
    onBackground = Palette.Ink,
    surface = Palette.Paper,
    onSurface = Palette.Ink,
    surfaceVariant = Color(0xFFEDF1EE),
    onSurfaceVariant = Palette.InkMuted,
    surfaceTint = Palette.Emerald700,
    outline = Palette.LineStrong,
    outlineVariant = Palette.Line,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFDFB),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F4F2),
    surfaceContainerHighest = Color(0xFFE9EDEA),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDDE2DF),
    inverseSurface = Color(0xFF2B322F),
    inverseOnSurface = Color(0xFFEEF2EF),
    scrim = Color.Black,
)

internal val DarkColors = darkColorScheme(
    primary = Palette.Emerald300,
    onPrimary = Palette.Emerald900,
    primaryContainer = Palette.Emerald800,
    onPrimaryContainer = Palette.Emerald100,
    inversePrimary = Palette.Emerald700,
    secondary = Color(0xFFAFC9C0),
    onSecondary = Color(0xFF1A302A),
    secondaryContainer = Color(0xFF2A3A35),
    onSecondaryContainer = Color(0xFFD5E6E0),
    tertiary = Color(0xFFF2C47A),
    onTertiary = Color(0xFF412C00),
    tertiaryContainer = Color(0xFF5C4100),
    onTertiaryContainer = Palette.Amber100,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Palette.Night,
    onBackground = Color(0xFFE1E6E3),
    surface = Palette.Night,
    onSurface = Color(0xFFE1E6E3),
    surfaceVariant = Color(0xFF26302C),
    onSurfaceVariant = Color(0xFFB8C3BD),
    surfaceTint = Palette.Emerald300,
    outline = Color(0xFF86918B),
    outlineVariant = Palette.NightLine,
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF121916),
    surfaceContainer = Palette.NightSurface,
    surfaceContainerHigh = Palette.NightRaised,
    surfaceContainerHighest = Color(0xFF242E2A),
    surfaceBright = Color(0xFF333D39),
    surfaceDim = Palette.Night,
    inverseSurface = Color(0xFFE1E6E3),
    inverseOnSurface = Color(0xFF2B322F),
    scrim = Color.Black,
)

/** Semantic colors Material 3 does not cover: answer states, provenance and status. */
@Immutable
data class ExtendedColors(
    val correct: Color,
    val correctContainer: Color,
    val incorrect: Color,
    val incorrectContainer: Color,
    val review: Color,
    val reviewContainer: Color,
    val official: Color,
    val verified: Color,
    val verifiedContainer: Color,
    val community: Color,
    val communityContainer: Color,
    val streak: Color,
    val chartTrack: Color,
)

internal val LightExtended = ExtendedColors(
    correct = Color(0xFF12784F),
    correctContainer = Color(0xFFDDF3E8),
    incorrect = Palette.Red600,
    incorrectContainer = Palette.Red100,
    review = Color(0xFF7A4FB0),
    reviewContainer = Color(0xFFEDE3F8),
    official = Palette.Emerald700,
    verified = Palette.Blue600,
    verifiedContainer = Palette.Blue100,
    community = Palette.Amber600,
    communityContainer = Palette.Amber100,
    streak = Color(0xFFE07B12),
    chartTrack = Color(0xFFE6EBE8),
)

internal val DarkExtended = ExtendedColors(
    correct = Color(0xFF6FD6A4),
    correctContainer = Color(0xFF113B2A),
    incorrect = Color(0xFFFF9C92),
    incorrectContainer = Color(0xFF4A1612),
    review = Color(0xFFC9A8F2),
    reviewContainer = Color(0xFF33224A),
    official = Palette.Emerald300,
    verified = Color(0xFF9DBCF0),
    verifiedContainer = Color(0xFF1C2D48),
    community = Color(0xFFF2C47A),
    communityContainer = Color(0xFF3E2D0A),
    streak = Color(0xFFFFA94D),
    chartTrack = Color(0xFF26302C),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }
