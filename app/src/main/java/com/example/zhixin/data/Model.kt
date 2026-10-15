package com.example.zhixin.data

/**
 * 五种一键打卡心情。colorHex 会同时用于日历色块、年度热力图和收藏本标签。
 */
enum class Mood(
    val key: String,
    val label: String,
    val emoji: String,
    val colorHex: Long,
    val score: Int
) {
    JOY("joy", "超开心", "\uD83D\uDE04", 0xFFF0A24C, 9),
    CALM("calm", "稳稳的", "\uD83C\uDF3F", 0xFF86B98D, 7),
    DOWN("down", "有点闷", "\uD83C\uDF27", 0xFF9BB6D6, 4),
    ANGRY("angry", "气呼呼", "\uD83D\uDD25", 0xFFE68A7A, 3),
    SLEEPY("sleepy", "困困的", "\uD83D\uDE2A", 0xFFB3A2CB, 5);

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
    DAILY("daily", "平平淡淡的小日常", "小日常", "☁", 0xFFB0CDA6);

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
