package app.prostuti.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

internal val ProstutiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 4dp grid. Screen gutters are 20dp on phones; content max width keeps tablets readable. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val gutter = 20.dp
    val contentMaxWidth = 640.dp
}

/**
 * Brand colors are fixed (no dynamic color) so emerald stays recognisable and answer-state colors
 * keep their meaning on every device.
 */
@Composable
fun ProstutiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ProstutiTypography,
            shapes = ProstutiShapes,
            content = content,
        )
    }
}

object ProstutiThemeExt {
    val colors: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
}
