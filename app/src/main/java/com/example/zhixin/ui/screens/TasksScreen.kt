package com.example.zhixin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import com.example.zhixin.ui.components.CheckInSuccessDialog
import com.example.zhixin.ui.components.SideConfetti
import com.example.zhixin.ui.components.ConfirmDialog
import com.example.zhixin.ui.components.DayRecordsDialog
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.theme.LocalAppColors
import java.util.Calendar

/** 添加任务时可选的 emoji。 */
val TASK_EMOJIS = listOf(
    "\uD83D\uDCA7", "\uD83D\uDCDA", "🏃", "\uD83E\uDDD8", "🍎", "\uD83D\uDCA4",
    "\uD83E\uDDF9", "✍\uFE0F", "\uD83C\uDFA7", "\uD83C\uDF3F", "☕", "\uD83C\uDFAF",
    "\uD83C\uDFB9", "\uD83C\uDFA8", "\uD83C\uDFCA", "\uD83D\uDC8A", "\uD83E\uDDF4",
    "\uD83C\uDF19", "\uD83D\uDEB6", "\uD83E\uDDE0"
)

// 最开始那几个颜色的位置不能动 —— 之前挑过颜色的任务存的是下标，挪了会变色
private val CLASSIC_COLORS = listOf(
    0xFF7FBFA8L, 0xFFF0A24CL, 0xFF9BB6D6L, 0xFFE38B8BL, 0xFFB69DCDL, 0xFF8FBF6FL
)

// 后面按色系追加：清新绿 → 暖阳橙 → 温柔粉 → 晴空蓝 → 安静紫 → 大地灰
private val FRESH_COLORS = listOf(
    0xFF6FA86FL, 0xFF5E9E7FL, 0xFFA8C96AL, 0xFFBBD68FL, 0xFF9FD6A8L, 0xFFC2DFA8L, 0xFF7FBFB8L
)
private val WARM_COLORS = listOf(
    0xFFE9B65EL, 0xFFF3C96BL, 0xFFD98A4EL, 0xFFF5D08AL, 0xFFEFA76AL, 0xFFC98A4BL, 0xFFF0A070L
)
private val PINK_COLORS = listOf(
    0xFFF3A6A6L, 0xFFD98CA8L, 0xFFEFA8C1L, 0xFFC9737AL, 0xFFF2B8BEL, 0xFFC98BB0L, 0xFFE8A0B4L
)
private val SKY_COLORS = listOf(
    0xFF6FA8C9L, 0xFF8FB6DEL, 0xFF5E9EC9L, 0xFFA8CFE3L, 0xFF6F8FD6L, 0xFF86C7D6L, 0xFF7F9FD6L
)
private val PURPLE_COLORS = listOf(
    0xFF8F9BD6L, 0xFF9B8FD6L, 0xFFA78FD6L, 0xFF7F7FB6L, 0xFF8277C9L, 0xFFC9A8DEL, 0xFFAE86C9L
)
private val EARTH_COLORS = listOf(
    0xFFBF9A7FL, 0xFFA88F7FL, 0xFFC9B9A8L, 0xFF8F8F8FL, 0xFFB0A08FL, 0xFFD6C8B8L, 0xFF7F8F9BL
)

/**
 * 每个任务卡片的主题色（按顺序轮换），打卡按钮 / 月历 / 图标底色共用。
 * 一共 49 个，已经很多了，所以界面上按色系分组摆。
 */
val TASK_COLORS: List<Long> = CLASSIC_COLORS + FRESH_COLORS + WARM_COLORS +
    PINK_COLORS + SKY_COLORS + PURPLE_COLORS + EARTH_COLORS

