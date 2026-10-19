package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.CUSTOM_TAG
import com.example.zhixin.data.CustomItem
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.TASK_CATEGORIES
import com.example.zhixin.data.TASK_DIFFICULTIES
import com.example.zhixin.data.difficultyReward
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.ConfirmDialog
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.InputDialog
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.StatCard
import com.example.zhixin.ui.components.TimePickDialog
import androidx.compose.animation.AnimatedContent
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.dateTransition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 编辑任务页（点任务卡进入），参考习惯类 App 的编辑页：
 *  - 顶部：大图标 + 名称
 *  - 挑选颜色（左右滑动选更多颜色）
 *  - 打卡规则：难度、可打卡时间、完成获得的努力值、每日目标
 *  - 写一句激励自己的话、任务分类
 *  - 坚持情况：统计 + 月历 + 最近打卡
 *  - 底部删除
 */
@Composable
fun TaskDetailScreen(
    item: CustomItem,
    accent: Color,
    entries: List<Entry>,
    /** 保存改后的任务，改完立即生效。 */
    onChange: (CustomItem) -> Unit,
    onToggleToday: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val now = System.currentTimeMillis()
    val today = DateKey.fromMillis(now)
    val target = item.target()
    val reward = item.rewardValue()

    val mine = remember(entries, item.name) {
        entries.filter {
            !it.deleted && it.tags.contains(CUSTOM_TAG) && it.tags.contains(item.name)
        }.sortedByDescending { it.time }
    }
    val doneDayKeys = remember(mine) { mine.map { it.dateKey }.toSet() }
    val todayCount = remember(mine, today) { mine.count { it.dateKey == today } }
    val doneToday = todayCount >= target
    val streak = remember(doneDayKeys, today) { calcStreak(doneDayKeys, today) }
    val doneThisMonth = remember(doneDayKeys, now) {
        val y = DateKey.yearOf(now)
        val m = DateKey.monthOf(now)
        doneDayKeys.count { k ->
            val ms = DateKey.toMillis(k)
            DateKey.yearOf(ms) == y && DateKey.monthOf(ms) == m
        }
    }

    var viewYear by remember { mutableIntStateOf(DateKey.yearOf(now)) }
    var viewMonth by remember { mutableIntStateOf(DateKey.monthOf(now)) }
    // 月历上选中的那天，默认是今天；点别的日期就切过去看那天的打卡内容
    var selectedKey by remember { mutableStateOf(today) }
    val selectedLabel = remember(selectedKey) {
        DateKey.monthDayLabel(DateKey.toMillis(selectedKey))
    }
    val selectedRecords = remember(mine, selectedKey) {
        mine.filter { it.dateKey == selectedKey }.sortedBy { it.time }
    }
    /** 某天的打卡记录（切日期动画里用当前 key 现算，退场那一页才是对的）。 */
    fun recordsOf(key: String) = mine.filter { it.dateKey == key }.sortedBy { it.time }
    var renaming by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    // 顶栏固定在上面，只有下面的内容滚动
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        ScreenTopBar(
            title = "编辑任务",
            subtitle = "改完立即生效",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // ── 图标 + 名称 ──
        SoftCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f))
                        .clickable { renaming = true },
                    contentAlignment = Alignment.Center
                ) { Text(item.emoji, fontSize = 34.sp) }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(c.cardAlt)
                        .border(1.dp, c.line, ChipShape)
                        .clickable { renaming = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    taskStatusText(target, todayCount, doneToday) +
                        if (item.hasWindow()) " · ${item.windowStart}~${item.windowEnd}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (doneToday) accent else c.inkSoft,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = if (doneToday) "已打卡 ✓" else "打卡",
                    enabled = true,
                    onClick = onToggleToday
                )
            }
        }

        // ── 挑选图标 ──
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "\uD83E\uDDE9 挑选图标：",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "往左滑动选择更多图标",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TASK_EMOJIS.chunked(4).forEach { column ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        column.forEach { e ->
                            IconDot(
                                emoji = e,
                                picked = e == item.emoji,
                                accent = accent
                            ) { onChange(item.copy(emoji = e)) }
                        }
                    }
                }
            }
        }

        // ── 挑选颜色 ──
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "\uD83D\uDCD0 挑选颜色：",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "一共 ${TASK_COLORS.size} 种，按色系排好了",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
            Spacer(Modifier.height(12.dp))
            TaskColorPicker(
                picked = item.colorIndex,
                onPick = { onChange(item.copy(colorIndex = it)) }
            )
        }

        DividerLabel("打卡规则")

        // ── 难度 / 打卡时段 / 努力值 / 每日目标 ──
        SoftCard {
            Text(
                "\uD83C\uDFAE 难度",
                style = MaterialTheme.typography.labelMedium,
                color = c.inkSoft
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TASK_DIFFICULTIES.forEach { d ->
                    val picked = d == item.difficulty
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(ChipShape)
                            .background(if (picked) accent else c.cardAlt)
                            .border(1.dp, if (picked) accent else c.line, ChipShape)
                            .clickable {
                                if (!picked) {
                                    onChange(
                                        item.copy(
                                            difficulty = d,
                                            reward = difficultyReward(d)
                                        )
                                    )
                                }
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            d,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (picked) Color.White else c.inkSoft
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            DividerMini(c.line)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "\u23F0 可打卡时间",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink
                    )
                    Text(
                        if (item.hasWindow()) "只能在这个时间段打卡"
                        else "不限时间，随时都能打卡",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
                Spacer(Modifier.width(10.dp))
                if (item.hasWindow()) {
                    Box(
                        modifier = Modifier
                            .clip(ChipShape)
                            .background(c.cardAlt)
                            .clickable {
                                onChange(item.copy(windowStart = "", windowEnd = ""))
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "全天",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(c.cardAlt)
                        .border(1.dp, c.line, ChipShape)
                        .clickable { pickingStart = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        item.windowStart.ifBlank { "开始" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.hasWindow()) c.ink else c.inkSoft
                    )
                }
                Text(
                    " ~ ",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
                Box(
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(c.cardAlt)
                        .border(1.dp, c.line, ChipShape)
                        .clickable { pickingEnd = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        item.windowEnd.ifBlank { "结束" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.hasWindow()) c.ink else c.inkSoft
                    )
                }
            }

            DividerMini(c.line)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "\uD83C\uDFC5 完成将获得努力值",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink
                    )
                    Text(
                        "难度越高，奖励越多",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
                Spacer(Modifier.width(10.dp))
                StepButton("−", enabled = reward > 1) {
                    onChange(item.copy(reward = (reward - 1).coerceIn(1, 10)))
                }
                Text(
                    "${reward}个",
                    modifier = Modifier.width(44.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink,
                    textAlign = TextAlign.Center
                )
                StepButton("＋", enabled = reward < 10) {
                    onChange(item.copy(reward = (reward + 1).coerceIn(1, 10)))
                }
            }

            DividerMini(c.line)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "\uD83C\uDFAF 每日目标",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink
                    )
                    Text(
                        "一天要完成几次（喝水可以设 8 杯）",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
                Spacer(Modifier.width(10.dp))
                StepButton("−", enabled = target > 1) {
                    onChange(item.copy(dailyTarget = (target - 1).coerceIn(1, 20)))
                }
                Text(
                    "$target",
                    modifier = Modifier.width(34.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink,
                    textAlign = TextAlign.Center
                )
                StepButton("＋", enabled = target < 20) {
                    onChange(item.copy(dailyTarget = (target + 1).coerceIn(1, 20)))
                }
            }

            DividerMini(c.line)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "\uD83D\uDCDD 打卡内容编辑",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink
                    )
                    Text(
                        if (item.noteEditable) {
                            "开启：打卡后弹窗，可以写点什么"
                        } else {
                            "关闭：点打卡直接打上，不弹窗"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = item.noteEditable,
                    onCheckedChange = { onChange(item.copy(noteEditable = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = c.line
                    )
                )
            }
        }

        // ── 激励语 ──
        SoftCard {
            Text(
                "✍️ 写一句激励自己的话：",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.cardAlt)
                    .clickable { editingNote = true }
                    .padding(12.dp)
            ) {
                Text(
                    if (item.note.isBlank())
                        "还没写，点这里写一句，比如「早点入睡，就能早起。」"
                    else item.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.note.isBlank()) c.inkSoft else c.ink
                )
            }
        }

        // ── 分类 ──
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "\uD83C\uDFF7 任务分类：",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TASK_CATEGORIES.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            val picked = cat == item.category
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(ChipShape)
                                    .background(if (picked) accent else c.cardAlt)
                                    .border(1.dp, if (picked) accent else c.line, ChipShape)
                                    .clickable { if (!picked) onChange(item.copy(category = cat)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (picked) Color.White else c.inkSoft
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        DividerLabel("坚持的情况")

        // ── 坚持情况 ──
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("\uD83D\uDCC8", "${mine.size}", "累计次数", Modifier.weight(1f))
            StatCard("\uD83D\uDCC5", "${doneDayKeys.size}", "打卡天数", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("\uD83D\uDD25", "$streak", "连续天数", Modifier.weight(1f))
            StatCard("\uD83C\uDF19", "$doneThisMonth", "本月天数", Modifier.weight(1f))
        }

        // ── 这个任务的月历 ──
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthNav("\u2039") { shiftMonth(viewYear, viewMonth, -1)?.let { viewYear = it.first; viewMonth = it.second } }
                Spacer(Modifier.width(8.dp))
                // 年月标题跟着格子一起滑
                AnimatedContent(
                    targetState = viewYear * 12 + viewMonth,
                    transitionSpec = { dateTransition(targetState > initialState) },
                    modifier = Modifier.weight(1f),
                    label = "detailMonthTitle"
                ) { idx ->
                    Text(
                        "${idx / 12}年${idx % 12 + 1}月",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.width(8.dp))
                MonthNav("\u203A") { shiftMonth(viewYear, viewMonth, 1)?.let { viewYear = it.first; viewMonth = it.second } }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                    Text(
                        w,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "点日期看那天的打卡内容",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
            Spacer(Modifier.height(6.dp))
            // 翻月时格子滑着换
            AnimatedContent(
                targetState = viewYear * 12 + viewMonth,
                transitionSpec = { dateTransition(targetState > initialState) },
                label = "detailMonth"
            ) { idx ->
            val cells = remember(idx) { DateKey.monthCells(idx / 12, idx % 12) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { millis ->
                            val inMonth = DateKey.yearOf(millis) == idx / 12 &&
                                DateKey.monthOf(millis) == idx % 12
                            val day = DateKey.dayOfMonth(millis)
                            val key = DateKey.fromMillis(millis)
                            BigDayCell(
                                day = if (inMonth) day else null,
                                done = inMonth && doneDayKeys.contains(key),
                                isToday = inMonth && key == today,
                                selected = inMonth && key == selectedKey,
                                accent = accent,
                                enabled = inMonth,
                                onClick = { if (inMonth) selectedKey = key },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            }
        }

        // ── 选中那天的打卡内容 ──
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "\uD83D\uDCCB $selectedLabel 的打卡",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink,
                    modifier = Modifier.weight(1f)
                )
                if (selectedRecords.isNotEmpty()) {
                    Text(
                        "打了 ${selectedRecords.size} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // 换一天时这段记录滑着换
            AnimatedContent(
                targetState = selectedKey,
                transitionSpec = { dateTransition(targetState > initialState) },
                label = "detailDayRecords"
            ) { day ->
            if (recordsOf(day).isEmpty()) {
                EmptyHint("\uD83C\uDF19", "这天未打卡")
            } else {
                val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                recordsOf(day).forEachIndexed { i, e ->
                    if (i > 0) DividerMini(c.line)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            fmt.format(Date(e.time)),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            recordTextOf(item, e),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink
                        )
                    }
                }
            }
            }
        }

        // ── 删除 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE9A7A2))
                .clickable { confirmingDelete = true }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "删除",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF8C3B35)
            )
        }
        Spacer(Modifier.height(4.dp))
        }
    }

    if (renaming) {
        InputDialog(
            title = "改个名字",
            label = "任务名称",
            initial = item.name,
            onConfirm = { v ->
                renaming = false
                val n = v.trim()
                if (n.isNotBlank()) onChange(item.copy(name = n))
            },
            onDismiss = { renaming = false }
        )
    }

    if (editingNote) {
        InputDialog(
            title = "激励语",
            label = "写一句激励自己的话",
            initial = item.note,
            singleLine = false,
            onConfirm = { v ->
                editingNote = false
                onChange(item.copy(note = v.trim()))
            },
            onDismiss = { editingNote = false }
        )
    }

    if (pickingStart) {
        TimePickDialog(
            title = "几点可以开始打卡？",
            initial = item.windowStart.ifBlank { "08:00" },
            onConfirm = { t ->
                pickingStart = false
                onChange(item.copy(windowStart = t, windowEnd = item.windowEnd.ifBlank { "22:00" }))
                pickingEnd = true
            },
            onDismiss = { pickingStart = false }
        )
    }

    if (pickingEnd) {
        TimePickDialog(
            title = "几点之前要打完？",
            initial = item.windowEnd.ifBlank { "22:00" },
            onConfirm = { t ->
                pickingEnd = false
                onChange(item.copy(windowEnd = t, windowStart = item.windowStart.ifBlank { "08:00" }))
            },
            onDismiss = { pickingEnd = false }
        )
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = "删除任务",
            message = "要删掉「${item.name}」吗？已经打过的记录会保留。",
            confirmText = "删除",
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false }
        )
    }
}

