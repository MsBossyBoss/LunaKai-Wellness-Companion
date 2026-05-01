package com.fancie.aicompanion.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEFA3C8),
    onPrimary = Color(0xFF2D2433),
    primaryContainer = Color(0xFF5B3752),
    onPrimaryContainer = Color(0xFFFFF1F7),
    secondary = Color(0xFFC8A8FF),
    tertiary = Color(0xFFE8C77A),
    background = Color(0xFF1C1622),
    onBackground = Color(0xFFFFFBFE),
    surface = Color(0xFF251D2D),
    onSurface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFF3A3042),
    onSurfaceVariant = Color(0xFFE9DDF0),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD96FA6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF1F7),
    onPrimaryContainer = Color(0xFF2D2433),
    secondary = Color(0xFF9D7BEA),
    onSecondary = Color.White,
    tertiary = Color(0xFFE8C77A),
    onTertiary = Color(0xFF2D2433),
    background = Color(0xFFFFF1F7),
    onBackground = Color(0xFF2D2433),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF2D2433),
    surfaceVariant = Color(0xFFF7E8FF),
    onSurfaceVariant = Color(0xFF7E7187),
)

@Composable
fun FancieAICompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
