package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.COMMON_MOODS
import com.example.zhixin.data.Mood
import com.example.zhixin.data.Store
import com.example.zhixin.ui.components.CardShape
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.HeatBar
import com.example.zhixin.ui.components.MonthGrid
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SectionTitle
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.TagChip
import com.example.zhixin.ui.components.TinyButton
import androidx.compose.animation.AnimatedContent
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.dateTransition
import com.example.zhixin.ui.theme.moodColorOf

/**
 * 打卡页：上半部分是打卡日历（选日期 / 看本月的打卡情况），
 * 下半部分是心情日记打卡（选心情 → 勾事件 → 写一句 → 完成打卡）。
 */
@Composable
fun CheckInScreen(
    settings: AppSettings,
    entries: List<Entry>,
    onSubmit: (String, Mood, List<String>, String) -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val now = System.currentTimeMillis()
    val todayKey = DateKey.fromMillis(now)

    var selectedDate by remember { mutableStateOf(todayKey) }
    var viewYear by remember { mutableIntStateOf(DateKey.yearOf(now)) }
    var viewMonth by remember { mutableIntStateOf(DateKey.monthOf(now)) }

    val dayMap = remember(entries) {
        val map = LinkedHashMap<String, Entry>()
        entries.filter { !it.deleted }.forEach { e -> map.putIfAbsent(e.dateKey, e) }
        map
    }
    val monthPrefix = String.format("%04d-%02d", viewYear, viewMonth + 1)
    val monthDays = remember(entries, monthPrefix) {
        entries.filter { !it.deleted && it.dateKey.startsWith(monthPrefix) }
            .map { it.dateKey }.distinct().size
    }
    val monthHeat = remember(entries, viewYear, viewMonth) {
        Heat.monthHeat(entries, viewYear, viewMonth)
    }
    val streak = remember(entries) { Store.streak(entries) }
    val totalHeat = remember(entries) { Heat.total(entries) }
    val heatProgress = remember(totalHeat) { Heat.progress(totalHeat) }
    val nextName = remember(totalHeat) { Heat.nextLevelName(totalHeat) }
    val need = remember(totalHeat) { Heat.needToNext(totalHeat) }

    val selectedMillis = DateKey.toMillis(selectedDate)
    /** 某天的说法：今天 / 昨天 / 前天 / 具体日期（切日期动画里要用）。 */
    fun labelOf(key: String, today: String): String {
        if (key == today) return "今天"
        val millis = DateKey.toMillis(key)
        return when (DateKey.daysBetween(millis, now)) {
            1 -> "昨天"
            2 -> "前天"
            else -> "${DateKey.monthDayLabel(millis)} ${DateKey.weekdayLabel(millis)}"
        }
    }
    val dateLabel = labelOf(selectedDate, todayKey)
    val selectedHeat = remember(entries, selectedDate) { Heat.dayHeat(entries, selectedDate) }

    var mood by remember { mutableStateOf<Mood?>(null) }
    val tags = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    fun reset() {
        mood = null
        tags.clear()
        note = ""
        error = null
        done = false
    }

    // 顶栏固定在上面，只有下面的内容滚动
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        ScreenTopBar(
            title = "⚡ 打卡",
            subtitle = "先挑个日子，再把心情收进日记",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // ── 上半部分：打卡日历 ──
        SoftCard {
            SectionTitle(
                emoji = "\uD83D\uDCC5",
                title = "打卡日历",
                subtitle = "点格子选中要打卡的日子",
                trailing = {
                    if (selectedDate != todayKey) {
                        TinyButton("回到今天", onClick = {
                            selectedDate = todayKey
                            viewYear = DateKey.yearOf(now)
                            viewMonth = DateKey.monthOf(now)
                        })
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TinyButton("‹", onClick = {
                    if (viewMonth == 0) {
                        viewMonth = 11
                        viewYear -= 1
                    } else viewMonth -= 1
                })
                Spacer(Modifier.width(6.dp))
                Text(
                    "${viewYear}年${viewMonth + 1}月",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(6.dp))
                TinyButton("›", onClick = {
                    if (viewMonth == 11) {
                        viewMonth = 0
                        viewYear += 1
                    } else viewMonth += 1
                })
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CalendarStat("🔥", "本月热度", "$monthHeat", Modifier.weight(1f))
                CalendarStat("\uD83D\uDCC5", "本月打卡", "${monthDays}天", Modifier.weight(1f))
                CalendarStat("\uD83D\uDD25", "连续打卡", "${streak}天", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            // 翻月时格子滑着换（往后一月往左滑）
            AnimatedContent(
                targetState = viewYear * 12 + viewMonth,
                transitionSpec = { dateTransition(targetState > initialState) },
                label = "checkInMonth"
            ) { idx ->
                MonthGrid(
                    year = idx / 12,
                    month = idx % 12,
                    dayMap = dayMap,
                    selected = selectedDate,
                    onDayClick = { key ->
                        if (key <= todayKey) selectedDate = key
                        done = false
                        error = null
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            // 换一天时「选中」那行跟着滑
            AnimatedContent(
                targetState = selectedDate,
                transitionSpec = { dateTransition(targetState > initialState) },
                label = "checkInDay"
            ) { day ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "选中：${labelOf(day, todayKey)} · " +
                            DateKey.monthDayLabel(DateKey.toMillis(day)),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.accent,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (selectedHeat > 0) "当天热度 🔥$selectedHeat" else "这天还没有打卡",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
            }
        }

        // ── 下半部分：心情日记打卡 ──
        if (done) {
            CheckInSuccessCard(
                dateLabel = dateLabel,
                dateDesc = DateKey.monthDayLabel(selectedMillis),
                moodEmoji = mood?.emoji ?: "✅",
                moodLabel = mood?.label ?: "已完成",
                tags = tags.toList(),
                note = note.trim(),
                dayHeat = selectedHeat,
                streak = streak,
                totalHeat = totalHeat,
                progress = heatProgress,
                tip = if (nextName == null) "热度已爆表，你太稳了！" else "再攒 $need 热度就能升到「$nextName」",
                onBackHome = onBack,
                onAgain = { reset() }
            )
        } else {
            SoftCard {
                SectionTitle(
                    emoji = "\uD83D\uDCDD",
                    title = "心情日记打卡",
                    subtitle = "$dateLabel · ${DateKey.monthDayLabel(selectedMillis)}" +
                        if (selectedHeat > 0) "（已有记录，可以再记一张）" else ""
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "今天心情如何？",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    COMMON_MOODS.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { item ->
                                val on = mood == item
                                val color = moodColorOf(item.key)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CardShape)
                                        .background(if (on) color.copy(alpha = 0.34f) else c.cardAlt)
                                        .border(
                                            width = if (on) 2.dp else 1.dp,
                                            color = if (on) color else c.line,
                                            shape = CardShape
                                        )
                                        .clickable { mood = item }
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(item.emoji, fontSize = 30.sp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            item.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (on) c.ink else c.inkSoft
                                        )
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "这天做了什么？",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(10.dp))
                if (settings.tags.isEmpty()) {
                    Text(
                        "还没有活动标签，可在设置里添加。",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        settings.tags.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { tag ->
                                    TagChip(
                                        text = tag,
                                        selected = tags.contains(tag),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (tags.contains(tag)) tags.remove(tag) else tags.add(tag)
                                        }
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "写一句日记（选填）",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("这一天有什么小小的收获？", color = c.inkSoft) },
                    shape = ChipShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.line,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(16.dp))
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = c.accent,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                PrimaryButton(
                    text = if (mood == null) "🔥 完成打卡" else "🔥 给$dateLabel 打卡",
                    onClick = {
                        val m = mood
                        if (m == null) {
                            error = "先挑一个这天的心情吧～"
                            return@PrimaryButton
                        }
                        error = null
                        onSubmit(selectedDate, m, tags.toList(), note.trim())
                        done = true
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CalendarStat(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = c.ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
    }
}

@Composable
private fun CheckInSuccessCard(
    dateLabel: String,
    dateDesc: String,
    moodEmoji: String,
    moodLabel: String,
    tags: List<String>,
    note: String,
    dayHeat: Int,
    streak: Int,
    totalHeat: Int,
    progress: Float,
    tip: String,
    onBackHome: () -> Unit,
    onAgain: () -> Unit
) {
    val c = LocalAppColors.current
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", fontSize = 52.sp)
            Spacer(Modifier.height(8.dp))
            Text("打卡成功！", style = MaterialTheme.typography.headlineSmall, color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "$dateLabel · $dateDesc",
                style = MaterialTheme.typography.labelMedium,
                color = c.inkSoft
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "$moodEmoji $moodLabel" + if (tags.isNotEmpty()) " · ${tags.joinToString(" · ")}" else "",
                style = MaterialTheme.typography.titleSmall,
                color = c.accent,
                textAlign = TextAlign.Center
            )
            if (note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.cardAlt)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("「$note」", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SuccessStat("🔥", "当天热度", "$dayHeat", Modifier.weight(1f))
                SuccessStat("\uD83D\uDD25", "连续打卡", "${streak}天", Modifier.weight(1f))
                SuccessStat("⭐", "累计热度", String.format("%,d", totalHeat), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            HeatBar(progress = progress)
            Spacer(Modifier.height(8.dp))
            Text(tip, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
            Spacer(Modifier.height(18.dp))
            PrimaryButton(text = "完成，回主页", onClick = onBackHome)
            Spacer(Modifier.height(10.dp))
            TinyButton(text = "再打一次", onClick = onAgain)
        }
    }
}

@Composable
private fun SuccessStat(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = c.ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
    }
}
