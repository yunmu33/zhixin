package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.TodoItem
import com.example.zhixin.data.TodoZone
import com.example.zhixin.ui.components.ConfirmDialog
import com.example.zhixin.ui.components.DatePickDialog
import com.example.zhixin.ui.components.TimePickDialog
import androidx.compose.animation.AnimatedContent
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.dateTransition
import com.example.zhixin.ui.theme.dialogEnter
import java.util.Calendar
import java.util.Locale

/** 设了开始时间但没动结束时间时，默认往后排这么久。 */
private const val DEFAULT_DURATION = 2 * 60 * 60 * 1000L

/** 待办清单里每条怎么排：按开始时间 / 按结束时间 / 按加进来的先后。 */
enum class TodoSort(val emoji: String, val label: String, val desc: String) {
    START("\uD83D\uDD50", "按开始", "设了时间的排前面，按开始时间"),
    END("\uD83C\uDFC1", "按结束", "先到期的排前面，按结束时间"),
    ADDED("\uD83D\uDCDD", "按添加", "按加进来的先后")
}

/**
 * 待办：把一天的事按「重不重要 / 急不急」分进四格。
 *
 * 摆位：左上最要紧 → 右上第二 → 左下第三 → 右下最末，
 * 四格各有底色，越要紧的那格颜色越醒目。
 * 跨天的待办（比如今晚 22:00 到明早 08:00）会在跨到的每一天清单上都出现。
 */
@Composable
fun TodoScreen(
    todos: List<TodoItem>,
    dateKey: String,
    onDateChange: (String) -> Unit,
    /** 加一条：内容、格子、开始时间、结束时间（时间给 0 就是不限时间）。 */
    onAdd: (String, Int, Long, Long) -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (TodoItem, String, Long, Long) -> Unit,
    onDelete: (TodoItem) -> Unit,
    onMove: (TodoItem, Int) -> Unit,
    onClearDone: () -> Unit
) {
    val c = LocalAppColors.current
    val today = DateKey.fromMillis(System.currentTimeMillis())
    // 这一天要显示的待办：没被删掉的，加上跨天覆盖到这一天的
    val mine = remember(todos, dateKey) {
        todos.filter { !it.deleted && it.covers(dateKey) }
    }
    var sort by remember { mutableIntStateOf(0) }
    val sorted = remember(mine, sort) { sortTodos(mine, TodoSort.entries[sort]) }
    val total = mine.size
    val done = mine.count { it.done }
    val progress = if (total == 0) 0f else done.toFloat() / total

    // 各个弹窗：加一条 / 改内容 / 挪位置 / 排序 / 清掉做完的
    var addingZone by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<TodoItem?>(null) }
    var moving by remember { mutableStateOf<TodoItem?>(null) }
    var pickingSort by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .padding(top = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 标题 + 完成进度 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "\uD83D\uDDD3 待办清单",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink
                )
                Text(
                    if (total == 0) "把要做的事分进四格，轻重缓急一眼看清"
                    else if (done == total) "都做完了，今天就到这里吧 \uD83C\uDF1F"
                    else "先把左上角那格的事做完",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$done/$total",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done == total && total > 0) c.accent else c.ink
                )
                Text(
                    "完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
        }

        // ── 日期切换 + 排序 + 完成进度条 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateArrow("\u2039") {
                onDateChange(DateKey.fromMillis(DateKey.addDays(DateKey.toMillis(dateKey), -1)))
            }
            Spacer(Modifier.width(6.dp))
            // 换一天时日期这几个字也跟着滑
            AnimatedContent(
                targetState = dateKey,
                transitionSpec = { dateTransition(targetState > initialState) },
                label = "todoDayLabel"
            ) { day ->
                Text(
                    if (day == today) "今天" else DateKey.monthDayLabel(DateKey.toMillis(day)),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
            }
            Spacer(Modifier.width(6.dp))
            DateArrow("\u203A") {
                onDateChange(DateKey.fromMillis(DateKey.addDays(DateKey.toMillis(dateKey), 1)))
            }
            if (dateKey != today) {
                Spacer(Modifier.width(8.dp))
                Pill("回今天") { onDateChange(today) }
            }
            Spacer(Modifier.width(8.dp))
            // 排序方式：点一下换一种排法
            Pill(TodoSort.entries[sort].emoji + " " + TodoSort.entries[sort].label) {
                pickingSort = true
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(c.line)
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(c.accent)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            if (done > 0) {
                Text(
                    "清掉已做",
                    modifier = Modifier.clickable { clearing = true },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
        }

        // ── 四格：左上最要紧 → 右上 → 左下 → 右下最末 ──
        // 换一天时四格一起滑着换：往后一天往左滑，往前一天往右滑
        AnimatedContent(
            targetState = dateKey,
            transitionSpec = { dateTransition(targetState > initialState) },
            modifier = Modifier.weight(1f),
            label = "todoDay"
        ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TodoQuad(
                zone = 0,
                items = sorted.filter { it.zone == 0 },
                modifier = Modifier.weight(1f),
                onAdd = { addingZone = 0 },
                onToggle = onToggle,
                onEdit = { editing = it },
                onMove = { moving = it },
                onDelete = onDelete
            )
            TodoQuad(
                zone = 1,
                items = sorted.filter { it.zone == 1 },
                modifier = Modifier.weight(1f),
                onAdd = { addingZone = 1 },
                onToggle = onToggle,
                onEdit = { editing = it },
                onMove = { moving = it },
                onDelete = onDelete
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TodoQuad(
                zone = 2,
                items = sorted.filter { it.zone == 2 },
                modifier = Modifier.weight(1f),
                onAdd = { addingZone = 2 },
                onToggle = onToggle,
                onEdit = { editing = it },
                onMove = { moving = it },
                onDelete = onDelete
            )
            TodoQuad(
                zone = 3,
                items = sorted.filter { it.zone == 3 },
                modifier = Modifier.weight(1f),
                onAdd = { addingZone = 3 },
                onToggle = onToggle,
                onEdit = { editing = it },
                onMove = { moving = it },
                onDelete = onDelete
            )
        }
        }
        }
    }

    addingZone?.let { zone ->
        TodoEditDialog(
            title = "加进「${TodoZone.of(zone).label}」",
            initialText = "",
            dayMillis = DateKey.toMillis(dateKey),
            initialStart = 0L,
            initialEnd = 0L,
            accent = Color(TodoZone.of(zone).colorHex),
            onConfirm = { text, start, end ->
                addingZone = null
                if (text.isNotBlank()) onAdd(text, zone, start, end)
            },
            onDismiss = { addingZone = null }
        )
    }

    editing?.let { item ->
        TodoEditDialog(
            title = "改一下",
            initialText = item.text,
            dayMillis = DateKey.toMillis(item.startKey().ifBlank { dateKey }),
            initialStart = item.startAt,
            initialEnd = item.endAt,
            accent = Color(TodoZone.of(item.zone).colorHex),
            onConfirm = { text, start, end ->
                editing = null
                if (text.isNotBlank()) onEdit(item, text, start, end)
            },
            onDismiss = { editing = null }
        )
    }

    moving?.let { item ->
        TodoMoveDialog(
            current = item.zone,
            onPick = { zone ->
                moving = null
                if (zone != item.zone) onMove(item, zone)
            },
            onDismiss = { moving = null }
        )
    }

    if (pickingSort) {
        TodoSortDialog(
            current = sort,
            onPick = { sort = it; pickingSort = false },
            onDismiss = { pickingSort = false }
        )
    }

    if (clearing) {
        ConfirmDialog(
            title = "清掉已做完的",
            message = "把这天已经做完的 $done 条收走（放进回收站，还能找回），没做完的留着。",
            confirmText = "清掉",
            onConfirm = {
                clearing = false
                onClearDone()
            },
            onDismiss = { clearing = false }
        )
    }
}

