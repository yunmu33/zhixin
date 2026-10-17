package com.example.zhixin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.zhixin.data.AppSettings

/** 手账色板，供卡片 / 标签 / 图表直接取用。 */
val LocalAppColors = staticCompositionLocalOf { appColorsOf(SKINS.first(), false) }

@Composable
fun ZhixinTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (settings.darkMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = appColorsOf(skinOf(settings.skin), darkTheme)

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = Color(0xFF1C1714),
            secondary = colors.accent,
            onSecondary = Color(0xFF1C1714),
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.card,
            onSurface = colors.ink,
            surfaceVariant = colors.cardAlt,
            onSurfaceVariant = colors.inkSoft,
            outline = colors.line
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            secondary = colors.accent,
            onSecondary = Color.White,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.card,
            onSurface = colors.ink,
            surfaceVariant = colors.cardAlt,
            onSurfaceVariant = colors.inkSoft,
            outline = colors.line
        )
    }

    val density = LocalDensity.current
    val scaledDensity = Density(density.density, density.fontScale * settings.fontScale)

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
