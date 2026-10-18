package br.com.dojolb.graduacao.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun DojoLBGraduacaoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Force dark theme for Dojo LB branding
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme else lightColorScheme,
        typography = DojoTypography,
        content = content
    )
}

private val darkColorScheme = androidx.compose.material3.darkColorScheme(
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
)

private val lightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = RedPrimary,
    onPrimary = White,
    primaryContainer = RedLight,
    onPrimaryContainer = Black,
    secondary = RedDark,
    onSecondary = White,
    tertiary = WarningOrange,
    onTertiary = Black,
    error = ErrorRed,
    onError = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = LightGray,
    onSurfaceVariant = Black,
    outline = DividerGray,
    outlineVariant = LightGray,
    shadow = Black,
    surfaceTint = RedPrimary,
)

@Composable
fun DojoSurface(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = androidx.compose.material3.Shapes.medium,
        border = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        content = content
    )
}

// Extension to set status bar color
fun Activity.setupDojoWindow() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.isAppearanceLightStatusBars = false
    controller.isAppearanceLightNavigationBars = false
    window.statusBarColor = Black.toArgb()
    window.navigationBarColor = Black.toArgb()
}