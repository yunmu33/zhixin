package com.example.zhixin.data

/**
 * 打卡热度：
 * - 每完成一次打卡 +10 热度
 * - 连续第 N 天额外 +N×2（最多 +20）
 * - 热度累计决定等级，热力图按「当天热度」分 5 档着色
 */
object Heat {
    const val BASE = 10

    private val LEVEL_AT = listOf(0, 1, 60, 200, 500, 1200)
    private val LEVEL_NAME = listOf("未点燃", "微光", "温热", "火热", "炽热", "爆表")
    private val LEVEL_EMOJI = listOf("\uD83D\uDD6F️", "\uD83C\uDF31", "🔥", "\uD83D\uDD25", "💥", "\uD83C\uDF0B")

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
        for (idx in LEVEL_AT.indices) if (total >= LEVEL_AT[idx]) i = idx
        return i
    }

    fun levelName(total: Int): String = LEVEL_NAME[levelIndex(total)]
    fun levelEmoji(total: Int): String = LEVEL_EMOJI[levelIndex(total)]

    /** 当前等级的进度 0..1，以及升到下一级还差多少热度。 */
    fun progress(total: Int): Float {
        val i = levelIndex(total)
        if (i >= LEVEL_AT.size - 1) return 1f
        val from = LEVEL_AT[i]
        val to = LEVEL_AT[i + 1]
        return ((total - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    }

    fun nextLevelName(total: Int): String? {
        val i = levelIndex(total)
        return if (i >= LEVEL_NAME.size - 1) null else LEVEL_NAME[i + 1]
    }

    fun needToNext(total: Int): Int {
        val i = levelIndex(total)
        return if (i >= LEVEL_AT.size - 1) 0 else LEVEL_AT[i + 1] - total
    }
}