/** 挑色时按色系分组：一组名字 + 它在 TASK_COLORS 里的那些下标。 */
val TASK_COLOR_GROUPS: List<Pair<String, List<Int>>> = buildList {
    add("经典" to CLASSIC_COLORS.indices.toList())
    var base = CLASSIC_COLORS.size
    listOf(
        "\uD83C\uDF3F 清新" to FRESH_COLORS,
        "\uD83C\uDF31 暖阳" to WARM_COLORS,
        "\uD83C\uDF38 温柔" to PINK_COLORS,
        "\uD83D\uDCA7 晴空" to SKY_COLORS,
        "\uD83C\uDF19 安静" to PURPLE_COLORS,
        "\uD83C\uDFEB 大地" to EARTH_COLORS
    ).forEach { (label, colors) ->
        add(label to (base until base + colors.size).toList())
        base += colors.size
    }
}

/**
 * 打卡成功后的随机激励语。任务里自己写过那句（note）就用写的，
 * 没写就从这里随手抽一句，每次不太一样。
 */
val CHECKIN_CHEERS = listOf(
    "又坚持了一天，慢慢来就好 ✨",
    "今天也很棒，一点点来就很好 🌱",
    "做了一点，就比昨天多一点 💪",
    "认真生活的样子很好看 ☀️",
    "小步不停，也算走了很远 🛤️",
    "这条记下了，回头看会很有力量 📖",
    "今天没有白过 🌙",
    "已经很好了，不用跟谁比 🍃",
    "又攒下一点小小的成就感 🎈",
    "坚持从来靠的不是狠劲，是记得 🌸",
    "一件小事做完了，可以松口气啦 ☕",
    "给自己记一功 🌟",
    "日子就是这样一点点攒起来的 🧺",
    "今天的你，比昨天更熟练了一点 🎐"
)

/** 任务在列表中的位置决定它的主题色。 */
fun taskAccentColor(index: Int): Color = Color(TASK_COLORS[index % TASK_COLORS.size])

/** 任务的主题色：用户挑过（colorIndex >= 0）就用挑的，否则按列表位置轮换。 */
fun taskColor(item: CustomItem, position: Int): Color {
    val i = if (item.colorIndex >= 0) item.colorIndex else position
    return Color(TASK_COLORS[i % TASK_COLORS.size])
}

/** 单个任务的统计信息。 */
private data class TaskStat(
    val count: Int,        // 累计打卡次数
    val days: Int,         // 打卡过的天数
    val streak: Int,       // 连续打卡天数
    val doneDaysThisMonth: Set<Int>, // 本月打过卡的"日"
    val todayCount: Int,   // 今天打卡了几次
    val doneToday: Boolean // 今天是否达成目标次数
)

/** 从今天（或昨天）往前数连续打卡天数。 */
fun calcStreak(dayKeys: Set<String>, today: String): Int {
    var cursor = today
    if (!dayKeys.contains(cursor)) {
        cursor = DateKey.fromMillis(DateKey.addDays(DateKey.toMillis(today), -1))
    }
    var s = 0
    while (dayKeys.contains(cursor)) {
        s++
        cursor = DateKey.fromMillis(DateKey.addDays(DateKey.toMillis(cursor), -1))
    }
    return s
}

/** 卡片状态文案：目标 1 次显示「已打卡 / 未打卡」，多次显示「今日 x/y」。 */
fun taskStatusText(target: Int, todayCount: Int, done: Boolean): String =
    if (target > 1) {
        if (done) "今日 $todayCount/$target 已完成 ✓" else "今日 $todayCount/$target"
    } else {
        if (done) "已打卡 ✓" else "未打卡"
    }

/** 今天这个任务的打卡里，有没有哪条是写了内容的（不只是打了卡）。 */
private fun wroteSomething(entries: List<Entry>, item: CustomItem): Boolean {
    val auto = item.autoText()
    return entries.any {
        !it.deleted && it.dateKey == DateKey.fromMillis(System.currentTimeMillis()) &&
            it.tags.contains(CUSTOM_TAG) && it.tags.contains(item.name) &&
            it.text != auto
    }
}

