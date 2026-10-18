package br.com.dojolb.graduacao.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dojo LB Color Palette
val Black = Color(0xFF000000)
val RedPrimary = Color(0xFFC62828)
val RedDark = Color(0xFFB71C1C)
val RedLight = Color(0xFFEF5350)
val White = Color(0xFFFFFFFF)
val DarkGray = Color(0xFF1E1E1E)
val MediumGray = Color(0xFF2D2D2D)
val LightGray = Color(0xFF424242)
val DividerGray = Color(0xFF333333)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)
val TextMuted = Color(0xFF757575)
val SuccessGreen = Color(0xFF2E7D32)
val SuccessLight = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFC62828)
val WarningOrange = Color(0xFFF57C00)
val SurfaceVariant = Color(0xFF252525)
val OnSurfaceVariant = Color(0xFFE0E0E0)

@Composable
fun DojoColors(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = RedPrimary,
            onPrimary = White,
            primaryContainer = RedDark,
            onPrimaryContainer = White,
            secondary = RedLight,
            onSecondary = Black,
            tertiary = WarningOrange,
            onTertiary = Black,
            error = ErrorRed,
            onError = White,
            background = Black,
            onBackground = White,
            surface = DarkGray,
            onSurface = White,
            surfaceVariant = MediumGray,
            onSurfaceVariant = OnSurfaceVariant,
            outline = DividerGray,
            outlineVariant = LightGray,
            shadow = Black,
            surfaceTint = RedPrimary,
        ),
        content = content
    )
}