/** 按选择的排法把待办排一排；没设时间的都排在后面。 */
private fun sortTodos(list: List<TodoItem>, sort: TodoSort): List<TodoItem> = when (sort) {
    // 按开始时间：先开始的在前
    TodoSort.START -> list.sortedWith(
        compareBy<TodoItem> { if (it.hasRange()) 0 else 1 }
            .thenBy { if (it.hasRange()) it.startAt else 0L }
            .thenBy { it.createdAt }
    )

    // 按结束时间：先到期的在前
    TodoSort.END -> list.sortedWith(
        compareBy<TodoItem> { if (it.hasRange()) 0 else 1 }
            .thenBy { if (it.hasRange()) it.endAt else 0L }
            .thenBy { it.createdAt }
    )

    TodoSort.ADDED -> list.sortedBy { it.createdAt }
}

/** 四格里的一格：底色越要紧越醒目，右上角加一条，里面列出待办。 */
@Composable
private fun TodoQuad(
    zone: Int,
    items: List<TodoItem>,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (TodoItem) -> Unit,
    onMove: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit
) {
    val c = LocalAppColors.current
    val z = TodoZone.of(zone)
    val tint = Color(z.colorHex)
    val done = items.count { it.done }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.15f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        // 格子的头：图标 + 名字 + 数量 + 加一条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(z.emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    z.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
                Text(
                    z.desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
            if (items.isNotEmpty()) {
                Text(
                    "$done/${items.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tint
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.25f))
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Text("＋", fontSize = 15.sp, color = c.ink)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(tint.copy(alpha = 0.3f))
        )

        // 格子里的待办，多了可以上下滚
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "点右上＋加一条",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items.forEach { item ->
                    TodoRow(
                        item = item,
                        tint = tint,
                        onToggle = { onToggle(item.id) },
                        onEdit = { onEdit(item) },
                        onMove = { onMove(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }
}

/** 一条待办：左边勾选、中间内容（点它改字）、右边挪位置和删掉。 */
@Composable
private fun TodoRow(
    item: TodoItem,
    tint: Color,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val c = LocalAppColors.current
    // 记下这段文字排了几行、每行在哪，好自己画删除线
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (item.done) tint else Color.Transparent)
                .border(
                    1.5.dp,
                    if (item.done) tint else c.line,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.done) {
                Text("✓", fontSize = 11.sp, color = Color.White)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.text,
                modifier = Modifier
                    // 做完了就划掉，并且再淡一点
                    .then(if (item.done) Modifier.alpha(0.55f) else Modifier)
                    .clickable(onClick = onEdit)
                    // 自己逐行画删除线：系统那条遇到换行的文字常常只划第一行
                    .drawWithContent {
                        drawContent()
                        if (!item.done) return@drawWithContent
                        val layout = textLayout ?: return@drawWithContent
                        val stroke = 1.2.dp.toPx()
                        for (i in 0 until layout.lineCount) {
                            // 划在这一行字的正中间
                            val y = (layout.getLineTop(i) + layout.getLineBottom(i)) / 2f
                            drawLine(
                                color = c.ink,
                                start = Offset(0f, y),
                                end = Offset(layout.getLineRight(i), y),
                                strokeWidth = stroke
                            )
                        }
                    },
                style = MaterialTheme.typography.bodySmall,
                color = if (item.done) c.inkSoft else c.ink,
                onTextLayout = { textLayout = it },
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            // 设了时间范围就在下面标一下；跨几天的还会说清楚
            timeLabel(item)?.let { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        RowIcon("⇄", onClick = onMove)
        RowIcon("✕", onClick = onDelete)
    }
}

/** 时间范围那一小行字：同一天就 09:00–11:00，跨天就把日期带上。 */
private fun timeLabel(item: TodoItem): String? {
    if (!item.hasRange()) return null
    val s = hmOf(item.startAt)
    val e = hmOf(item.endAt)
    return if (item.startKey() == item.endKey()) {
        "$s–$e · ${durationText(item.endAt - item.startAt)}"
    } else {
        val days = item.spanDays()
        "${DateKey.dayOfMonth(item.startAt)}日$s → " +
            "${DateKey.dayOfMonth(item.endAt)}日$e · 跨 $days 天"
    }
}

/** 毫秒差说成人话：2 小时 30 分 / 45 分钟。 */
private fun durationText(millis: Long): String {
    val total = (millis / 60000L).coerceAtLeast(1)
    val h = total / 60
    val m = total % 60
    return when {
        h == 0L -> "$m 分钟"
        m == 0L -> "$h 小时"
        else -> "$h 小时 $m 分"
    }
}

/** 行尾的小图标按钮。 */
@Composable
private fun RowIcon(text: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = c.inkSoft)
    }
}

/** 一行里的小胶囊按钮（回今天、排序方式）。 */
@Composable
private fun Pill(label: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.cardAlt)
            .border(1.dp, c.line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
    }
}

