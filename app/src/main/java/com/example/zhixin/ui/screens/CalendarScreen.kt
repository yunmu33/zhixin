package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.Mood
import com.example.zhixin.data.Store
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.EntryCard
import com.example.zhixin.ui.components.HeatBar
import com.example.zhixin.ui.components.HeatMapCard
import com.example.zhixin.ui.components.HeatMode
import com.example.zhixin.ui.components.MoodBars
import com.example.zhixin.ui.components.MonthGrid
import com.example.zhixin.ui.components.SectionTitle
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.components.TrendChart
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.moodColorOf

@Composable
fun CalendarScreen(
    entries: List<Entry>,
    onOpenEntry: (Entry) -> Unit,
    onAddAt: (String) -> Unit
) {
    val c = LocalAppColors.current
    val now = System.currentTimeMillis()
    var year by remember { mutableIntStateOf(DateKey.yearOf(now)) }
    var month by remember { mutableIntStateOf(DateKey.monthOf(now)) }
    var heatYear by remember { mutableIntStateOf(DateKey.yearOf(now)) }
    var heatMode by remember { mutableStateOf(HeatMode.MONTH) }
    var selectedDay by remember { mutableStateOf<String?>(null) }

    val dayMap = remember(entries) { Store.dayMap(entries) }
    val prefix = String.format("%04d-%02d", year, month + 1)
    val monthEntries = remember(entries, year, month) { entries.filter { it.dateKey.startsWith(prefix) } }

    val totalHeat = remember(entries) { Heat.total(entries) }
    val todayHeat = remember(entries) { Heat.todayHeat(entries) }
    val heatByDay = remember(entries) { Heat.heatByDay(entries) }
    val scores = remember(entries) { Store.recentScores(entries, 30) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("\uD83D\uDCC5 日历 · 热度", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Text(
                    "每一天的心情色块，和你的打卡热度",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            }
        }

        item {
            SoftCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("🔥", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        String.format("%,d", totalHeat),
                        style = MaterialTheme.typography.displaySmall,
                        color = c.accent
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${Heat.levelEmoji(totalHeat)} ${Heat.levelName(totalHeat)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "今日 +$todayHeat",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (todayHeat > 0) c.accent else c.inkSoft
                    )
                }
                Spacer(Modifier.height(12.dp))
                HeatBar(progress = remember(totalHeat) { Heat.progress(totalHeat) })
                Spacer(Modifier.height(8.dp))
                Text(
                    Heat.nextLevelName(totalHeat)?.let { "再攒 ${Heat.needToNext(totalHeat)} 热度升到「$it」" }
                        ?: "热度已爆表，你太稳了！",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
        }

        item {
            SoftCard(padding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        if (month == 0) { month = 11; year -= 1 } else month -= 1
                    }) { Text("‹", style = MaterialTheme.typography.titleLarge, color = c.accent) }
                    Text(
                        "${year}年${month + 1}月",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.ink,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = {
                        if (month == 11) { month = 0; year += 1 } else month += 1
                    }) { Text("›", style = MaterialTheme.typography.titleLarge, color = c.accent) }
                    TinyButton("今天") {
                        year = DateKey.yearOf(now)
                        month = DateKey.monthOf(now)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "本月热度 ${Heat.monthHeat(entries, year, month)} 🔥",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.accent,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                MonthGrid(
                    year = year,
                    month = month,
                    dayMap = dayMap,
                    onDayClick = { selectedDay = it }
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Mood.entries.forEach { mood ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(moodColorOf(mood.key))
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(mood.label, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                        }
                    }
                }
            }
        }

        item {
            SoftCard {
                SectionTitle(emoji = "📈", title = "近 30 天情绪趋势", subtitle = "越高越轻盈，越低越低落")
                Spacer(Modifier.height(12.dp))
                TrendChart(scores = scores, color = c.accent)
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    Text("30 天前", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                    Spacer(Modifier.weight(1f))
                    Text("今天", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                }
            }
        }

        item {
            SoftCard {
                SectionTitle(emoji = "🎭", title = "心情分布", subtitle = "哪张小贴纸出现得最多？")
                Spacer(Modifier.height(12.dp))
                if (entries.isEmpty()) EmptyHint("🍃", "还没有记录，先去主页打个卡吧") else MoodBars(entries)
            }
        }

        item {
            SoftCard {
                SectionTitle(
                    emoji = "\uD83C\uDF2B",
                    title = "打卡热度",
                    subtitle = "日 / 周 / 月 / 年，四种视角看热度"
                )
                Spacer(Modifier.height(10.dp))
                HeatMapCard(
                    heatByDay = heatByDay,
                    mode = heatMode,
                    onModeChange = { heatMode = it },
                    year = heatYear,
                    onYearChange = { heatYear = it }
                )
            }
        }
    }

    selectedDay?.let { key ->
        val list = remember(entries, key) { entries.filter { it.dateKey == key && !it.deleted } }
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            containerColor = c.card,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "${DateKey.monthDayLabel(DateKey.toMillis(key))} ${DateKey.weekdayLabel(DateKey.toMillis(key))}",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
            },
            text = {
                if (list.isEmpty()) {
                    Text("这一天还很安静～", style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { e -> EntryCard(entry = e, onClick = { onOpenEntry(e) }) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onAddAt(key); selectedDay = null }) {
                    Text("写一条", color = c.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDay = null }) { Text("关闭", color = c.inkSoft) }
            }
        )
    }
}