/** 可选图标：选中项用主题色描边打底。 */
@Composable
private fun IconDot(
    emoji: String,
    picked: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (picked) accent.copy(alpha = 0.18f) else c.cardAlt)
            .border(2.dp, if (picked) accent else c.line, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}

/** 带文字的分割线：── 文字 ── */
@Composable
private fun DividerLabel(label: String) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(c.line))
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.labelSmall,
            color = c.inkSoft
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(c.line))
    }
}

/** 细分割线。 */
@Composable
private fun DividerMini(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

/** 加减按钮（每日目标次数 / 努力值）。 */
@Composable
private fun StepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) c.cardAlt else c.cardAlt.copy(alpha = 0.4f))
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 18.sp,
            color = if (enabled) c.ink else c.inkSoft
        )
    }
}

/** 月历上的「上月 / 下月」方块按钮。 */
@Composable
private fun MonthNav(label: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.cardAlt)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 18.sp, color = c.ink)
    }
}

/** 月历里的一个日期格：打过卡的填色，今天描边，选中的再套一圈，可以点。 */
@Composable
private fun BigDayCell(
    day: Int?,
    done: Boolean,
    isToday: Boolean,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    // 选中 / 打卡状态变化时，颜色和边框慢慢过渡，别一下跳过去
    val bg by animateColorAsState(
        targetValue = when {
            done -> accent
            selected -> accent.copy(alpha = 0.16f)
            else -> Color.Transparent
        },
        animationSpec = tween(220),
        label = "dayBg"
    )
    val ringWidth by animateDpAsState(
        targetValue = when {
            selected -> 2.dp
            isToday && !done -> 1.dp
            else -> 0.dp
        },
        animationSpec = tween(220),
        label = "dayRing"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            done -> Color.White
            selected || isToday -> accent
            else -> c.inkSoft
        },
        animationSpec = tween(220),
        label = "dayText"
    )
    // 选中的那天稍微放大一点，看起来像被拎起来
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = tween(220),
        label = "dayScale"
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CircleShape)
                    .background(bg)
                    .border(ringWidth, accent, CircleShape)
                    .clickable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$day",
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = textColor
                )
            }
        }
    }
}

/** 打卡内容：没写过的那句默认文案，就当成「这次没写」。 */
private fun recordTextOf(item: CustomItem, entry: Entry): String {
    val fallback = item.autoText()
    if (entry.text == fallback) return "（这次只打了卡，没写内容）"
    return entry.text.removePrefix("${item.emoji} ").trim().ifBlank { entry.text }
}

/** 挑颜色的小圆点：color 为 null 表示「自动」，添加任务和编辑任务共用。 */
@Composable
fun ColorDot(
    color: Color?,
    picked: Boolean,
    onClick: () -> Unit,
    size: Dp = 38.dp
) {
    val c = LocalAppColors.current
    val fill = color ?: c.inkSoft.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(
                if (picked) 2.dp else 1.dp,
                if (picked) c.ink else c.line,
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (color == null) {
            Text("A", fontSize = 14.sp, color = c.ink)
        }
    }
}

private fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month, 1)
        add(Calendar.MONTH, delta)
    }
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
}
