package com.example.zhixin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.Mood
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.heatColor
import com.example.zhixin.ui.theme.moodColorOf
import com.example.zhixin.ui.theme.stickerOf

/** 近 N 天情绪折线（1-10 分）。 */
@Composable
fun TrendChart(
    scores: List<Float?>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val line = LocalAppColors.current.line
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val w = size.width
        val h = size.height
        val pad = 14.dp.toPx()
        val bottom = h - pad
        val top = pad

        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3f
            drawLine(
                color = line,
                start = Offset(pad, y),
                end = Offset(w - pad, y),
                strokeWidth = 1f
            )
        }

        val steps = (scores.size - 1).coerceAtLeast(1)
        val pts = scores.mapIndexedNotNull { i, v ->
            if (v == null) null else Offset(
                x = pad + (w - 2 * pad) * (i.toFloat() / steps),
                y = bottom - (bottom - top) * ((v - 1f) / 9f).coerceIn(0f, 1f)
            )
        }

        if (pts.size >= 2) {
            val path = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        val dot = 3.6.dp.toPx()
        pts.forEach {
            drawCircle(color = color, radius = dot, center = it)
            drawCircle(color = Color.White, radius = dot * 0.45f, center = it)
        }
    }
}

/** 心情分布横向条形图。 */
@Composable
fun MoodBars(entries: List<Entry>, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val total = entries.size.coerceAtLeast(1)
    // 心情种类多，只列出记过的、按次数从多到少，最多十条
    val rows = remember(entries) {
        Mood.entries
            .map { mood -> mood to entries.count { it.mood == mood.key } }
            .filter { (_, count) -> count > 0 }
            .sortedByDescending { (_, count) -> count }
            .take(10)
    }
    if (rows.isEmpty()) {
        Text(
            "还没有记过心情，选一张表情试试 ✨",
            style = MaterialTheme.typography.labelMedium,
            color = c.inkSoft
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            val mood = row.first
            val count = row.second
            val ratio = count.toFloat() / total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val res = stickerOf(mood)
                if (res != null) {
                    Image(
                        painter = painterResource(res),
                        contentDescription = mood.label,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                } else {
                    Text(mood.emoji, fontSize = 15.sp, modifier = Modifier.width(24.dp))
                }
                Text(
                    mood.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.ink,
                    modifier = Modifier.width(56.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.line.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceAtLeast(0.02f))
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(moodColorOf(mood.key))
                    )
                }
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft,
                    modifier = Modifier.width(28.dp)
                )
            }
        }
    }
}

enum class HeatMode(val label: String, val emoji: String) {
    DAY("日度", "\uD83D\uDCC5"),
    WEEK("周度", "\uD83D\uDDD3"),
    MONTH("月度", "\uD83D\uDCC6"),
    YEAR("年度", "\uD83C\uDF0C")
}

/**
 * 打卡热度图：支持日 / 周 / 月 / 年四种视图。
 * - 日度：最近 14 天，每天一根柱
 * - 周度：最近 12 周，按周汇总
 * - 月度：所选年份 12 个月的色块（默认视图）
 * - 年度：有记录以来每一年
 */
