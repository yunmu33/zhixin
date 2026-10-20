package com.example.zhixin.data

import java.util.Calendar

/**
 * 记录提醒的循环方式：除了每天，还能只在工作日 / 周末 / 每周某一天提醒。
 */
enum class RemindRepeat(val key: String, val label: String, val emoji: String, val hint: String) {
    DAILY("daily", "每天", "\uD83D\uDD01", "每天都来叫你"),
    WEEKDAY("weekday", "工作日", "\uD83D\uDCBC", "周一到周五才提醒，周末不打扰"),
    WEEKEND("weekend", "周末", "\uD83C\uDF34", "只在周六周日提醒"),
    WEEKLY("weekly", "每周", "\uD83D\uDCC5", "只在你挑的那天提醒");

    companion object {
        fun of(key: String?): RemindRepeat =
            entries.firstOrNull { it.key == key } ?: DAILY

        /** 设置页滚轮里的顺序。 */
        val ALL: List<RemindRepeat> = entries.toList()
    }
}

/** 星期（周一到周日）的中文写法，滚轮里按顺序排。 */
val WEEKDAY_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** 和 WEEKDAY_LABELS 一一对应的 Calendar 星期值。 */
val WEEKDAY_KEYS = listOf(
    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
)

/** 当前选的是星期几在列表里的下标（老数据越界时退回周一）。 */
fun weekdayIndex(weekday: Int): Int = WEEKDAY_KEYS.indexOf(weekday).coerceAtLeast(0)

/** 这一天（Calendar 的 DAY_OF_WEEK）要不要提醒。 */
private fun RemindRepeat.hits(dayOfWeek: Int, weekday: Int): Boolean = when (this) {
    RemindRepeat.DAILY -> true
    RemindRepeat.WEEKDAY -> dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
    RemindRepeat.WEEKEND -> dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    RemindRepeat.WEEKLY -> dayOfWeek == weekday
}

/** 这一天要不要提醒（自检时用）。 */
fun RemindRepeat.matches(cal: Calendar, weekday: Int): Boolean =
    hits(cal.get(Calendar.DAY_OF_WEEK), weekday)

/**
 * 下一次该响的时刻：从今天起按循环规则往后找第一个「该提醒」的日子，
 * 当天时间已经过去了就顺延到下一天（最多找 8 天，规则不对也不至于死循环）。
 */
fun nextReminderTime(
    hour: Int,
    minute: Int,
    repeatKey: String,
    weekday: Int,
    nowMillis: Long = System.currentTimeMillis()
): Calendar {
    val repeat = RemindRepeat.of(repeatKey)
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    repeat(8) {
        val ok = repeat.hits(cal.get(Calendar.DAY_OF_WEEK), weekday) &&
            cal.timeInMillis > nowMillis
        if (ok) return cal
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return cal
}
