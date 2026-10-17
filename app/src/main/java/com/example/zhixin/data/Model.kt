package com.example.zhixin.data

/**
 * 每日心情打卡。colorHex 用于日历色块和心情标签。
 * 只保留常见的十二种心情；key 一旦用过就不改，保证老数据还能对上。
 */
enum class Mood(
    val key: String,
    val label: String,
    val emoji: String,
    val colorHex: Long,
    val score: Int
) {
    // —— 圆形选择器里的 27 种，每个都对应一张手绘表情 ——
    HAPPY("happy", "开心", "\uD83D\uDE06", 0xFFFFB347, 9),
    LAUGH("laugh", "笑哭", "\uD83D\uDE02", 0xFFF9C74F, 9),
    HEART("heart", "心动", "\uD83D\uDE0D", 0xFFFFB6C1, 8),
    KISS("kiss", "亲亲", "\uD83D\uDE17", 0xFFFF9FB2, 8),
    PLAYFUL("playful", "俏皮", "\uD83D\uDE1B", 0xFFF7C948, 8),
    NAUGHTY("naughty", "调皮", "\uD83D\uDE1C", 0xFFF4B942, 8),
    PROUD("proud", "得意", "\uD83D\uDE0F", 0xFFD9B26F, 8),
    CONTENT("content", "满足", "\uD83D\uDE0A", 0xFFB8D8A8, 8),
    CALM("calm", "平静", "\uD83D\uDE0C", 0xFF90C695, 7),
    ANGEL("angel", "乖巧", "\uD83D\uDE07", 0xFFC9D6E8, 7),
    DEVIL("devil", "坏笑", "\uD83D\uDE08", 0xFFB08BBB, 6),
    COOL("cool", "淡定", "\uD83D\uDE0E", 0xFF9BB6D6, 7),
    SURPRISED("surprised", "惊讶", "\uD83D\uDE33", 0xFFFFC97A, 6),
    SHOCKED("shocked", "震惊", "\uD83D\uDE31", 0xFFFFA45C, 5),
    PANICKED("panicked", "慌张", "\uD83D\uDE30", 0xFFA8A0C0, 3),
    WRONGED("wronged", "委屈", "\uD83E\uDD72", 0xFFA9C4D6, 3),
    UPSET("upset", "难受", "\uD83D\uDE16", 0xFF9AA7B8, 3),
    ANGRY("angry", "生气", "\uD83D\uDE21", 0xFFE07A5F, 3),
    SULKY("sulky", "赌气", "\uD83D\uDE24", 0xFFE39A76, 3),
    FURIOUS("furious", "暴怒", "\uD83D\uDC7F", 0xFFD64545, 1),
    BLUE("blue", "低落", "\uD83D\uDE14", 0xFF8FA8C0, 4),
    TEARFUL("tearful", "想哭", "\uD83E\uDD79", 0xFF7FB3D1, 3),
    SAD("sad", "难过", "\uD83E\uDD7A", 0xFF87CEEB, 3),
    CRY("cry", "大哭", "\uD83D\uDE2D", 0xFF6FA8DC, 2),
    DIZZY("dizzy", "发晕", "\uD83D\uDE35", 0xFFB0A8C8, 3),
    HURT("hurt", "受伤", "\uD83E\uDD15", 0xFFC9A0A0, 3),
    SICK("sick", "生病", "\uD83E\uDD12", 0xFF90C695, 3);

    companion object {
        fun of(key: String?): Mood? = entries.firstOrNull { it.key == key }
    }
}

/** 打卡页 / 写记录时那种「一屏小网格」只放常用的十二种，不然太长。 */
val COMMON_MOODS: List<Mood> = listOf(
    Mood.HAPPY,
    Mood.LAUGH,
    Mood.HEART,
    Mood.CALM,
    Mood.CONTENT,
    Mood.PROUD,
    Mood.COOL,
    Mood.SURPRISED,
    Mood.SAD,
    Mood.CRY,
    Mood.ANGRY,
    Mood.SICK
)

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
    DIARY("diary", "日记本", "日记", "\uD83D\uDCDD", 0xFFB69DCD),
    MOOD("mood", "每日心情打卡", "心情", "\uD83D\uDE06", 0xFFF0A24C);

    companion object {
        fun of(key: String?): Category = entries.firstOrNull { it.key == key } ?: DAILY
    }
}

