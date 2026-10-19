package com.example.zhixin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Mood
import com.example.zhixin.data.parseHm
import androidx.compose.animation.AnimatedContent
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.categoryColor
import com.example.zhixin.ui.theme.dateTransition
import com.example.zhixin.ui.theme.dialogEnter
import com.example.zhixin.ui.theme.moodColorOf
import com.example.zhixin.ui.theme.stickerOf
import java.util.Calendar
import java.util.Locale

@Composable
fun EntryDetailDialog(
    entry: Entry,
    onDismiss: () -> Unit,
    onEdit: (Entry) -> Unit,
    onDelete: (Entry) -> Unit
) {
    val c = LocalAppColors.current
    val mood = Mood.of(entry.mood)
    val cat = Category.of(entry.category)
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "${DateKey.monthDayLabel(entry.time)} ${DateKey.weekdayLabel(entry.time)} · ${DateKey.timeLabel(entry.time)}",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mood != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(moodColorOf(mood.key).copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val res = stickerOf(mood)
                                if (res != null) {
                                    Image(
                                        painter = painterResource(res),
                                        contentDescription = mood.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    mood.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.ink
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(categoryColor(entry.category).copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("${cat.emoji} ${cat.short}", style = MaterialTheme.typography.labelMedium, color = c.ink)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("情绪分 ${entry.score}", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                }
                if (entry.text.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("💬 我讲的小故事", style = MaterialTheme.typography.labelMedium, color = c.accent)
                    Spacer(Modifier.height(4.dp))
                    Text(entry.text, style = MaterialTheme.typography.bodyMedium, color = c.ink)
                }
                if (entry.summary.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("💌 小屋整理记录", style = MaterialTheme.typography.labelMedium, color = c.accent)
                    Spacer(Modifier.height(4.dp))
                    Text(entry.summary, style = MaterialTheme.typography.bodyMedium, color = c.ink)
                }
                if (entry.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.tags.forEach { t ->
                            Text("#$t", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEdit(entry) }) {
                Text("编辑", color = c.accent)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(entry) }) {
                    Text("删除", color = Color(0xFFD9736B))
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = c.inkSoft)
                }
            }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = c.inkSoft) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.inkSoft) }
        }
    )
}

@Composable
fun InputDialog(
    title: String,
    label: String,
    initial: String = "",
    singleLine: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth().then(if (!singleLine) Modifier.height(120.dp) else Modifier),
                label = { Text(label, color = c.inkSoft) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.line
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("确定", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.inkSoft) }
        }
    )
}

/**
 * 弹窗底部的一颗胶囊按钮：确定、再想想都用它，两个才一样高、排得齐。
 *
 * @param filled true = 实心主按钮；false = 浅色次要按钮
 */
@Composable
private fun DialogPill(
    text: String,
    accent: Color,
    filled: Boolean,
    /** 实心时的底色，删除这种危险操作换成别的颜色。 */
    fill: Color = accent,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) fill else c.cardAlt)
            .then(
                if (filled) Modifier
                else Modifier.border(1.dp, c.line, RoundedCornerShape(50))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) Color.White else c.ink
        )
    }
}

/**
 * 打卡成功弹窗：一点打卡就弹，先说一声「打卡成功」，再给两个选项 ——
 * 「写点什么」补一句今天具体做了什么，「确定」收尾（写了的会存进这条记录）。
 */
