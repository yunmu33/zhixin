package com.example.zhixin.data

/**
 * 每日心情打卡。colorHex 用于日历色块和心情标签。
 */
enum class Mood(
    val key: String,
    val label: String,
    val emoji: String,
    val colorHex: Long,
    val score: Int
) {
    HAPPY("happy", "开心", "\uD83D\uDE0A", 0xFFFFB347, 8),
    CALM("calm", "平静", "\uD83D\uDE0C", 0xFF90C695, 7),
    EXCITED("excited", "兴奋", "\uD83E\uDD29", 0xFFFFD93D, 9),
    HEART("heart", "心动", "\uD83E\uDD70", 0xFFFFB6C1, 8),
    TIRED("tired", "心累", "\uD83D\uDE2B", 0xFFC4A77D, 4),
    SAD("sad", "伤心", "\uD83D\uDE22", 0xFF87CEEB, 3),
    ANGRY("angry", "生气", "\uD83D\uDE20", 0xFFE07A5F, 2),
    ANXIOUS("anxious", "烦躁", "\uD83D\uDE24", 0xFF8E9AAF, 3);

    companion object {
        fun of(key: String?): Mood? = entries.firstOrNull { it.key == key }
    }
}

/**
 * 记录的三个抽屉：闷闷的小故事 / 小美好 / 小日常。
 */
enum class Category(
    val key: String,
    val label: String,
    val short: String,
    val emoji: String,
    val colorHex: Long
) {
    NEW("new", "今天的新收获", "新收获", "\uD83C\uDF31", 0xFF7FBFA8),
    STORY("story", "闷闷的小故事", "闷闷", "\uD83C\uDF27", 0xFF9BB6D6),
    GOOD("good", "闪闪发光的小美好", "小美好", "✨", 0xFFF2C55C),
    DAILY("daily", "平平淡淡的小日常", "小日常", "☁", 0xFFB0CDA6),
    MOOD("mood", "每日心情打卡", "心情", "\uD83D\uDE0A", 0xFFF0A24C);

    companion object {
        fun of(key: String?): Category = entries.firstOrNull { it.key == key } ?: DAILY
    }
}

/** 自定义打卡项：用户可以自己添加「喝水 / 阅读 / 运动…」。 */
data class CustomItem(
    val emoji: String = "\uD83C\uDFAF",
    val name: String = ""
)

/** 自定义打卡产生的记录会带上这个标签，方便区分。 */
const val CUSTOM_TAG = "自定义"

/**
 * 一条打卡 / 故事记录。全部字段都存在本机。
 */
data class Entry(
    val id: String = "",
    val time: Long = System.currentTimeMillis(),
    val dateKey: String = "",
    val mood: String? = null,
    val score: Int = 5,
    val category: String = Category.DAILY.key,
    val text: String = "",
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val deleted: Boolean = false
) {
    /** 补齐 id 与日期键，保证老数据 / 导入数据也能正常显示。 */
    fun normalize(): Entry {
        val fixedId = if (id.isBlank()) "e" + time.toString() + (0..9999).random() else id
        val fixedDate = if (dateKey.isBlank()) DateKey.fromMillis(time) else dateKey
        return copy(id = fixedId, dateKey = fixedDate)
    }
}
