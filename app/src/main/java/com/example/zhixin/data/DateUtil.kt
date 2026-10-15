package com.example.zhixin.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 只用 java.util.Calendar 做日期运算，避免引入 coreLibraryDesugaring。
 */
object DateKey {
    private const val DAY = 24 * 60 * 60 * 1000L
    private val FMT = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    fun fromMillis(millis: Long): String = FMT.format(Date(millis))

    fun toMillis(key: String): Long = try {
        FMT.parse(key)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(millis)
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    /** 相隔天数（按自然日算）。 */
    fun daysBetween(from: Long, to: Long): Int =
        ((startOfDay(to) - startOfDay(from)) / DAY).toInt()

    /** 周一 = 0 … 周日 = 6 */
    fun weekdayIndexMondayFirst(millis: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    /** 某年某月（month 为 0-11）的日历格子，固定 42 个，空位用 0 表示。 */
    fun monthCells(year: Int, month: Int): List<Long> {
        val first = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val start = first.timeInMillis - lead * DAY
        return List(42) { i -> start + i * DAY }
    }

    fun daysInMonth(year: Int, month: Int): Int =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

    fun yearOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }
        .get(Calendar.YEAR)

    fun monthOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }
        .get(Calendar.MONTH)

    fun dayOfMonth(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }
        .get(Calendar.DAY_OF_MONTH)

    /** 如：10月14日 */
    fun monthDayLabel(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日"
    }

    /** 如：21:30 */
    fun timeLabel(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(millis))

    fun weekdayLabel(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[c.get(Calendar.DAY_OF_WEEK) - 1]
    }
}
