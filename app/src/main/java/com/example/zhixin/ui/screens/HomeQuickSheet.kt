package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.CUSTOM_TAG
import com.example.zhixin.data.Category
import com.example.zhixin.data.CustomItem
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.Mood
import com.example.zhixin.data.Store
import com.example.zhixin.ui.components.CardShape
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.ConfirmDialog
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.HeatBar
import com.example.zhixin.ui.components.MoodPicker
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.TagChip
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.moodColorOf

val TASK_EMOJIS = listOf(
    "\uD83D\uDCA7", "\uD83D\uDCDA", "🏃", "\uD83E\uDDD8", "🍎", "\uD83D\uDCA4",
    "\uD83E\uDDF9", "✍️", "\uD83C\uDFA7", "\uD83C\uDF3F", "☕", "\uD83C\uDFAF"
)

/** 主页三个入口点开后的弹窗外壳。 */
@Composable
fun QuickSheet(
    emoji: String,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalAppColors.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                }
                TinyButton("✕", onClick = onDismiss)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/** 🌱 每日一新：快速记一条 + 今天记了什么。 */
@Composable
fun DailyNewSheet(
    entries: List<Entry>,
    onQuickSave: (String) -> Unit,
    onGoNew: () -> Unit,
    onOpenEntry: (Entry) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    val today = DateKey.fromMillis(System.currentTimeMillis())
    val todayList = remember(entries) {
        entries.filter { !it.deleted && it.category == Category.NEW.key && it.dateKey == today }
    }
    var text by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }

    QuickSheet(
        emoji = "\uD83C\uDF31",
        title = "每日一新",
        subtitle = if (todayList.isEmpty()) "今天还没有新收获"
        else "今天已经收获 ${todayList.size} 条，很棒～",
        onDismiss = onDismiss
    ) {
        if (done) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🌱", fontSize = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text("已收下你的新收获！", style = MaterialTheme.typography.titleSmall, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text("热度 +${Heat.BASE}", style = MaterialTheme.typography.labelMedium, color = c.accent)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "完成", onClick = onDismiss)
                Spacer(Modifier.height(8.dp))
                TinyButton(text = "再记一条", onClick = { text = ""; done = false })
            }
        } else {
            Text("今天学到了 / 发现了什么？", style = MaterialTheme.typography.bodyMedium, color = c.ink)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                minLines = 3,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("哪怕一句话也算数…", color = c.inkSoft) },
                shape = ChipShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.line,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = "✨ 记下这条",
                onClick = {
                    if (text.isBlank()) return@PrimaryButton
                    onQuickSave(text.trim())
                    done = true
                }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "今天已记录",
                style = MaterialTheme.typography.labelMedium,
                color = c.inkSoft
            )
            Spacer(Modifier.height(8.dp))
            if (todayList.isEmpty()) {
                EmptyHint("🍃", "还没有记录，写第一条吧～")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    todayList.forEach { entry ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CardShape)
                                .background(c.cardAlt)
                                .clickable { onOpenEntry(entry) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                entry.text.ifBlank { "（无内容）" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.ink,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TinyButton(text = "去每日一新页看看 →", onClick = onGoNew)
            }
        }
    }
}