/**
 * 任务打卡主页（参考习惯打卡样式）：
 * - 顶部：标题 + 今日完成徽章 + 「＋」添加任务
 * - 每个任务一张卡：emoji + 名称 + 状态 + 圆形「打卡」按钮
 *   + 左下「数量 / 天数 / 连续」统计 + 右下该任务专属迷你月历
 * - 长按卡片删除任务；右上角「＋」弹出添加任务弹窗
 */
@Composable
fun TasksScreen(
    entries: List<Entry>,
    settings: AppSettings,
    /** 打卡：带上弹窗里写的内容，返回 true 表示真的打上了。 */
    onQuickToggle: (CustomItem, String) -> Boolean,
    onDeleteCustom: (CustomItem) -> Unit,
    /** 点卡片 → 打开该任务的设置页。 */
    onOpenDetail: (CustomItem) -> Unit,
    /** 右上角「＋」→ 打开添加任务页。 */
    onOpenAdd: () -> Unit,
    /** 右上角「🔥」→ 打开打卡热度日历。 */
    onOpenHeat: () -> Unit,
    /** 改某天打卡里写的内容（只改文字，打卡本身不动）。 */
    onEditRecord: (Entry, String) -> Unit,
    /** 成功弹窗里补写的那句话，记到刚打的那条上。 */
    onQuickNote: (CustomItem, String) -> Unit
) {
    val c = LocalAppColors.current
    val now = System.currentTimeMillis()
    val today = DateKey.fromMillis(now)
    val viewYear = DateKey.yearOf(now)
    val viewMonth = DateKey.monthOf(now)

    // 任务打卡记录（tags 含 CUSTOM_TAG），与心情 / 日记等其它打卡分开
    val taskEntries = remember(entries) {
        entries.filter { !it.deleted && it.tags.contains(CUSTOM_TAG) }
    }

    val stats = remember(taskEntries, settings, viewYear, viewMonth, today) {
        settings.customItems.associate { item ->
            val mine = taskEntries.filter { it.tags.contains(item.name) }
            val dayKeys = mine.map { it.dateKey }.toSet()
            val doneDays = mine.filter {
                DateKey.yearOf(it.time) == viewYear &&
                    DateKey.monthOf(it.time) == viewMonth
            }.map { DateKey.dayOfMonth(it.time) }.toSet()
            val todayCount = mine.count { it.dateKey == today }
            item.name to TaskStat(
                count = mine.size,
                days = dayKeys.size,
                streak = calcStreak(dayKeys, today),
                doneDaysThisMonth = doneDays,
                todayCount = todayCount,
                doneToday = todayCount >= item.target()
            )
        }
    }

    val doneTodayCount = settings.customItems.count { stats[it.name]?.doneToday == true }
    val totalCount = settings.customItems.size

    var pendingDelete by remember { mutableStateOf<CustomItem?>(null) }
    // 一点打卡就打上了，接着弹窗说成功，里面还能补一句
    var successItem by remember { mutableStateOf<CustomItem?>(null) }
    // 每次打卡成功 +1，用来重新放一次礼花
    var confettiId by remember { mutableIntStateOf(0) }
    // 已经打过卡了，再点一次要先问要不要撤
    var undoItem by remember { mutableStateOf<CustomItem?>(null) }
    // 日历里点开的那一天（哪个任务 + 几号）
    var dayDialog by remember { mutableStateOf<Pair<CustomItem, Int>?>(null) }
    // 打卡后浮出来的激励语，过几秒自己消失
    var cheerText by remember { mutableStateOf<String?>(null) }
    // 上次抽到第几句，尽量避免连着两次抽到同一句
    var lastCheer by remember { mutableIntStateOf(-1) }
    fun pickCheer(item: CustomItem): String {
        if (item.note.isNotBlank()) return item.note.trim()
        var i = CHECKIN_CHEERS.indices.random()
        if (i == lastCheer) i = (i + 1) % CHECKIN_CHEERS.size
        lastCheer = i
        return CHECKIN_CHEERS[i]
    }
    LaunchedEffect(cheerText) {
        if (cheerText != null) {
            delay(2800)
            cheerText = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 12.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 顶部：标题 + 完成度徽章 + 添加（高度和日日新 / 心情日记对齐）──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "\uD83C\uDFAF 任务打卡",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.cardAlt)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "✅ $doneTodayCount/$totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink
                    )
                }
                Spacer(Modifier.width(10.dp))
                // 热度日历：在「＋」左边
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(1.dp, c.line, CircleShape)
                        .combinedClickable(onClick = onOpenHeat),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\uD83D\uDD25", fontSize = 14.sp)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(1.dp, c.line, CircleShape)
                        .combinedClickable(onClick = onOpenAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋", fontSize = 17.sp, color = c.ink)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "慢慢来，坚持就好 ✨",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkSoft
            )
        }

        // ── 任务卡列表 ──
        if (settings.customItems.isEmpty()) {
            item {
                SoftCard {
                    EmptyHint(
                        "\uD83C\uDFAF",
                        "点右上「＋」添加第一个任务，打卡从今天开始"
                    )
                }
            }
        } else {
            itemsIndexed(settings.customItems, key = { _, it -> it.name }) { index, item ->
                val st = stats[item.name] ?: TaskStat(0, 0, 0, emptySet(), 0, false)
                val accent = taskColor(item, index)
                val mine = remember(taskEntries, item.name) {
                    taskEntries.filter { it.tags.contains(item.name) }
                }
                TaskCard(
                    item = item,
                    stat = st,
                    year = viewYear,
                    month = viewMonth,
                    todayDay = DateKey.dayOfMonth(now),
                    accent = accent,
                    onOpen = { onOpenDetail(item) },
                    onToggle = {
                        if (st.doneToday) {
                            // 今天已经打过了，先问一句再撤
                            undoItem = item
                        } else if (onQuickToggle(item, "")) {
                            // 先打上卡；开了「打卡内容编辑」的才弹窗，关了的只浮一句激励语
                            if (item.noteEditable) successItem = item
                            cheerText = pickCheer(item)
                            // 两边放个礼花
                            confettiId++
                        }
                    },
                    onLongPress = { pendingDelete = item },
                    onDayClick = { day -> dayDialog = item to day }
                )
            }
        }

        item {
            Text(
                "每个任务有自己的日历，坚持会看得见 ✨",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

        // 打卡成功：两边喷礼花（在最上层，不挡操作）
        SideConfetti(
            runId = confettiId,
            modifier = Modifier.fillMaxSize()
        )

        // 打卡后从下面浮出来的小框：说句激励的话，几秒后自己消失
        AnimatedVisibility(
            visible = cheerText != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.card)
                    .border(1.dp, c.line, RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    "\uD83C\uDF1F ${cheerText.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.ink,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    dayDialog?.let { (item, day) ->
        val key = DateKey.fromMillis(
            Calendar.getInstance().apply {
                clear()
                set(viewYear, viewMonth, day)
            }.timeInMillis
        )
        val records = taskEntries.filter {
            it.tags.contains(item.name) && it.dateKey == key
        }
        DayRecordsDialog(
            dateLabel = "${viewMonth + 1}月${day}日",
            emoji = item.emoji,
            name = item.name,
            accent = taskColor(item, settings.customItems.indexOf(item).coerceAtLeast(0)),
            records = records,
            onSave = { changes -> changes.forEach { (e, t) -> onEditRecord(e, t) } },
            onDismiss = { dayDialog = null }
        )
    }

    undoItem?.let { item ->
        // 没写内容的撤销了就直接清掉，写了的才需要留着能找回
        val wrote = wroteSomething(entries, item)
        ConfirmDialog(
            title = "撤销打卡",
            message = if (wrote) {
                "「${item.name}」今天已经打过了，还写了内容。撤销会把打卡和写的内容一起放进回收站，想找回来还能在回收站里找回。"
            } else {
                "「${item.name}」今天已经打过了。这次没写内容，撤销后直接清掉，不会留在回收站里。"
            },
            confirmText = "撤销",
            onConfirm = {
                undoItem = null
                onQuickToggle(item, "")
                cheerText = if (wrote) {
                    "已撤销「${item.name}」，记录放进回收站了"
                } else {
                    "已撤销「${item.name}」，这次打卡清掉了"
                }
            },
            onDismiss = { undoItem = null }
        )
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "删除任务",
            message = "要删掉「${item.name}」吗？已经打过的记录会保留。",
            confirmText = "删除",
            onConfirm = {
                onDeleteCustom(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    successItem?.let { item ->
        CheckInSuccessDialog(
            subtitle = "${item.emoji} ${item.name}",
            onConfirm = { note ->
                successItem = null
                if (note.isNotBlank()) onQuickNote(item, note)
            },
            onDismiss = { successItem = null }
        )
    }
}

/** 一个任务的大卡片：头部（图标 / 名称 / 打卡钮）+ 底部（统计 / 迷你月历）。 */
@Composable
private fun TaskCard(
    item: CustomItem,
    stat: TaskStat,
    year: Int,
    month: Int,
    todayDay: Int,
    accent: Color,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    /** 点迷你月历上的某一天 → 看 / 改那天写的内容。 */
    onDayClick: (Int) -> Unit
) {
    val c = LocalAppColors.current
    SoftCard {
        Column(
            modifier = Modifier.combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress
            )
        ) {
            // ── 头部 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink
                    )
                    Text(
                        taskStatusText(item.target(), stat.todayCount, stat.doneToday),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stat.doneToday) accent else c.inkSoft
                    )
                }
                // 圆形打卡按钮
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (stat.doneToday) accent.copy(alpha = 0.16f) else accent
                        )
                        .combinedClickable(onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (stat.doneToday) "✓" else "打卡",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (stat.doneToday) accent else Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── 底部：左统计 / 右迷你月历 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(0.42f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat("数量", "${stat.count}")
                    MiniStat("天数", "${stat.days}")
                    MiniStat("连续", "${stat.streak}")
                }
                MiniMonth(
                    year = year,
                    month = month,
                    doneDays = stat.doneDaysThisMonth,
                    todayDay = todayDay,
                    accent = accent,
                    onDayClick = onDayClick,
                    modifier = Modifier.weight(0.58f)
                )
            }
        }
    }
}

