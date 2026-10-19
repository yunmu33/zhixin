package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.ui.components.CardShape
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.MonthGrid
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.StatCard
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.dialogEnter

/**
 * 日记内容（已合并进「心情日记」页，用顶部的「心情 / 日记」切换）：
 * - 顶部：累计写过的天数 / 条目 / 累计字数
 * - 月历视图：点击某天 → 弹出当日的全部记录
 * - "✍ 写一条"按钮：今天直接写；月历选中可补写
 * - 近期日记列表：按时间倒序展示 DIARY 类记录
 */
@Composable
fun DiaryScreen(
    entries: List<Entry>,
    onOpenEntry: (Entry) -> Unit,
    onWriteDiary: (dateKey: String) -> Unit,
    onDeleteDiary: (Entry) -> Unit
) {
    val c = LocalAppColors.current

    val diaries = remember(entries) {
        entries.filter { !it.deleted && it.category == Category.DIARY.key }
            .sortedByDescending { it.time }
    }
    val diaryKeys = remember(diaries) { diaries.map { it.dateKey }.toSet() }
    val dayMap = remember(entries) {
        entries.filter { !it.deleted }
            .groupBy { it.dateKey }
            .mapValues { (_, list) -> list.maxByOrNull { it.time }!! }
    }

    var year by remember { mutableStateOf(DateKey.yearOf(System.currentTimeMillis())) }
    var month by remember { mutableStateOf(DateKey.monthOf(System.currentTimeMillis())) }
    var selectedDateKey by remember {
        mutableStateOf(DateKey.fromMillis(System.currentTimeMillis()))
    }
    var dayDialogKey by remember { mutableStateOf<String?>(null) }

    val today = DateKey.fromMillis(System.currentTimeMillis())
    val totalDays = diaryKeys.size
    val totalCount = diaries.size
    val totalChars = diaries.sumOf { it.text.length }

    val selectedDiaries = remember(diaries, selectedDateKey) {
        diaries.filter { it.dateKey == selectedDateKey }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 12.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    "\uD83D\uDCC5", "$totalDays 天",
                    "写过的日子", Modifier.weight(1f)
                )
                StatCard(
                    "\uD83D\uDCDD", "$totalCount 条",
                    "累计日记", Modifier.weight(1f)
                )
                StatCard(
                    "✍\uFE0F", "$totalChars",
                    "累计字数", Modifier.weight(1f)
                )
            }
        }

        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${year} 年 ${month + 1} 月",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink
                    )
                    Spacer(Modifier.weight(1f))
                    TinyButton(text = "‹", onClick = {
                        if (month == 0) { year -= 1; month = 11 } else month -= 1
                    })
                    Spacer(Modifier.width(6.dp))
                    TinyButton(text = "›", onClick = {
                        if (month == 11) { year += 1; month = 0 } else month += 1
                    })
                    Spacer(Modifier.width(6.dp))
                    TinyButton(
                        text = "今",
                        onClick = {
                            val now = System.currentTimeMillis()
                            year = DateKey.yearOf(now)
                            month = DateKey.monthOf(now)
                            selectedDateKey = today
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                MonthGrid(
                    year = year,
                    month = month,
                    dayMap = dayMap,
                    selected = selectedDateKey,
                    onDayClick = {
                        selectedDateKey = it
                        dayDialogKey = it
                    }
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryButton(
                        text = "✍ 写今天",
                        modifier = Modifier.weight(1f),
                        onClick = { onWriteDiary(today) }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        PrimaryButton(
                            text = if (selectedDateKey == today) "✍ 补今天"
                            else "✍ 补 ${DateKey.monthDayLabel(
                                DateKey.toMillis(selectedDateKey)
                            )}",
                            onClick = { onWriteDiary(selectedDateKey) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "点击日历上的某一天可查看/补写",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        if (selectedDiaries.isNotEmpty()) {
            item {
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "\uD83D\uDCDD ${DateKey.monthDayLabel(
                                DateKey.toMillis(selectedDateKey)
                            )}",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${selectedDiaries.size} 条",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedDiaries.forEach { e ->
                            EntryRow(
                                entry = e,
                                onOpen = { onOpenEntry(e) },
                                onDelete = { onDeleteDiary(e) }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                SoftCard {
                    Text(
                        "\uD83D\uDCDD ${DateKey.monthDayLabel(
                            DateKey.toMillis(selectedDateKey)
                        )}",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (selectedDateKey == today)
                            "今天还没有写日记哦，把这一天的想法记下来吧"
                        else "这一天还没留下文字，要不要补写一篇？",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "\uD83C\uDF38 近期日记",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "共 ${diaries.size} 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
        }

        if (diaries.isEmpty()) {
            item {
                EmptyHint(
                    "\uD83D\uDCDD",
                    "还没有写过日记。哪怕只是今天的一点点心情，也值得被记录～"
                )
            }
        } else {
            items(diaries.take(40)) { e ->
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(
                            modifier = Modifier.width(72.dp)
                        ) {
                            Text(
                                DateKey.monthDayLabel(e.time),
                                style = MaterialTheme.typography.labelMedium,
                                color = c.accent
                            )
                            Text(
                                DateKey.weekdayLabel(e.time),
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft
                            )
                        }
                        Text(
                            text = e.text.ifBlank { "（空）" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink,
                            maxLines = 4,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenEntry(e) }
                        )
                    }
                }
            }
            if (diaries.size > 40) {
                item {
                    Text(
                        "只展示最近 40 条，更多可在「我的」里查看",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Text(
                "日子慢慢写，慢慢变成自己的书 \uD83D\uDCDA",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    dayDialogKey?.let { key ->
        val dayEntries = remember(key, entries) {
            entries.filter { !it.deleted && it.dateKey == key }
                .sortedByDescending { it.time }
        }
        AlertDialog(
            modifier = dialogEnter(),
            onDismissRequest = { dayDialogKey = null },
            containerColor = c.card,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    DateKey.formatFull(key),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
            },
            text = {
                if (dayEntries.isEmpty()) {
                    Text(
                        "这一天还没有留下记录，要不要补写一篇？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSoft
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        dayEntries.forEach { e ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    Category.of(e.category).emoji,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = e.text.ifBlank {
                                            Category.of(e.category).label
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = c.ink
                                    )
                                    if (e.tags.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = e.tags.joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = c.inkSoft
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = DateKey.timeLabel(e.time),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.inkSoft
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    dayDialogKey = null
                                    onOpenEntry(e)
                                }) { Text("编辑", color = c.accent) }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = {
                                    onDeleteDiary(e)
                                }) { Text("删除", color = c.inkSoft) }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(c.line)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dayDialogKey = null
                    onWriteDiary(key)
                }) {
                    Text(
                        if (key == today) "✍ 写一条" else "✍ 补一篇",
                        color = c.accent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { dayDialogKey = null }) {
                    Text("关闭", color = c.inkSoft)
                }
            }
        )
    }
}

@Composable
private fun EntryRow(
    entry: Entry,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.cardAlt)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = entry.text.ifBlank { "（空）" },
                style = MaterialTheme.typography.bodyMedium,
                color = c.ink,
                maxLines = 3
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateKey.timeLabel(entry.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}
