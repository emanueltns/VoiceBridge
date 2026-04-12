package freeapp.voicebridge.ai.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VBPrimary,
    secondary = VBSecondary,
    background = VBBackground,
    surface = VBSurface,
    error = VBError,
    onPrimary = VBBackground,
    onSecondary = VBBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = Color.White,
    secondaryContainer = VBSurface,
    onSecondaryContainer = TextPrimary,
    surfaceVariant = VBSurface,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
)

@Composable
fun VoiceBridgeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