@Composable
fun CheckInSuccessDialog(
    title: String = "打卡成功！",
    subtitle: String,
    doneNames: List<String> = emptyList(),
    onConfirm: (note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    // 没点「写点什么」之前只有一行入口，点了才展开输入框
    var writing by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("\uD83C\uDF89", fontSize = 44.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (doneNames.isNotEmpty()) {
                    Text(
                        "这项内容已打卡 ✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        doneNames.joinToString(" · "),
                        style = MaterialTheme.typography.titleSmall,
                        color = c.accent
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (writing) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = {
                            Text("今天做了什么、感觉怎么样…", color = c.inkSoft)
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.cardAlt,
                            focusedTextColor = c.ink,
                            unfocusedTextColor = c.ink,
                            cursorColor = c.accent
                        )
                    )
                    LaunchedEffect(Unit) { focus.requestFocus() }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.cardAlt)
                            .clickable { writing = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✏\uFE0F", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "写点什么…（可选）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.inkSoft
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note.trim()) }) {
                Text("确定", style = MaterialTheme.typography.titleSmall, color = c.accent)
            }
        },
        dismissButton = {
            if (!writing) {
                TextButton(onClick = { writing = true }) {
                    Text("写点什么", style = MaterialTheme.typography.titleSmall, color = c.inkSoft)
                }
            }
        }
    )
}

/**
 * 打卡弹窗：先写一句今天具体做了什么，点下方「确定」才算打卡成功。
 * @param willUndo 今天已经打过卡了，这次点确定是删掉这次的打卡（连同写的内容一起进回收站）
 * @param history  这次打卡当时写的内容，删除时原样展示，不能改
 */
@Composable
fun CheckInDialog(
    emoji: String,
    name: String,
    accent: Color,
    willUndo: Boolean,
    history: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    var value by remember { mutableStateOf("") }
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (willUndo) "已打卡" else "打卡 · $name",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
            }
        },
        text = {
            Column {
                Text(
                    if (willUndo) {
                        "已经打过卡了。删除会把这次打卡和当时写的内容一起放进回收站，确定吗？"
                    } else {
                        "写点具体的吧，以后翻记录会很有画面感～"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(10.dp))
                if (willUndo) {
                    // 删除时就不用填了，把当时写的原样摆出来
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.cardAlt)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "当时写的",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            history.ifBlank { "（这次打卡没有写内容）" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (history.isBlank()) c.inkSoft else c.ink
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        placeholder = { Text("今天做了什么？（选填）", color = c.inkSoft) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = c.line
                        )
                    )
                }
            }
        },
        confirmButton = {
            // 下方的确定按钮：点了才算完成打卡
            DialogPill(
                text = if (willUndo) "确定删除" else "确定",
                accent = accent,
                filled = true,
                fill = if (willUndo) Color(0xFFE38B8B) else accent,
                onClick = { onConfirm(value) }
            )
        },
        dismissButton = {
            // 跟确定一样的一颗胶囊，两个按钮才排得齐
            DialogPill(
                text = "再想想",
                accent = accent,
                filled = false,
                onClick = onDismiss
            )
        }
    )
}

/** 把毫秒格式化成 HH:mm。 */
private fun hmOf(millis: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return String.format(
        Locale.US,
        "%02d:%02d",
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE)
    )
}

/**
 * 日历点某一天：把那天的打卡内容摊开给你看，写错的字可以改。
 * 打没打卡是记录本身，这里不能改（要改请用卡片上的打卡钮）。
 */
