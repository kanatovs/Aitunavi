package com.aitu.navigator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClassicColors = lightColorScheme(
    primary = Color(0xFF3B82F6),
    secondary = Color(0xFF64748B),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFF3F8),
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

private val VioletColors = darkColorScheme(
    primary = Color(0xFFA855F7),
    secondary = Color(0xFFD8B4FE),
    background = Color(0xFF140A1F),
    surface = Color(0xFF1B1029),
    surfaceVariant = Color(0xFF26153B),
    onPrimary = Color.White,
    onBackground = Color(0xFFF5EFFF),
    onSurface = Color(0xFFF5EFFF),
    onSurfaceVariant = Color(0xFFD8B4FE)
)

private val ObsidianColors = darkColorScheme(
    primary = Color(0xFFB8C0CC),
    secondary = Color(0xFF8B95A5),
    background = Color(0xFF0B0D10),
    surface = Color(0xFF111418),
    surfaceVariant = Color(0xFF171C22),
    onPrimary = Color(0xFF0B0D10),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFFB8C0CC)
)

private val NebulaColors = darkColorScheme(
    primary = Color(0xFF7C3AED),
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF090B1A),
    surface = Color(0xFF11152A),
    surfaceVariant = Color(0xFF1A2040),
    onPrimary = Color.White,
    onBackground = Color(0xFFEAF2FF),
    onSurface = Color(0xFFEAF2FF),
    onSurfaceVariant = Color(0xFFA5B4FC)
)

private val RoseNeonColors = darkColorScheme(
    primary = Color(0xFFFF4FD8),
    secondary = Color(0xFFFF8AE2),
    background = Color(0xFF160914),
    surface = Color(0xFF221021),
    surfaceVariant = Color(0xFF32152F),
    onPrimary = Color.White,
    onBackground = Color(0xFFFFEFFA),
    onSurface = Color(0xFFFFEFFA),
    onSurfaceVariant = Color(0xFFFFB3E9)
)

private val SunsetColors = darkColorScheme(
    primary = Color(0xFFFF7A18),
    secondary = Color(0xFFFFB347),
    background = Color(0xFF1B0E08),
    surface = Color(0xFF2A1610),
    surfaceVariant = Color(0xFF3A2017),
    onPrimary = Color.White,
    onBackground = Color(0xFFFFF3E8),
    onSurface = Color(0xFFFFF3E8),
    onSurfaceVariant = Color(0xFFFFC58C)
)

private val MonoColors = lightColorScheme(
    primary = Color(0xFF111111),
    secondary = Color(0xFF4B5563),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7E7E7),
    onPrimary = Color.White,
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF525252)
)

private val CyberMintColors = darkColorScheme(
    primary = Color(0xFF2EF2C3),
    secondary = Color(0xFF7FFFD4),
    background = Color(0xFF071512),
    surface = Color(0xFF0E211C),
    surfaceVariant = Color(0xFF143029),
    onPrimary = Color(0xFF05211A),
    onBackground = Color(0xFFE9FFF8),
    onSurface = Color(0xFFE9FFF8),
    onSurfaceVariant = Color(0xFF9CF7DA)
)

private val DeepSpaceColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF030712),
    surface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFF111827),
    onPrimary = Color.White,
    onBackground = Color(0xFFE6F0FF),
    onSurface = Color(0xFFE6F0FF),
    onSurfaceVariant = Color(0xFF93C5FD)
)

private val SilverColors = lightColorScheme(
    primary = Color(0xFF8E9AA8),
    secondary = Color(0xFFB0BAC5),
    background = Color(0xFFF4F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3E8ED),
    onPrimary = Color.White,
    onBackground = Color(0xFF1F2937),
    onSurface = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF6B7280)
)

private val GrayColors = lightColorScheme(
    primary = Color(0xFF6B7280),
    secondary = Color(0xFF9CA3AF),
    background = Color(0xFFF3F4F6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E7EB),
    onPrimary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF6B7280)
)

private val BlackColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFF9CA3AF),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF141414),
    onPrimary = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFBDBDBD)
)

@Composable
fun AITUStudentNavigatorTheme(
    themeName: String = "classic",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "classic" -> ClassicColors
        "violet" -> VioletColors
        "obsidian" -> ObsidianColors
        "nebula" -> NebulaColors
        "rose_neon" -> RoseNeonColors
        "sunset" -> SunsetColors
        "mono" -> MonoColors
        "cyber_mint" -> CyberMintColors
        "deep_space" -> DeepSpaceColors
        "silver" -> SilverColors
        "gray" -> GrayColors
        "black" -> BlackColors
        else -> if (isSystemInDarkTheme()) BlackColors else ClassicColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}