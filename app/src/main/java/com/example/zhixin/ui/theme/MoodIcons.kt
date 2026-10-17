package com.example.zhixin.ui.theme

import com.example.zhixin.R
import com.example.zhixin.data.Mood

/**
 * 心情圆形选择器里的一颗小表情：一张手绘图 + 它代表的心情。
 * 27 颗图一一对应 27 种心情，每颗都是一种不同的情绪。
 */
data class MoodSticker(val res: Int, val mood: Mood)

/** 全部 27 颗手绘表情，按「内圈 11 颗 + 外圈 16 颗」两层排列。 */
val moodStickers: List<MoodSticker> = listOf(
    // —— 内圈：开心这一挂 ——
    MoodSticker(R.drawable.st_smile_big, Mood.HAPPY),      // 开心大笑
    MoodSticker(R.drawable.st_laugh, Mood.LAUGH),          // 笑哭
    MoodSticker(R.drawable.st_heart_eyes, Mood.HEART),     // 爱心眼 → 心动
    MoodSticker(R.drawable.st_kiss, Mood.KISS),            // 亲亲
    MoodSticker(R.drawable.st_tongue, Mood.PLAYFUL),       // 吐舌 → 俏皮
    MoodSticker(R.drawable.st_wink, Mood.NAUGHTY),         // 眨眼 → 调皮
    MoodSticker(R.drawable.st_smile_proud, Mood.PROUD),    // 得意
    MoodSticker(R.drawable.st_smile_soft, Mood.CONTENT),   // 满足
    MoodSticker(R.drawable.st_smile, Mood.CALM),           // 平静
    MoodSticker(R.drawable.st_angel, Mood.ANGEL),          // 天使 → 乖巧
    MoodSticker(R.drawable.st_devil, Mood.DEVIL),          // 小恶魔 → 坏笑
    // —— 外圈：其余情绪 ——
    MoodSticker(R.drawable.st_cool, Mood.COOL),            // 墨镜 → 淡定
    MoodSticker(R.drawable.st_surprise_wide, Mood.SURPRISED),// 瞪眼 → 惊讶
    MoodSticker(R.drawable.st_surprise_o, Mood.SHOCKED),   // o 嘴 → 震惊
    MoodSticker(R.drawable.st_sweat, Mood.PANICKED),       // 冒冷汗 → 慌张
    MoodSticker(R.drawable.st_pout, Mood.WRONGED),         // 撇嘴 → 委屈
    MoodSticker(R.drawable.st_gross, Mood.UPSET),          // 波浪嘴 → 难受
    MoodSticker(R.drawable.st_angry, Mood.ANGRY),          // 皱眉 → 生气
    MoodSticker(R.drawable.st_angry_orange, Mood.SULKY),   // 橙脸 → 赌气
    MoodSticker(R.drawable.st_furious, Mood.FURIOUS),      // 红脸 → 暴怒
    MoodSticker(R.drawable.st_blue_sad, Mood.BLUE),        // 蓝脸 → 低落
    MoodSticker(R.drawable.st_blue_tear, Mood.TEARFUL),    // 蓝脸掉泪 → 想哭
    MoodSticker(R.drawable.st_sad, Mood.SAD),              // 闭眼难过
    MoodSticker(R.drawable.st_cry, Mood.CRY),              // 大哭
    MoodSticker(R.drawable.st_dizzy, Mood.DIZZY),          // X 眼 → 发晕
    MoodSticker(R.drawable.st_bandage, Mood.HURT),         // 缠绷带 → 受伤
    MoodSticker(R.drawable.st_green_sick, Mood.SICK)       // 绿脸 → 生病
)

/** 这个心情对应哪张手绘表情；日历这些小地方也用它显示。 */
fun stickerOf(mood: Mood): Int? = moodStickers.firstOrNull { it.mood == mood }?.res
