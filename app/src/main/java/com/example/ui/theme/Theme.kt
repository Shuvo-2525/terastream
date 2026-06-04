package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

// Premium, immersive deep space slate cinematic dark color palette
private val DarkThemeColorScheme = darkColorScheme(
    primary = Color(0xFF7C3AED),        // Royal Violet Accent
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1065), // Deep Violet Container
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFF06B6D4),      // Neon Cyan Accent
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFF0B0B0F),     // Immersive UI space black
    onBackground = Color(0xFFF1F5F9),   // slate-100
    surface = Color(0xFF1A1B23),        // Immersive UI surface cells
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF16171E),  // Bottom navigation background
    onSurfaceVariant = Color(0xFF94A3B8), // slate-400
    outline = Color(0xFF2E303E),         // Border white/10%
    error = Color(0xFFEF4444)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkThemeColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Extension modifier that applies fluid, cinematic background glows (Indigo and Cyan circular gradients)
 * mimicking the "Immersive UI" design theme specification.
 */
fun Modifier.immersiveGlowBackground(): Modifier = this.drawBehind {
    // Top-left glow - deep space violet
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x1F7C3AED), Color.Transparent),
            center = Offset(0f, 0f),
            radius = size.width * 0.9f
        ),
        radius = size.width * 0.9f,
        center = Offset(0f, 0f)
    )
    // Middle-right glow - premium luminous cyan neon
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x1606B6D4), Color.Transparent),
            center = Offset(size.width, size.height * 0.5f),
            radius = size.width * 0.8f
        ),
        radius = size.width * 0.8f,
        center = Offset(size.width, size.height * 0.5f)
    )
}
