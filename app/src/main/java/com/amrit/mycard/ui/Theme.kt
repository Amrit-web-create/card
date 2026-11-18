package com.amrit.mycard.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val AccentColors = listOf(
    Color(0xFF7C4DFF),
    Color(0xFF0091EA),
    Color(0xFF00BFA5),
    Color(0xFFFF6E40),
    Color(0xFFF50057),
    Color(0xFFFFB300),
    Color(0xFF111111)
)
val AccentNames = listOf("Violet", "Ocean", "Emerald", "Sunset", "Rose", "Gold", "Black")

val TileColors = listOf(
    Color(0xFF7C4DFF),
    Color(0xFF0091EA),
    Color(0xFF00BFA5),
    Color(0xFFFF6E40),
    Color(0xFFF50057),
    Color(0xFFFFB300),
    Color(0xFF26C6DA),
    Color(0xFF8D6E63)
)

@Composable
fun resolveDark(themeMode: Int): Boolean = when (themeMode) {
    1 -> false
    2 -> true
    3 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun MyCardTheme(dark: Boolean, black: Boolean, accentChoice: Color, content: @Composable () -> Unit) {
    // A black accent would vanish on dark backgrounds, so it turns into soft white there.
    val accent = if (dark && accentChoice.luminance() < 0.08f) Color(0xFFF2F2F2) else accentChoice
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val scheme = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = accent,
            background = if (black) Color(0xFF000000) else Color(0xFF0B0D14),
            onBackground = Color(0xFFF1F2F8),
            surface = if (black) Color(0xFF0C0C0E) else Color(0xFF151823),
            onSurface = Color(0xFFF1F2F8),
            surfaceVariant = if (black) Color(0xFF18191D) else Color(0xFF1E2230),
            onSurfaceVariant = Color(0xFF9AA0B8),
            outline = if (black) Color(0xFF34363D) else Color(0xFF3A3F55),
            error = Color(0xFFFF5470),
            surfaceTint = Color.Transparent
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = accent,
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF0F1115),
            surface = Color(0xFFF4F5F7),
            onSurface = Color(0xFF0F1115),
            surfaceVariant = Color(0xFFE8EAEE),
            onSurfaceVariant = Color(0xFF5F6472),
            outline = Color(0xFFD3D6DD),
            error = Color(0xFFD32F4A),
            surfaceTint = Color.Transparent
        )
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
