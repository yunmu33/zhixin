package com.example.zhixin.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.zhixin.data.Mood

/**
 * 奶油手账系皮肤：每套皮肤自带浅色 / 深色两套底色。
 */
data class SkinPalette(
    val key: String,
    val name: String,
    val emoji: String,
    val bg: Color,
    val card: Color,
    val ink: Color,
    val accent: Color,
    val bgDark: Color,
    val cardDark: Color,
    val inkDark: Color,
    val accentDark: Color
)

val SKINS = listOf(
    SkinPalette(
        "cream", "奶油", "\uD83E\uDD5C",
        Color(0xFFFFF5E8), Color(0xFFFFFDF7), Color(0xFF6B5847), Color(0xFFE2955F),
        Color(0xFF272120), Color(0xFF332B29), Color(0xFFF1E4D6), Color(0xFFE8A46F)
    ),
    SkinPalette(
        "matcha", "抹茶", "\uD83C\uDF75",
        Color(0xFFF0F6EA), Color(0xFFFCFEF8), Color(0xFF4C5A46), Color(0xFF79A96F),
        Color(0xFF21271F), Color(0xFF2B3229), Color(0xFFE3EBDC), Color(0xFF8FBC85)
    ),
    SkinPalette(
        "sky", "天空", "\uD83C\uDF0A",
        Color(0xFFEDF4FA), Color(0xFFFAFCFF), Color(0xFF44586A), Color(0xFF6FA6CC),
        Color(0xFF1F262C), Color(0xFF28313A), Color(0xFFDEE8F1), Color(0xFF84B6DA)
    ),
    SkinPalette(
        "lavender", "丁香", "\uD83D\uDC9C",
        Color(0xFFF5F0FA), Color(0xFFFCF9FF), Color(0xFF554A68), Color(0xFFA28CC8),
        Color(0xFF241F2C), Color(0xFF2E2838), Color(0xFFEAE1F6), Color(0xFFB49DD8)
    ),
    SkinPalette(
        "peach", "蜜桃", "\uD83C\uDF51",
        Color(0xFFFFF1EE), Color(0xFFFFFAF9), Color(0xFF6C4C48), Color(0xFFEB9A90),
        Color(0xFF2B2120), Color(0xFF352A28), Color(0xFFF6E3DE), Color(0xFFEFAAA0)
    )
)

fun skinOf(key: String): SkinPalette = SKINS.firstOrNull { it.key == key } ?: SKINS.first()

/** 界面里除 Material 色板之外还要用到的「手账色」。 */
data class AppColors(
    val bg: Color,
    val card: Color,
    val cardAlt: Color,
    val ink: Color,
    val inkSoft: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color
)

fun appColorsOf(skin: SkinPalette, dark: Boolean): AppColors {
    val bg = if (dark) skin.bgDark else skin.bg
    val card = if (dark) skin.cardDark else skin.card
    val ink = if (dark) skin.inkDark else skin.ink
    val accent = if (dark) skin.accentDark else skin.accent
    return AppColors(
        bg = bg,
        card = card,
        cardAlt = if (dark) card.copy(alpha = 0.6f) else card.copy(alpha = 0.72f),
        ink = ink,
        inkSoft = ink.copy(alpha = 0.62f),
        line = ink.copy(alpha = 0.14f),
        accent = accent,
        accentSoft = accent.copy(alpha = 0.16f)
    )
}

fun moodColorOf(key: String?): Color =
    Mood.of(key)?.let { Color(it.colorHex) } ?: Color.Transparent

fun moodColor(mood: Mood?): Color =
    mood?.let { Color(it.colorHex) } ?: Color.Transparent

fun categoryColor(key: String): Color = when (key) {
    "new" -> Color(0xFF7FBFA8)
    "story" -> Color(0xFF9BB6D6)
    "good" -> Color(0xFFF2C55C)
    else -> Color(0xFFB0CDA6)
}

/** 打卡热度 0-5 档的颜色，档位越高越“烫”。 */
fun heatColor(level: Int): Color = when (level) {
    1 -> Color(0xFFFFE2C4)
    2 -> Color(0xFFFFC08A)
    3 -> Color(0xFFFF9C55)
    4 -> Color(0xFFF4733A)
    5 -> Color(0xFFD94E1C)
    else -> Color(0xFFE9E1D9)
}