@Composable
fun DayRecordsDialog(
    dateLabel: String,
    emoji: String,
    name: String,
    accent: Color,
    records: List<Entry>,
    /** 改完点保存，把改过的那几条交回去。 */
    onSave: (List<Pair<Entry, String>>) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    var texts by remember(records) { mutableStateOf(records.map { it.text }) }
    val scroll = rememberScrollState()
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(dateLabel, style = MaterialTheme.typography.titleSmall, color = c.ink)
                }
                Text(
                    if (records.isEmpty()) {
                        "$name · 这天没有打卡记录"
                    } else {
                        "$name · 打了 ${records.size} 次"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(scroll)) {
                if (records.isEmpty()) {
                    Text(
                        "这天还没打过卡，内容也就不存在啦。想记录的话用卡片上的打卡钮～",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSoft
                    )
                } else {
                    Text(
                        "打卡记录改不了，只改当时写的内容",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(10.dp))
                    records.forEachIndexed { i, record ->
                        Text(
                            "第 ${i + 1} 次 · ${hmOf(record.time)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = texts[i],
                            onValueChange = { v ->
                                texts = texts.toMutableList().also { it[i] = v }
                            },
                            placeholder = { Text("当时没写内容", color = c.inkSoft) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = c.line
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        },
        confirmButton = {
            if (records.isNotEmpty()) {
                DialogPill(
                    text = "保存",
                    accent = accent,
                    filled = true,
                    onClick = {
                        val changed = records.mapIndexedNotNull { i, record ->
                            val newText = texts[i].trim()
                            if (newText != record.text) record to newText else null
                        }
                        if (changed.isNotEmpty()) onSave(changed)
                        onDismiss()
                    }
                )
            }
        },
        dismissButton = {
            // 跟保存同一颗胶囊，两个才排得齐
            DialogPill(
                text = if (records.isEmpty()) "知道啦" else "取消",
                accent = accent,
                filled = false,
                onClick = onDismiss
            )
        }
    )
}

/** 选一天：上面切月份，下面点日期。回调给的是那天 0 点的毫秒。 */
@Composable
fun DatePickDialog(
    title: String,
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    var year by remember { mutableIntStateOf(DateKey.yearOf(initialMillis)) }
    var month by remember { mutableIntStateOf(DateKey.monthOf(initialMillis)) }
    val pickKey = DateKey.fromMillis(initialMillis)
    val todayKey = DateKey.fromMillis(System.currentTimeMillis())
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink)
        },
        text = {
            Column {
                // 月份：‹ 2026年10月 ›
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonthArrow("‹") { shiftMonth(year, month, -1).let { year = it.first; month = it.second } }
                    // 翻月时年月跟着滑
                    AnimatedContent(
                        targetState = year * 12 + month,
                        transitionSpec = { dateTransition(targetState > initialState) },
                        modifier = Modifier.weight(1f),
                        label = "pickMonthTitle"
                    ) { idx ->
                        Text(
                            "${idx / 12}年${idx % 12 + 1}月",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    MonthArrow("›") { shiftMonth(year, month, 1).let { year = it.first; month = it.second } }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { w ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                w,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 翻月时格子滑着换
                AnimatedContent(
                    targetState = year * 12 + month,
                    transitionSpec = { dateTransition(targetState > initialState) },
                    label = "pickMonthGrid"
                ) { idx ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    remember(idx) { DateKey.monthCells(idx / 12, idx % 12) }
                        .chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { millis ->
                                val key = DateKey.fromMillis(millis)
                                val inMonth = DateKey.yearOf(millis) == idx / 12 &&
                                    DateKey.monthOf(millis) == idx % 12
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (key) {
                                                pickKey -> c.accent
                                                todayKey -> c.accentSoft
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onConfirm(startOfDayMs(millis)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (inMonth) {
                                        Text(
                                            "${DateKey.dayOfMonth(millis)}",
                                            fontSize = 13.sp,
                                            color = if (key == pickKey) Color.White else c.ink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.inkSoft) }
        }
    )
}

/** 往前 / 往后翻一个月，返回新的（年, 月）。 */
private fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month, 1)
    }
    cal.add(Calendar.MONTH, delta)
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
}

/** 某一天的 0 点。 */
private fun startOfDayMs(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** 日期弹窗里切月份的小箭头。 */
@Composable
private fun MonthArrow(label: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.cardAlt)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 18.sp, color = c.ink)
    }
}

/** 时间选择弹窗（24 小时制），回调返回 "HH:mm"。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    val hm = parseHm(initial)
    val state = rememberTimePickerState(
        initialHour = hm?.div(60) ?: 8,
        initialMinute = hm?.mod(60) ?: 0,
        is24Hour = true
    )
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
            }) { Text("确定", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.inkSoft) }
        }
    )
}