@Composable
fun HeatMapCard(
    heatByDay: Map<String, Int>,
    mode: HeatMode,
    onModeChange: (HeatMode) -> Unit,
    year: Int,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    val now = System.currentTimeMillis()
    val curYear = DateKey.yearOf(now)
    val curMonth = DateKey.monthOf(now)

    // 最近 14 天
    val dayItems = (13 downTo 0).map { i ->
        val m = DateKey.addDays(now, -i)
        val key = DateKey.fromMillis(m)
        "${DateKey.monthOf(m) + 1}/${DateKey.dayOfMonth(m)}" to (heatByDay[key] ?: 0)
    }
    // 最近 12 周（周一为首）
    val lead = DateKey.weekdayIndexMondayFirst(now)
    val weekItems = (11 downTo 0).map { w ->
        val start = DateKey.addDays(now, -(lead + w * 7))
        val heat = (0..6).sumOf { d ->
            heatByDay[DateKey.fromMillis(DateKey.addDays(start, d))] ?: 0
        }
        "${DateKey.monthOf(start) + 1}/${DateKey.dayOfMonth(start)}" to heat
    }
    // 所选年份 12 个月
    val monthItems = (0..11).map { m ->
        val prefix = String.format("%04d-%02d", year, m + 1)
        "${m + 1}月" to heatByDay.filterKeys { it.startsWith(prefix) }.values.sum()
    }
    // 有记录以来每一年
    val firstYear = heatByDay.keys.mapNotNull { it.take(4).toIntOrNull() }.minOrNull() ?: curYear
    val yearItems = (firstYear..curYear).map { y ->
        "$y" to heatByDay.filterKeys { it.startsWith("$y-") }.values.sum()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeatMode.entries.forEach { m ->
                HeatModeChip(
                    label = "${m.emoji} ${m.label}",
                    selected = m == mode,
                    modifier = Modifier.weight(1f)
                ) { onModeChange(m) }
            }
        }
        Spacer(Modifier.height(12.dp))

        when (mode) {
            HeatMode.DAY -> {
                HeatSummary("最近 14 天", dayItems.sumOf { it.second })
                HeatBars(items = dayItems)
            }

            HeatMode.WEEK -> {
                HeatSummary("最近 12 周（周一算起）", weekItems.sumOf { it.second })
                HeatBars(items = weekItems)
            }

            HeatMode.MONTH -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.TextButton(onClick = { onYearChange(year - 1) }) {
                        Text("‹", style = MaterialTheme.typography.titleLarge, color = c.accent)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${year} 年", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Text(
                            "全年 ${monthItems.sumOf { it.second }} 热度",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = { onYearChange(year + 1) }) {
                        Text("›", style = MaterialTheme.typography.titleLarge, color = c.accent)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val maxMonth = monthItems.maxOf { it.second }.coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    monthItems.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, heat) ->
                                val isNow = year == curYear && label == "${curMonth + 1}月"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (heat <= 0) c.line.copy(alpha = 0.45f)
                                            else c.accent.copy(
                                                alpha = (0.24f + 0.66f * (heat.toFloat() / maxMonth))
                                                    .coerceIn(0.24f, 0.92f)
                                            )
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isNow) c.accent else c.line,
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (heat > 0) c.ink else c.inkSoft
                                        )
                                        Text(
                                            if (heat > 0) "$heat" else "—",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = c.inkSoft
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HeatMode.YEAR -> {
                HeatSummary("共 ${yearItems.size} 年", yearItems.sumOf { it.second })
                HeatBars(items = yearItems)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("少", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
            Spacer(Modifier.width(6.dp))
            listOf(0, 1, 2, 3, 4, 5).forEach { lv ->
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (lv == 0) c.line.copy(alpha = 0.5f) else heatColor(lv))
                )
                Spacer(Modifier.width(3.dp))
            }
            Text("多", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
            Spacer(Modifier.width(12.dp))
            Text(
                "热度 = 打卡次数×10 + 连续加成",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
        }
    }
}

@Composable
private fun HeatSummary(title: String, heat: Int) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink)
        Spacer(Modifier.width(8.dp))
        Text(
            "$heat 热度",
            style = MaterialTheme.typography.labelMedium,
            color = c.accent
        )
    }
}

/** 柱状热度图：每项一根柱，颜色按当天热度分档。 */
@Composable
private fun HeatBars(items: List<Pair<String, Int>>) {
    val c = LocalAppColors.current
    val max = items.maxOf { it.second }.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        items.forEach { (label, heat) ->
            val h = if (heat <= 0) 6.dp else (10 + (heat.toFloat() / max) * 58f).dp
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (heat > 0) {
                    Text(
                        "$heat",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft,
                        fontSize = 8.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(h)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (heat <= 0) c.line.copy(alpha = 0.5f)
                            else heatColor(Heat.levelOf(heat))
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft,
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HeatModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(if (selected) c.accentSoft else c.cardAlt)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) c.accent else c.line,
                shape = ChipShape
            )
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.accent else c.inkSoft,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** 日历月视图：7 列 × 6 行色块。 */
@Composable
fun MonthGrid(
    year: Int,
    month: Int,
    dayMap: Map<String, Entry>,
    selected: String? = null,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    val cells = DateKey.monthCells(year, month)
    val today = DateKey.fromMillis(System.currentTimeMillis())
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        for (r in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (col in 0 until 7) {
                    val millis = cells[r * 7 + col]
                    val key = DateKey.fromMillis(millis)
                    val inMonth = DateKey.monthOf(millis) == month && DateKey.yearOf(millis) == year
                    val entry = dayMap[key]
                    val bg = if (entry != null) moodColorOf(entry.mood) else c.cardAlt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (inMonth) bg else bg.copy(alpha = 0.35f))
                            .then(
                                if (key == selected) {
                                    Modifier.border(3.dp, c.accent, RoundedCornerShape(12.dp))
                                } else if (key == today) {
                                    Modifier.border(
                                        2.dp,
                                        c.accent.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                } else Modifier
                            )
                            .then(if (inMonth) Modifier.clickable { onDayClick(key) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                DateKey.dayOfMonth(millis).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (entry != null) Color.White else c.inkSoft
                            )
                            if (entry != null) {
                                Text(
                                    Mood.of(entry.mood)?.emoji ?: "",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
