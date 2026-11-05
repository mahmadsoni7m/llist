package tj.soni.listoffice.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UltraScheme = darkColorScheme(
    primary = Color(0xFF2F80FF),
    onPrimary = Color.White,
    secondary = Color(0xFF1FA45B),
    onSecondary = Color.White,
    background = Color(0xFF050B08),
    onBackground = Color(0xFFE6F0EA),
    surface = Color(0xFF0C1712),
    onSurface = Color(0xFFE6F0EA),
    surfaceVariant = Color(0xFF14231B),
    onSurfaceVariant = Color(0xFFB9CBC0),
    primaryContainer = Color(0xFF0F3D26),
    onPrimaryContainer = Color(0xFFD6F5E3),
    error = Color(0xFFFF6B6B)
)

@Composable
fun ListOfficeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = UltraScheme, content = content)
}
