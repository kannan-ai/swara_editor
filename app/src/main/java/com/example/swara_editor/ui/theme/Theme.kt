package com.example.swara_editor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.swara_editor.videoeditor.data.AppSettings
import com.example.swara_editor.videoeditor.data.AppTheme

private val OledColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF101010),
    secondaryContainer = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    secondaryContainer = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9ECEF),
    secondaryContainer = Color(0xFFE0E0E0),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun Swara_editorTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        settings.theme == AppTheme.LIGHT -> LightColorScheme
        settings.theme == AppTheme.OLED || (settings.theme == AppTheme.DARK && settings.isOledEnabled) -> OledColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
