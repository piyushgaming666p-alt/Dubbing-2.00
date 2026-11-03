package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BhashaStudioDarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = StudioTextPrimary,
    primaryContainer = StudioSurfaceVariant,
    onPrimaryContainer = PurpleGlow,
    secondary = PurpleSecondary,
    onSecondary = StudioBackground,
    secondaryContainer = StudioSurfaceCard,
    onSecondaryContainer = StudioTextPrimary,
    tertiary = AudioDubColor,
    onTertiary = StudioBackground,
    background = StudioBackground,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder,
    error = StudioError,
    onError = StudioTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force modern dark studio theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = BhashaStudioDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = StudioBackground.toArgb()
                it.navigationBarColor = StudioBackground.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
