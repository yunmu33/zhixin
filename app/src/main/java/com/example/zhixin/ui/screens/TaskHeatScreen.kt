package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.CUSTOM_TAG
import com.example.zhixin.data.CustomItem
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.theme.LocalAppColors
import java.util.Calendar

private enum class HeatView(val label: String) { WEEK("周"), MONTH("月"), YEAR("年") }

/** 任务打卡的热度日历：可以切周 / 月 / 年三种视图，格子越深那天打得越多。 */
@Composable
fun TaskHeatScreen(
    entries: List<Entry>,
    settings: AppSettings,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val now = System.currentTimeMillis()
    // 当前看到的时间点：切视图 / 左右翻页都动它
    var anchor by remember { mutableLongStateOf(now) }
    var view by remember { mutableStateOf(HeatView.MONTH) }
    var pickedDay by remember { mutableStateOf<String?>(null) }

    // 只看任务打卡记录，心情 / 日记那些不算进来
    val taskEntries = remember(entries) {
        entries.filter { !it.deleted && it.tags.contains(CUSTOM_TAG) }
    }
    // 每天打了几次
    val countByDay = remember(taskEntries) {
        taskEntries.groupingBy { it.dateKey }.eachCount()
    }
    val todayKey = DateKey.fromMillis(now)

    // 当前视图覆盖的时间范围
    val rangePrefix = remember(view, anchor) {
        when (view) {
            HeatView.YEAR -> "${DateKey.yearOf(anchor)}"
            else -> {
                val cal = Calendar.getInstance().apply { timeInMillis = anchor }
                "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
            }
        }
    }
    val rangeEntries = remember(taskEntries, view, anchor) {
        when (view) {
            HeatView.WEEK -> {
                val start = DateKey.fromMillis(weekStart(anchor))
                val keys = (0..6).map { DateKey.fromMillis(DateKey.addDays(DateKey.toMillis(start), it)) }
                taskEntries.filter { it.dateKey in keys }
            }
            else -> taskEntries.filter { it.dateKey.startsWith(rangePrefix) }
        }
    }
    val rangeCount = rangeEntries.size
    val rangeDays = rangeEntries.map { it.dateKey }.distinct().size
    val rangeBest = rangeEntries.groupingBy { it.dateKey }.eachCount().values.maxOrNull() ?: 0
    val totalDays = countByDay.size

    // 连续打卡天数：今天没打就从昨天往前数
    val streak = remember(countByDay) {
        val keys = countByDay.keys
        var start = DateKey.startOfDay(now)
        if (DateKey.fromMillis(start) !in keys) start = DateKey.addDays(start, -1)
        var n = 0
        while (DateKey.fromMillis(start) in keys) {
            n++
            start = DateKey.addDays(start, -1)
        }
        n
    }

    // 标题：三种视图各自的写法
    val rangeLabel = remember(view, anchor) {
        when (view) {
            HeatView.WEEK -> {
                val s = weekStart(anchor)
                val e = DateKey.addDays(s, 6)
                "${DateKey.monthDayLabel(s)} - ${DateKey.monthDayLabel(e)}"
            }
            HeatView.MONTH -> "${DateKey.yearOf(anchor)}年${DateKey.monthOf(anchor) + 1}月"
            HeatView.YEAR -> "${DateKey.yearOf(anchor)}年"
        }
    }

    fun shift(delta: Int) {
        anchor = when (view) {
            HeatView.WEEK -> DateKey.addDays(anchor, delta * 7)
            HeatView.MONTH -> Calendar.getInstance().apply {
                timeInMillis = anchor
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, delta)
            }.timeInMillis
            HeatView.YEAR -> Calendar.getInstance().apply {
                timeInMillis = anchor
                add(Calendar.YEAR, delta)
            }.timeInMillis
        }
        pickedDay = null
    }

    // 顶栏固定，只有下面的内容滚动；上下都避开手机的状态栏 / 手势条
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Bottom
                )
            )
    ) {
        ScreenTopBar(
            title = "\uD83D\uDD25 打卡热度日历",
            subtitle = "格子越深，那天打得越多",
            compact = true,
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SoftCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.cardAlt)
                                .clickable { shift(-1) },
                            contentAlignment = Alignment.Center
                        ) { Text("‹", fontSize = 18.sp, color = c.ink) }
                        Spacer(Modifier.weight(1f))
                        Text(
                            rangeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.cardAlt)
                                .clickable { shift(1) },
                            contentAlignment = Alignment.Center
                        ) { Text("›", fontSize = 18.sp, color = c.ink) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeatView.entries.forEach { v ->
                            ViewChip(
                                text = "${v.label}视图",
                                selected = view == v,
                                modifier = Modifier.weight(1f)
                            ) {
                                view = v
                                pickedDay = null
                            }
                        }
                    }
                }
            }

            item {
                // 周 / 年视图把左右留白收紧一点，格子就能更大
                SoftCard(
                    padding = PaddingValues(
                        horizontal = when (view) {
                            HeatView.WEEK -> 10.dp
                            // 年视图把留白让给格子，格子能大一点
                            HeatView.YEAR -> 6.dp
                            else -> 16.dp
                        },
                        vertical = 14.dp
                    )
                ) {
                    when (view) {
                        HeatView.WEEK -> WeekBoard(
                            startMillis = weekStart(anchor),
                            countByDay = countByDay,
                            todayKey = todayKey,
                            picked = pickedDay
                        ) { pickedDay = if (pickedDay == it) null else it }
                        HeatView.MONTH -> MonthBoard(
                            year = DateKey.yearOf(anchor),
                            month = DateKey.monthOf(anchor),
                            countByDay = countByDay,
                            todayKey = todayKey,
                            picked = pickedDay
                        ) { pickedDay = if (pickedDay == it) null else it }
                        HeatView.YEAR -> YearBoard(
                            year = DateKey.yearOf(anchor),
                            countByDay = countByDay,
                            todayKey = todayKey,
                            picked = pickedDay
                        ) { pickedDay = if (pickedDay == it) null else it }
                    }
                    // 日历和下面的图例隔开，别让两片颜色贴在一起
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(c.line)
                    )
                    Spacer(Modifier.height(10.dp))
                    // 图例
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("少", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = c.inkSoft)
                        Spacer(Modifier.width(4.dp))
                        listOf(0, 1, 2, 3, 4).forEach { n ->
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(heatColor(c.accent, c.cardAlt, n))
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("多", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = c.inkSoft)
                    }
                }
            }

            // 选中某天：下面列出那天完成了什么
            if (pickedDay != null) {
                item {
                    val key = pickedDay!!
                    val thatDay = taskEntries.filter { it.dateKey == key }
                    SoftCard {
                        Text(
                            "${DateKey.monthDayLabel(DateKey.toMillis(key))} · ${DateKey.weekdayLabel(DateKey.toMillis(key))}",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink
                        )
                        Spacer(Modifier.height(8.dp))
                        if (thatDay.isEmpty()) {
                            Text(
                                "这天没有打卡记录，休息一下也挺好的 🌿",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.inkSoft
                            )
                        } else {
                            thatDay.groupingBy { e ->
                                e.tags.firstOrNull { it != CUSTOM_TAG } ?: "打卡"
                            }.eachCount().forEach { (name, n) ->
                                val item = settings.customItems.firstOrNull { it.name == name }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item?.emoji ?: "\uD83C\uDFAF", fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.ink,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "×$n",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.inkSoft
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SoftCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MiniStat(
                            when (view) {
                                HeatView.WEEK -> "本周打卡"
                                HeatView.MONTH -> "本月打卡"
                                HeatView.YEAR -> "全年打卡"
                            },
                            "$rangeCount",
                            Modifier.weight(1f)
                        )
                        MiniStat("打卡天数", "$rangeDays", Modifier.weight(1f))
                        MiniStat("单日最多", "$rangeBest", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MiniStat("累计打卡", "${taskEntries.size}", Modifier.weight(1f))
                        MiniStat("累计天数", "$totalDays", Modifier.weight(1f))
                        MiniStat("连续", "$streak", Modifier.weight(1f))
                    }
                    if (view == HeatView.YEAR && rangeDays > 0) {
                        Spacer(Modifier.height(10.dp))
                        val bestMonth = rangeEntries
                            .groupingBy { it.dateKey.substring(5, 7) }
                            .eachCount()
                            .maxByOrNull { it.value }
                        if (bestMonth != null) {
                            Text(
                                "最投入的一个月：${bestMonth.key.toInt()}月 · ${bestMonth.value} 次",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                SoftCard {
                    Text(
                        when (view) {
                            HeatView.WEEK -> "本周各任务"
                            HeatView.MONTH -> "本月各任务"
                            HeatView.YEAR -> "全年各任务"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    if (settings.customItems.isEmpty()) {
                        Text(
                            "还没有任务，先去任务页加一个吧 ✨",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkSoft
                        )
                    } else {
                        settings.customItems.forEachIndexed { index, item ->
                            val mine = rangeEntries.filter { it.tags.contains(item.name) }
                            TaskHeatRow(
                                item = item,
                                count = mine.size,
                                days = mine.map { it.dateKey }.distinct().size,
                                color = taskColor(item, index)
                            )
                            if (index != settings.customItems.lastIndex) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "点格子可以看那天完成了什么 · 左右箭头换${view.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 当天所在周的周一（毫秒）。 */
private fun weekStart(anchor: Long): Long {
    val day = DateKey.startOfDay(anchor)
    return DateKey.addDays(day, -DateKey.weekdayIndexMondayFirst(day))
}

/** 打卡次数 -> 格子颜色：档位之间拉得开，一眼就能分出打得多少。 */
private fun heatColor(accent: Color, empty: Color, count: Int): Color = when {
    count <= 0 -> empty
    count == 1 -> accent.copy(alpha = 0.45f)
    count == 2 -> accent.copy(alpha = 0.68f)
    count == 3 -> accent.copy(alpha = 0.88f)
    else -> accent
}

@Composable
private fun ViewChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.accentSoft else c.cardAlt)
            .border(1.dp, if (selected) c.accent else c.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.accent else c.inkSoft
        )
    }
}

/** 周视图：七个大格子，写着日期和当天次数。 */
@Composable
private fun WeekBoard(
    startMillis: Long,
    countByDay: Map<String, Int>,
    todayKey: String,
    picked: String?,
    onPick: (String) -> Unit
) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (0..6).forEach { i ->
            val millis = DateKey.addDays(startMillis, i)
            val key = DateKey.fromMillis(millis)
            val n = countByDay[key] ?: 0
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    listOf("一", "二", "三", "四", "五", "六", "日")[i],
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(5.dp))
                HeatCell(
                    day = DateKey.dayOfMonth(millis),
                    count = n,
                    picked = picked == key,
                    isToday = key == todayKey,
                    // 宽度撑满这一列，高度按比例算：不能用 weight，那会把它拉满整个卡片
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.78f),
                    big = true,
                    showCount = true
                ) { onPick(key) }
            }
        }
    }
}

/** 月视图：整月铺开，周一开头。 */
@Composable
private fun MonthBoard(
    year: Int,
    month: Int,
    countByDay: Map<String, Int>,
    todayKey: String,
    picked: String?,
    onPick: (String) -> Unit
) {
    val c = LocalAppColors.current
    val firstMillis = DateKey.toMillis(DateKey.of(year, month, 1))
    val lead = DateKey.weekdayIndexMondayFirst(firstMillis)
    val dayCount = DateKey.daysInMonth(year, month)
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        w,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = c.inkSoft
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        (0 until (lead + dayCount + 6) / 7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (0..6).forEach { col ->
                    val index = row * 7 + col
                    val day = index - lead + 1
                    if (index < lead || day > dayCount) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val key = DateKey.of(year, month, day)
                        HeatCell(
                            day = day,
                            count = countByDay[key] ?: 0,
                            picked = picked == key,
                            isToday = key == todayKey,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) { onPick(key) }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

/**
 * 年视图：一整年的日子按顺序每天一个小方块，一行 20 个往下排，
 * 宽度铺满、格子是正方形，不用左右滑，往下滚一点就能看完整年。
 */
@Composable
private fun YearBoard(
    year: Int,
    countByDay: Map<String, Int>,
    todayKey: String,
    picked: String?,
    onPick: (String) -> Unit
) {
    val c = LocalAppColors.current
    val perRow = 20
    val gap = 1.dp

    // 这一年的每一天，从 1 月 1 日排到 12 月 31 日
    val days = remember(year) {
        val out = ArrayList<Long>()
        var t = DateKey.toMillis(DateKey.of(year, 0, 1))
        while (DateKey.yearOf(t) == year) {
            out.add(t)
            t = DateKey.addDays(t, 1)
        }
        out
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cell = (maxWidth - gap * (perRow - 1)) / perRow
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            days.chunked(perRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { millis ->
                        val key = DateKey.fromMillis(millis)
                        val n = countByDay[key] ?: 0
                        val marked = picked == key || key == todayKey
                        Box(
                            modifier = Modifier
                                .size(cell)
                                .clip(RoundedCornerShape(2.dp))
                                .background(heatColor(c.accent, c.cardAlt, n))
                                .border(
                                    if (marked) 1.5.dp else 0.5.dp,
                                    if (marked) {
                                        c.accent
                                    } else {
                                        // 淡淡一圈边，小方块之间界限清楚
                                        c.inkSoft.copy(alpha = 0.25f)
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                                .clickable { onPick(key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatCell(
    day: Int,
    count: Int,
    picked: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    big: Boolean = false,
    showCount: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    val bg = heatColor(c.accent, c.cardAlt, count)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (big) 14.dp else 9.dp))
            .background(bg)
            .border(
                if (picked || isToday) 1.5.dp else 0.dp,
                if (picked) c.accent else c.line,
                RoundedCornerShape(if (big) 14.dp else 9.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                style = if (big) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.labelSmall
                },
                fontSize = if (big) 18.sp else 11.sp,
                color = if (count >= 4) Color.White else c.ink
            )
            if (showCount) {
                Spacer(Modifier.height(2.dp))
                Text(
                    if (count > 0) "×$count" else "·",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (big) 11.sp else 9.sp,
                    color = if (count >= 4) Color.White else c.inkSoft
                )
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = c.accent)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = c.inkSoft
        )
    }
}

@Composable
private fun TaskHeatRow(
    item: CustomItem,
    count: Int,
    days: Int,
    color: Color
) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { Text(item.emoji, fontSize = 16.sp) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodySmall, color = c.ink)
            Text(
                "打卡 $count 次 · $days 天",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = c.inkSoft
            )
        }
        Text(if (count > 0) "\uD83D\uDD25" else "·", fontSize = 14.sp, color = c.inkSoft)
    }
}