/** 🎯 任务打卡：自定义任务的今日完成情况。 */
@Composable
fun TaskCheckSheet(
    settings: AppSettings,
    entries: List<Entry>,
    onToggle: (CustomItem) -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (CustomItem) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    val today = DateKey.fromMillis(System.currentTimeMillis())
    val doneSet = remember(entries, today) {
        entries.filter { it.dateKey == today && !it.deleted && it.tags.contains(CUSTOM_TAG) }
            .flatMap { it.tags }
            .toSet()
    }
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(TASK_EMOJIS.first()) }
    var pendingDelete by remember { mutableStateOf<CustomItem?>(null) }
    val finished = settings.customItems.count { doneSet.contains(it.name) }

    QuickSheet(
        emoji = "\uD83C\uDFAF",
        title = "任务打卡",
        subtitle = if (settings.customItems.isEmpty()) "还没有任务，先添加一个"
        else "今天完成 $finished / ${settings.customItems.size}",
        onDismiss = onDismiss
    ) {
        if (settings.customItems.isEmpty()) {
            EmptyHint("\uD83C\uDFAF", "点下面「＋ 添加任务」，创建你的第一个打卡项")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                settings.customItems.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item ->
                            val done = doneSet.contains(item.name)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (done) c.accent.copy(alpha = 0.24f) else c.cardAlt
                                    )
                                    .border(
                                        width = if (done) 1.5.dp else 1.dp,
                                        color = if (done) c.accent else c.line,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .combinedClickable(
                                        onClick = { onToggle(item) },
                                        onLongClick = { pendingDelete = item }
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (done) "✅" else item.emoji, fontSize = 20.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (done) c.accent else c.ink,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (!adding) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TinyButton(text = "＋ 添加任务", onClick = { adding = true })
            }
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("名称，例如：喝水", color = c.inkSoft) },
                shape = ChipShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.line,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TASK_EMOJIS.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { e ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (e == emoji) c.accentSoft else c.cardAlt)
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) { Text(e, fontSize = 18.sp) }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                TinyButton(text = "取消", onClick = { adding = false; name = "" })
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryButton(
                        text = "添加任务",
                        onClick = {
                            val n = name.trim()
                            if (n.isBlank()) return@PrimaryButton
                            onAdd(emoji, n)
                            name = ""
                            adding = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "点一下完成，再点一下取消，长按可删除",
            style = MaterialTheme.typography.labelSmall,
            color = c.inkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "删除任务",
            message = "要删掉「${item.name}」吗？已经打过的记录会保留。",
            confirmText = "删除",
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

/** 😊 心情打卡：快速记录今天的心情。 */
@Composable
fun MoodCheckSheet(
    settings: AppSettings,
    entries: List<Entry>,
    onCheckIn: (Mood, List<String>, String) -> Unit,
    onGoFull: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    val todayHeat = remember(entries) { Heat.todayHeat(entries) }
    val totalHeat = remember(entries) { Heat.total(entries) }
    val streak = remember(entries) { Store.streak(entries) }

    var mood by remember { mutableStateOf<Mood?>(null) }
    val tags = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    QuickSheet(
        emoji = "\uD83D\uDE00",
        title = "心情打卡",
        subtitle = if (todayHeat > 0) "今日热度 🔥$todayHeat · 连续 ${streak}天"
        else "今天还没有打卡",
        onDismiss = onDismiss
    ) {
        if (done) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text("打卡成功！", style = MaterialTheme.typography.titleSmall, color = c.ink)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔥", fontSize = 18.sp)
                        Text(
                            "$todayHeat",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink
                        )
                        Text("今日热度", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("\uD83D\uDD25", fontSize = 18.sp)
                        Text(
                            "${streak}天",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink
                        )
                        Text("连续打卡", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HeatBar(progress = remember(totalHeat) { Heat.progress(totalHeat) })
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "完成", onClick = onDismiss)
                Spacer(Modifier.height(8.dp))
                TinyButton(text = "再打一张", onClick = {
                    mood = null
                    tags.clear()
                    note = ""
                    done = false
                })
            }
        } else {
            Text("今天心情如何？", style = MaterialTheme.typography.bodyMedium, color = c.ink)
            Spacer(Modifier.height(10.dp))
            MoodPicker(selected = mood?.key, onSelect = { mood = it })
            Spacer(Modifier.height(16.dp))
            if (settings.tags.isNotEmpty()) {
                Text("做了什么？", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(16.dp))
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("写一句今天想说的话（选填）…", color = c.inkSoft) },
                shape = ChipShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.line,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = "🔥 完成打卡",
                onClick = {
                    val m = mood
                    if (m == null) {
                        error = "先挑一个今天的心情吧～"
                        return@PrimaryButton
                    }
                    error = null
                    onCheckIn(m, tags.toList(), note.trim())
                    done = true
                }
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TinyButton(text = "打卡日历 + 心情日记 →", onClick = onGoFull)
            }
        }
    }
}
