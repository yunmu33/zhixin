package com.example.zhixin.data

/**
 * 一个等级：第几级、图标、名字、攒到多少累计热度就是这一级。
 */
data class LevelStep(
    val index: Int,
    val emoji: String,
    val name: String,
    /** 达到这一级需要的累计热度。 */
    val at: Int
)

/**
 * 打卡热度：
 * - 每完成一次打卡 +10 热度
 * - 连续第 N 天额外 +N×2（最多 +20）
 * - 热度累计决定等级，热力图按「当天热度」分 5 档着色
 */
object Heat {
    const val BASE = 10

    /** 等级阶梯：攒够 at 那么多累计热度就升到这一级。 */
    val LEVELS = listOf(
        LevelStep(0, "\uD83D\uDD6F️", "未点燃", 0),
        LevelStep(1, "\uD83C\uDF31", "微光", 1),
        LevelStep(2, "\uD83D\uDD25", "温热", 60),
        LevelStep(3, "\uD83D\uDD25", "火热", 200),
        LevelStep(4, "\uD83D\uDCA5", "炽热", 500),
        LevelStep(5, "\uD83C\uDF0B", "爆表", 1200)
    )

    fun bonus(streak: Int): Int = (streak * 2).coerceAtMost(20)

    private fun live(entries: List<Entry>): List<Entry> = entries.filter { !it.deleted }

    /** dateKey -> 到那天为止的连续打卡天数。 */
    fun streakByDay(entries: List<Entry>): Map<String, Int> {
        val days = live(entries).map { it.dateKey }.distinct().sorted()
        val map = LinkedHashMap<String, Int>()
        var streak = 0
        var prev: String? = null
        for (d in days) {
            streak = if (prev != null &&
                DateKey.daysBetween(DateKey.toMillis(prev), DateKey.toMillis(d)) == 1
            ) streak + 1 else 1
            map[d] = streak
            prev = d
        }
        return map
    }

    /** dateKey -> 当天获得的热度。 */
    fun heatByDay(entries: List<Entry>): Map<String, Int> {
        val sb = streakByDay(entries)
        return live(entries)
            .groupBy { it.dateKey }
            .mapValues { (k, list) -> list.size * BASE + bonus(sb[k] ?: 1) }
    }

    fun dayHeat(entries: List<Entry>, key: String): Int =
        heatByDay(entries)[key] ?: 0

    fun todayHeat(entries: List<Entry>): Int =
        dayHeat(entries, DateKey.fromMillis(System.currentTimeMillis()))

    /** 累计总热度。 */
    fun total(entries: List<Entry>): Int = heatByDay(entries).values.sum()

    fun monthHeat(entries: List<Entry>, year: Int, month: Int): Int {
        val prefix = String.format("%04d-%02d", year, month + 1)
        return heatByDay(entries).filterKeys { it.startsWith(prefix) }.values.sum()
    }

    /** 当天热度 -> 热力图档位 0..5。 */
    fun levelOf(heat: Int): Int = when {
        heat <= 0 -> 0
        heat < 20 -> 1
        heat < 35 -> 2
        heat < 50 -> 3
        heat < 70 -> 4
        else -> 5
    }

    /** 累计热度 -> 等级下标 0..5。 */
    fun levelIndex(total: Int): Int {
        var i = 0
        for (s in LEVELS) if (total >= s.at) i = s.index
        return i
    }

    /** 当前所在的那一阶。 */
    fun levelStep(total: Int): LevelStep = LEVELS[levelIndex(total)]

    fun levelName(total: Int): String = levelStep(total).name
    fun levelEmoji(total: Int): String = levelStep(total).emoji

    /** 再上一级是哪一阶；已经是最高的就返回 null。 */
    fun nextLevelStep(total: Int): LevelStep? = LEVELS.getOrNull(levelIndex(total) + 1)

    /** 当前等级的进度 0..1，以及升到下一级还差多少热度。 */
    fun progress(total: Int): Float {
        val step = levelStep(total)
        val next = nextLevelStep(total) ?: return 1f
        return ((total - step.at).toFloat() / (next.at - step.at)).coerceIn(0f, 1f)
    }

    fun nextLevelName(total: Int): String? = nextLevelStep(total)?.name

    fun needToNext(total: Int): Int {
        val next = nextLevelStep(total) ?: return 0
        return next.at - total
    }
}