/** 小统计：数值 + 标签。 */
@Composable
private fun MiniStat(label: String, value: String) {
    val c = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = c.ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
    }
}

/** 任务专属迷你月历：打过卡的日期填任务色，今天描边。 */
@Composable
private fun MiniMonth(
    year: Int,
    month: Int,
    doneDays: Set<Int>,
    todayDay: Int,
    accent: Color,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    val cal = remember(year, month) {
        Calendar.getInstance().apply {
            clear()
            set(year, month, 1)
        }
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // 周一开头：DAY_OF_WEEK 周日=1 → 周一空 0 格，周日空 6 格
    val leading = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val cells: List<Int?> = List(leading) { null } + (1..daysInMonth).toList()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    MiniDayCell(
                        day = day,
                        done = day != null && doneDays.contains(day),
                        isToday = day == todayDay,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = { day?.let { onDayClick(it) } }
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MiniDayCell(
    day: Int?,
    done: Boolean,
    isToday: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            val borderModifier = if (isToday && !done) {
                Modifier.border(1.dp, accent, CircleShape)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (done) accent else Color.Transparent)
                    .clickable(onClick = onClick)
                    .then(borderModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$day",
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    color = when {
                        done -> Color.White
                        isToday -> accent
                        else -> c.inkSoft
                    }
                )
            }
        }
    }
}