/** 日期左边 / 右边的箭头。 */
@Composable
private fun DateArrow(label: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.cardAlt)
            .border(1.dp, c.line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, color = c.ink)
    }
}

/** 把毫秒格式化成 HH:mm。 */
private fun hmOf(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return String.format(
        Locale.US,
        "%02d:%02d",
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE)
    )
}

/** 这一天的 0 点，算时间范围时当基准。 */
private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** 把一条待办挪到别的格子。 */
@Composable
private fun TodoMoveDialog(
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "挪到哪一格",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TodoZone.entries.forEachIndexed { i, z ->
                    val tint = Color(z.colorHex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tint.copy(alpha = 0.15f))
                            .clickable { onPick(i) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(z.emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                z.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.ink
                            )
                            Text(
                                z.desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft
                            )
                        }
                        if (i == current) {
                            Text(
                                "在这",
                                style = MaterialTheme.typography.labelSmall,
                                color = tint
                            )
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

/** 选清单里每条怎么排。 */
@Composable
private fun TodoSortDialog(
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "怎么排",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TodoSort.entries.forEachIndexed { i, s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (i == current) c.accentSoft else Color.Transparent
                            )
                            .clickable { onPick(i) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                s.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.ink
                            )
                            Text(
                                s.desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft
                            )
                        }
                        if (i == current) {
                            Text(
                                "✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.accent
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("好了", color = c.inkSoft) }
        }
    )
}

/**
 * 写一条待办：内容 + 时间范围（可选）。
 * 结束时间比开始早、或者结束定在后面几天，就是跨天的那种待办。
 */
@Composable
private fun TodoEditDialog(
    title: String,
    initialText: String,
    /** 清单这一天的 0 点，用来算时间。 */
    dayMillis: Long,
    initialStart: Long,
    initialEnd: Long,
    accent: Color,
    onConfirm: (String, Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    var text by remember { mutableStateOf(initialText) }
    var start by remember { mutableLongStateOf(initialStart) }
    var end by remember { mutableLongStateOf(initialEnd) }
    // 正在给「开始 / 结束」选哪一天（0 = 开始，1 = 结束）
    var dateField by remember { mutableStateOf<Int?>(null) }
    // 日期选好了、正等着选几点的那一天
    var draftDay by remember { mutableLongStateOf(0L) }
    var timeField by remember { mutableStateOf<Int?>(null) }

    val day0 = remember(dayMillis) { startOfDay(dayMillis) }
    val hasRange = start > 0L

    // 有了开始时间就一定得有结束时间：没改过就默认往后 2 小时
    fun applyStart(value: Long) {
        // 原来那段时长留着，改开始时间时结束跟着平移
        val keep = if (start > 0L && end > start) end - start else DEFAULT_DURATION
        start = value
        end = value + keep
    }

    fun applyEnd(value: Long) {
        // 结束不能早于开始，早了就按默认时长往后排
        end = if (value > start) value else start + DEFAULT_DURATION
    }

    AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("要做什么？", color = c.inkSoft) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = c.line
                    )
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "\uD83D\uDD52 时间范围（选填）",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.ink
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeBox(
                        label = "开始",
                        value = if (start > 0L) dateTimeText(start) else "不设",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { dateField = 0 }
                    TimeBox(
                        label = "结束",
                        value = if (end > 0L) dateTimeText(end) else "跟着开始走",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { dateField = 1 }
                }

                // 懒得一天天点的话，这里按开始时间直接给个时长
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(30L, 60L, 120L, 1440L).forEach { minutes ->
                        val quick = when {
                            minutes >= 1440 -> "1天"
                            minutes >= 60 -> "${minutes / 60}小时"
                            else -> "${minutes}分"
                        }
                        Pill(quick) {
                            val base = if (start > 0L) start else day0 + 9 * 3600 * 1000L
                            if (start <= 0L) {
                                applyStart(base)
                            }
                            end = base + minutes * 60 * 1000L
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    if (hasRange) {
                        "持续 ${durationText(end - start)}"
                    } else {
                        "不设时间就只按格子排"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
                if (hasRange) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "不设时间",
                        modifier = Modifier.clickable { start = 0L; end = 0L },
                        style = MaterialTheme.typography.labelSmall,
                        color = c.accent
                    )
                }
            }
        },
        confirmButton = {
            Pill(text = "确定", accent = accent, filled = true) {
                val s = if (start > 0L) start else 0L
                // 有开始就一定要有结束，这里再兜一次底
                val e = if (start > 0L) {
                    if (end > start) end else start + DEFAULT_DURATION
                } else 0L
                onConfirm(text.trim(), s, e)
            }
        },
        dismissButton = {
            Pill(text = "取消", accent = accent, filled = false, onClick = onDismiss)
        }
    )

    // 先选哪一天，选完接着选几点
    dateField?.let { field ->
        val base = if (field == 0) {
            if (start > 0L) start else day0
        } else {
            if (end > 0L) end else (if (start > 0L) start else day0)
        }
        DatePickDialog(
            title = if (field == 0) "开始：哪天" else "结束：哪天",
            initialMillis = base,
            onConfirm = { day ->
                draftDay = day
                dateField = null
                timeField = field
            },
            onDismiss = { dateField = null }
        )
    }

    timeField?.let { field ->
        val base = if (field == 0) start else end
        TimePickDialog(
            title = if (field == 0) "开始：几点" else "结束：几点",
            initial = if (base > 0L) hmOf(base) else "09:00",
            onConfirm = { hm ->
                val (h, m) = hm.split(":").let { it[0].toInt() to it[1].toInt() }
                val value = draftDay + (h * 60 + m) * 60 * 1000L
                if (field == 0) applyStart(value) else applyEnd(value)
                timeField = null
            },
            onDismiss = { timeField = null }
        )
    }
}

/** 把时间说全：10月18日 09:00。 */
private fun dateTimeText(millis: Long): String =
    "${DateKey.monthDayLabel(millis)} ${hmOf(millis)}"

/** 时间范围里的一颗「开始 / 结束」按钮。 */
@Composable
private fun TimeBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.cardAlt)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = accent)
    }
}

/** 弹窗底下的胶囊按钮，确定和取消一样高。 */
@Composable
private fun Pill(
    text: String,
    accent: Color,
    filled: Boolean,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) accent else c.cardAlt)
            .then(if (filled) Modifier else Modifier.border(1.dp, c.line, RoundedCornerShape(50)))
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