/** 自定义打卡项：用户可以自己添加「喝水 / 阅读 / 运动…」。 */
data class CustomItem(
    val emoji: String = "\uD83C\uDFAF",
    val name: String = "",
    /** 每天要完成的次数（喝水可以设 8 杯）。 */
    val dailyTarget: Int = 1,
    /** 一句激励自己的话，打卡页会显示。 */
    val note: String = "",
    /** 挑选的主题色下标（TASK_COLORS），-1 = 按列表顺序自动分配。 */
    val colorIndex: Int = -1,
    /** 难度：简单 / 中等 / 困难。 */
    val difficulty: String = "中等",
    /** 可打卡时间段 "HH:mm"，空 = 不限制。 */
    val windowStart: String = "",
    val windowEnd: String = "",
    /** 完成一次获得的努力值（1~10）。 */
    val reward: Int = 2,
    /** 任务分类：任意 / 起床 / 早晨 / 中午 / 下午 / 晚上 / 睡前 / 周末。 */
    val category: String = "任意"
) {
    /** 目标次数至少 1 次，最多 20 次。 */
    fun target(): Int = dailyTarget.coerceIn(1, 20)

    /** 努力值限制在 1~10。 */
    fun rewardValue(): Int = reward.coerceIn(1, 10)

    /** 是否设置了打卡时段。 */
    fun hasWindow(): Boolean = windowStart.isNotBlank() && windowEnd.isNotBlank()
}

/** 任务可选的难度。 */
val TASK_DIFFICULTIES = listOf("简单", "中等", "困难")

/** 任务可选的分类。 */
val TASK_CATEGORIES = listOf("任意", "起床", "早晨", "中午", "下午", "晚上", "睡前", "周末")

/** 难度对应的默认努力值。 */
fun difficultyReward(difficulty: String): Int = when (difficulty) {
    "简单" -> 1
    "困难" -> 3
    else -> 2
}

/** "HH:mm" → 当天分钟数；空或非法返回 null（表示不限制）。 */
fun parseHm(hhmm: String): Int? {
    if (hhmm.isBlank()) return null
    val parts = hhmm.split(":")
    if (parts.size != 2) return null
    val h = parts[0].trim().toIntOrNull() ?: return null
    val m = parts[1].trim().toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/** 现在是否在该任务的打卡时间段内（支持跨夜，比如 21:00 ~ 07:00）。 */
fun inCheckWindow(item: CustomItem, nowMillis: Long): Boolean {
    if (!item.hasWindow()) return true
    val start = parseHm(item.windowStart) ?: return true
    val end = parseHm(item.windowEnd) ?: return true
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
    val cur = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
        cal.get(java.util.Calendar.MINUTE)
    return if (start <= end) cur in start..end else cur >= start || cur <= end
}

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
    /**
     * 图片文件名列表（文件存在私有目录 filesDir/images/ 下）。
     * 声明为可空：Gson 反序列化老数据时没有该字段会是 null，
     * 读取一律走 [imageList]，避免 NPE。
     */
    val images: List<String>? = null,
    val deleted: Boolean = false
) {
    /** 安全读取图片列表（老数据没有 images 字段时返回空）。 */
    val imageList: List<String> get() = images.orEmpty()

    /** 补齐 id 与日期键，保证老数据 / 导入数据也能正常显示。 */
    fun normalize(): Entry {
        val fixedId = if (id.isBlank()) "e" + time.toString() + (0..9999).random() else id
        val fixedDate = if (dateKey.isBlank()) DateKey.fromMillis(time) else dateKey
        return copy(id = fixedId, dateKey = fixedDate)
    }
}
