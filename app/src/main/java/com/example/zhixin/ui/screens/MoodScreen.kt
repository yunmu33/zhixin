package com.example.zhixin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.zhixin.data.Category
import com.example.zhixin.data.AudioStore
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.ImageStore
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Mood
import com.example.zhixin.ui.components.AudioSection
import com.example.zhixin.ui.components.ImagePickGrid
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.imeAwareBottomPadding
import com.example.zhixin.ui.components.ImageThumb
import com.example.zhixin.ui.components.ImageViewer
import androidx.compose.animation.AnimatedContent
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.dateTransition
import com.example.zhixin.ui.theme.fadeTransition
import com.example.zhixin.ui.theme.moodStickers
import com.example.zhixin.ui.theme.overlayEnter
import com.example.zhixin.ui.theme.popIn
import com.example.zhixin.ui.theme.stickerOf
import java.util.Calendar

/**
 * 底部导航的「心情日记」页：心情和日记融合在一起。
 * - 一整页月历：点某一天 → 圆形心情选择器，点表情记心情，也可以只写文字
 * - 月历上：有心情的日子显示表情，只写了日记的日子显示一个小圆点，今天描边
 */
@Composable
fun MoodScreen(
    entries: List<Entry>,
    onQuickCheckIn: (Mood, String, String, List<String>) -> Unit,
    onUpdateEntry: (Entry) -> Unit,
    /** 向导最后一步写的日记：日期、标题、正文、配图文件名。 */
    onAddDiary: (String, String, String, List<String>, List<String>) -> Unit
) {
    val c = LocalAppColors.current
    val today = DateKey.fromMillis(System.currentTimeMillis())
    // 改标签时选到一半的草稿：回到日记页点「保存」才真的存下去
    var draftMood by remember { mutableStateOf<String?>(null) }
    var draftTags by remember { mutableStateOf<List<String>?>(null) }

    val moodEntries = remember(entries) {
        entries.filter { !it.deleted && it.category == Category.MOOD.key }
    }
    val diaries = remember(entries) {
        entries.filter { !it.deleted && it.category == Category.DIARY.key }
    }
    val moodMap = remember(moodEntries) { moodEntries.associateBy { it.dateKey } }
    val diaryKeys = remember(diaries) { diaries.map { it.dateKey }.toSet() }
    val diaryMap = remember(diaries) {
        diaries.groupBy { it.dateKey }.mapValues { (_, list) -> list.first() }
    }

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    // 点日历 → 圆形心情选择页
    var pickerDate by remember { mutableStateOf<String?>(null) }
    // 选完心情 → 记录小向导（天气 / 事件 / 日记）
    var wizardDate by remember { mutableStateOf<String?>(null) }
    // 这天已经写过日记 → 直接打开看看
    var viewDate by remember { mutableStateOf<String?>(null) }
    // 改标签 → 只显示事件（原因）那一页
    var tagDate by remember { mutableStateOf<String?>(null) }
    // 改标签进行中：先选心情，再选原因
    var tagFlowDate by remember { mutableStateOf<String?>(null) }
    // 日记页是否处于编辑态（提到外面，改标签回来后仍是编辑态）
    var diaryEditing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "\uD83D\uDE0A 心情日记",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.ink
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "今天的你感觉如何呢，也可以随手记两句",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            }
        }

        // ── 月历 ──
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cal = currentMonth
                    // 年月标题跟着格子一起滑
                    AnimatedContent(
                        targetState = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH),
                        transitionSpec = { dateTransition(targetState > initialState) },
                        modifier = Modifier.weight(1f),
                        label = "moodMonthTitle"
                    ) {
                        Text(
                            "%d年%d月".format(it / 12, it % 12 + 1),
                            style = MaterialTheme.typography.titleMedium,
                            color = c.ink
                        )
                    }
                    listOf("\u2039", "\u203A", "今").forEachIndexed { i, label ->
                        if (i > 0) Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.cardAlt)
                                .clickable {
                                    val next = currentMonth.clone() as Calendar
                                    when (label) {
                                        "\u2039" -> next.add(Calendar.MONTH, -1)
                                        "\u203A" -> next.add(Calendar.MONTH, 1)
                                        else -> {
                                            next.timeInMillis = System.currentTimeMillis()
                                        }
                                    }
                                    currentMonth = next
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft,
                                fontSize = 19.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(
                            day,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                // 翻月时格子滑着换：往后翻往左滑，往前翻往右滑
                val monthIndex =
                    currentMonth.get(Calendar.YEAR) * 12 + currentMonth.get(Calendar.MONTH)
                AnimatedContent(
                    targetState = monthIndex,
                    transitionSpec = { dateTransition(targetState > initialState) },
                    label = "moodMonth"
                ) { MoodCalendarGrid(
                    month = currentMonth,
                    moodMap = moodMap,
                    diaryKeys = diaryKeys,
                    todayKey = today
                ) { dateKey ->
                    // 这天已经有日记 → 直接打开看；否则进心情选择
                    if (diaryMap.containsKey(dateKey)) {
                        viewDate = dateKey
                    } else {
                        pickerDate = dateKey
                    }
                } }
            }
        }

    }

    // 覆盖层打开时铺一层拦截，挡住点到后面的日历
    if (pickerDate != null || wizardDate != null || viewDate != null || tagDate != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        )
    }

    pickerDate?.let { dateKey ->
        // 心情选择页滑进来
        Box(modifier = Modifier.fillMaxSize().then(overlayEnter())) {
        // 手机上按返回：退回心情日记，不是退出 App
        BackHandler {
            pickerDate = null
            // 改标签时按返回 → 回到日记页，半路选的作废
            if (tagFlowDate == dateKey) {
                draftMood = null
                draftTags = null
                tagFlowDate = null
                viewDate = dateKey
            }
        }
        MoodCirclePicker(
            dateKey = dateKey,
            existing = moodMap[dateKey],
            onPick = { mood ->
                pickerDate = null
                val existing = moodMap[dateKey]
                if (tagFlowDate == dateKey) {
                    // 改标签路线：先只记在草稿里，日记页点保存才算数
                    draftMood = mood.key
                    tagDate = dateKey
                } else {
                    if (existing != null) {
                        // 已经记过心情 → 只换心情，保留原来的标签和文字
                        onUpdateEntry(
                            existing.copy(mood = mood.key, score = mood.score)
                        )
                    } else {
                        onQuickCheckIn(mood, "", dateKey, listOf())
                    }
                    wizardDate = dateKey
                }
            },
            onClose = {
                pickerDate = null
                // 改标签时按返回 → 回到日记页，半路选的作废
                if (tagFlowDate == dateKey) {
                    draftMood = null
                    draftTags = null
                    tagFlowDate = null
                    viewDate = dateKey
                }
            }
        )
        }
    }

    wizardDate?.let { dateKey ->
        // 记一记向导滑进来
        Box(modifier = Modifier.fillMaxSize().then(overlayEnter())) {
        // 向导第一步再按返回 → 退回心情选择页
        BackHandler {
            wizardDate = null
            pickerDate = dateKey
        }
        MoodWizard(
            dateKey = dateKey,
            existing = moodMap[dateKey],
            mood = moodMap[dateKey]?.mood?.let { Mood.of(it) },
            onUpdateEntry = onUpdateEntry,
            onAddDiary = onAddDiary,
            onBackToMood = {
                wizardDate = null
                pickerDate = dateKey
            },
            onClose = { wizardDate = null }
        )
        }
    }

    viewDate?.let { dateKey ->
        val entry = diaryMap[dateKey]
        if (entry != null) {
            // 看日记这一层也滑进来
            Box(modifier = Modifier.fillMaxSize().then(overlayEnter())) {
            // 编辑态按返回先退出编辑，否则退回日历
            BackHandler {
                if (diaryEditing) diaryEditing = false else viewDate = null
            }
            val moodEntry = moodMap[dateKey]
            DiaryViewScreen(
                dateKey = dateKey,
                entry = entry,
                editing = diaryEditing,
                onEditingChange = {
                    // 退出编辑 = 放弃，改标签的草稿也一起丢掉
                    if (!it) {
                        draftMood = null
                        draftTags = null
                    }
                    diaryEditing = it
                },
                tags = buildList {
                    // 有草稿就先显示草稿，存没存看有没有点保存
                    val saved = draftTags ?: moodEntry?.tags.orEmpty()
                    saved.firstOrNull()?.let { add(it) }
                    val moodKey = draftMood ?: moodEntry?.mood
                    moodKey?.let { Mood.of(it) }?.let { add(it.label) }
                    saved.drop(1).forEach { add(it) }
                },
                onSave = { title, text, images, audios ->
                    onUpdateEntry(
                        entry.copy(
                            summary = title.trim(),
                            text = text.trim(),
                            images = images,
                            audio = audios
                        )
                    )
                    // 改标签选的东西跟着这一下保存一起落到库里
                    if (draftMood != null || draftTags != null) {
                        val moodKey = draftMood
                        if (moodEntry != null) {
                            onUpdateEntry(
                                moodEntry.copy(
                                    mood = moodKey ?: moodEntry.mood,
                                    score = moodKey?.let { Mood.of(it)?.score } ?: moodEntry.score,
                                    tags = draftTags ?: moodEntry.tags
                                )
                            )
                        } else {
                            moodKey?.let { Mood.of(it) }?.let {
                                // 这天还没记过心情，点保存时顺手建一条
                                onQuickCheckIn(it, "", dateKey, draftTags.orEmpty())
                            }
                        }
                        draftMood = null
                        draftTags = null
                    }
                },
                onChangeTags = {
                    viewDate = null
                    // 改标签：先选心情，再选原因
                    tagFlowDate = dateKey
                    pickerDate = dateKey
                },
                onBack = {
                    viewDate = null
                    diaryEditing = false
                }
            )
            }
        }
    }

    tagDate?.let { dateKey ->
        // 改标签那一层也滑进来
        Box(modifier = Modifier.fillMaxSize().then(overlayEnter())) {
        // 改标签流程按返回 → 回到日记页，半路选的作废
        BackHandler {
            tagDate = null
            draftMood = null
            draftTags = null
            tagFlowDate = null
            viewDate = dateKey
        }
        MoodWizard(
            dateKey = dateKey,
            existing = moodMap[dateKey],
            mood = (draftMood ?: moodMap[dateKey]?.mood)?.let { Mood.of(it) },
            onUpdateEntry = onUpdateEntry,
            onAddDiary = onAddDiary,
            onBackToMood = {
                // 天气页返回 → 退回心情页，仍在改标签流程里
                tagDate = null
                tagFlowDate = dateKey
                pickerDate = dateKey
            },
            tagFlow = true,
            onTagsSubmit = { tags ->
                // 选完了：带回日记页，等点保存才存
                draftTags = tags
            },
            onClose = {
                tagDate = null
                tagFlowDate = null
                viewDate = dateKey
            }
        )
        }
    }
}

/** 这天写过日记后，再点一下就直接进日记页，可以直接改。 */
@Composable
private fun DiaryViewScreen(
    dateKey: String,
    entry: Entry,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    tags: List<String>,
    onSave: (String, String, List<String>, List<String>) -> Unit,
    onChangeTags: () -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    // 进出编辑都回到「已存下的内容」：没点保存的改动不留在页面上
    var title by remember(dateKey, editing, entry.summary) { mutableStateOf(entry.summary) }
    var text by remember(dateKey, editing, entry.text) { mutableStateOf(entry.text) }
    var images by remember(dateKey, editing) { mutableStateOf(entry.imageList) }
    var audios by remember(dateKey, editing) { mutableStateOf(entry.audioList) }
    // 看大图：存下标，-1 表示没打开
    var previewIndex by remember { mutableIntStateOf(-1) }

    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance().apply { timeInMillis = DateKey.toMillis(dateKey) }
    val dateText = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    val weekText = "星期${weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]}"

    // 退出编辑：改动全部丢掉，临时录的语音、临时选的图也一起清掉
    fun discard() {
        audios.filterNot { it in entry.audioList }.forEach { AudioStore.delete(context, it) }
        images.filterNot { it in entry.imageList }.forEach { ImageStore.delete(context, it) }
        onEditingChange(false)
    }

    // 编辑态按手机返回键：只退出编辑回到查看页，改动不存
    BackHandler(enabled = editing) { discard() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            // 键盘弹出时整页跟着上移，编辑日记能看到光标
            .imeAwareBottomPadding()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.cardAlt)
                    .clickable {
                        // 编辑态按返回 = 回到查看页，改动不存；要留着就点右上角「保存」
                        if (editing) discard() else onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("\u2039", fontSize = 18.sp, color = c.inkSoft)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateText,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink
                )
                Text(
                    weekText,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.cardAlt)
                    .clickable {
                        // 只有点这里才真的存下来
                        if (editing) {
                            onSave(title, text, images, audios)
                            onEditingChange(false)
                        } else {
                            onEditingChange(true)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (editing) "保存" else "编辑",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (editing) c.accent else c.ink
                )
            }
        }

        Spacer(Modifier.height(12.dp))

                // 内容可滚动：键盘弹出或配图多时，都能滚到输入框
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                // 编辑 ↔ 查看：标题和正文都淡着换，不做画面闪变
                AnimatedContent(
                    targetState = editing,
                    transitionSpec = { fadeTransition() },
                    label = "diaryTitleSwitch"
                ) { ed ->
                if (ed) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "标题",
                                color = c.inkSoft,
                                fontSize = 17.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = c.ink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                } else {
                    Text(
                        if (title.isBlank()) "无标题" else title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (title.isBlank()) c.inkSoft else c.ink,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                }

                // 标签放在标题和正文之间
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tags.forEachIndexed { index, tag ->
                            if (index > 0) Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .border(1.dp, c.line, RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(tag, fontSize = 12.sp, color = c.ink)
                            }
                        }
                    }
                }
                // 改标签只在编辑时出现，看日记的时候不显示
                if (editing) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "\uD83C\uDFF7 改标签",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .clickable { onChangeTags() }
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // 配图：编辑时可增删，平时只看
                if (editing) {
                    ImagePickGrid(
                        images = images,
                        onImages = { images = it },
                        emptyHint = "给今天配张图",
                        onPreview = { previewIndex = images.indexOf(it) }
                    )
                    Spacer(Modifier.height(10.dp))
                } else if (images.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        images.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { name ->
                                    ImageThumb(
                                        name = name,
                                        modifier = Modifier.weight(1f),
                                        onClick = { previewIndex = images.indexOf(name) }
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // 正文：输入时是一整块输入框，存下来后变成卡片，两者淡着切换
                AnimatedContent(
                    targetState = editing,
                    transitionSpec = { fadeTransition() },
                    label = "diaryBodySwitch"
                ) { ed ->
                if (ed) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = {
                            Text(
                                "今天发生了什么…",
                                color = c.inkSoft,
                                fontSize = 15.sp,
                                lineHeight = 24.sp
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = c.ink,
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.line
                        )
                    )
                } else {
                    // 外层已经可以滚动，这里只是个卡片
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.card)
                            .padding(16.dp)
                    ) {
                        Text(
                            if (text.isBlank()) "还没有写下什么…" else text,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = if (text.isBlank()) c.inkSoft else c.ink
                        )
                    }
                }
                }

                // 语音：编辑时录，查看时点一下就能听
                if (editing || audios.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    AudioSection(
                        files = audios,
                        editing = editing,
                        onAdd = { audios = audios + it },
                        onDelete = { name -> audios = audios - name }
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "共 ${text.length} 字",
                    fontSize = 12.sp,
                    color = c.inkSoft,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
                }

        if (previewIndex in images.indices) {
            ImageViewer(
                images = images,
                index = previewIndex,
                onIndexChange = { previewIndex = it },
                onDismiss = { previewIndex = -1 }
            )
        }
    }
}

/** 点日历后弹出的圆形心情选择器：点一个表情就记下，也可以只写文字。 */
@Composable
private fun MoodCirclePicker(
    dateKey: String,
    existing: Entry?,
    onPick: (Mood) -> Unit,
    onClose: () -> Unit
) {
    val c = LocalAppColors.current
    val today = DateKey.fromMillis(System.currentTimeMillis())
    val existingMood = existing?.mood?.let { Mood.of(it) }
    var selectedMood by remember(dateKey) { mutableStateOf(existingMood) }

    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance().apply { timeInMillis = DateKey.toMillis(dateKey) }
    val dateText = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    val weekText = "星期${weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]}"
    val isToday = dateKey == today

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.cardAlt)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("\u2039", fontSize = 18.sp, color = c.inkSoft)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateText,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink
                )
                Text(
                    if (isToday) "今天 · $weekText" else weekText,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Hi，你今天心情怎么样",
            style = MaterialTheme.typography.headlineSmall,
            color = c.ink,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (existingMood != null) "心情是什么呢？（已记过「${existingMood.label}」）"
            else "心情是什么呢？",
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkSoft,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // 半径跟着可用空间算：两层圆 + 表情都不会超出屏幕
            val outerSpace = 30.dp
            val density = LocalDensity.current
            val radiusPx = with(density) {
                ((if (maxWidth < maxHeight) maxWidth else maxHeight) / 2 - outerSpace).toPx()
            }
            val radiusInner = radiusPx * 0.55f
            // 整圈往上挪一点，给下面的「确定」留位置
            val offsetUp = with(density) { 30.dp.toPx() }

            // 27 颗手绘表情：内圈 11 颗常用，外圈 16 颗
            val inner = moodStickers.take(11)
            val outer = moodStickers.drop(11)
            listOf(radiusInner to inner, radiusPx to outer).forEach { (radius, stickers) ->
                stickers.forEachIndexed { index, st ->
                    val angle = (index * 360f / stickers.size) - 90f
                    val radian = Math.toRadians(angle.toDouble())
                    val x = radius * cos(radian).toFloat()
                    val y = radius * sin(radian).toFloat() - offsetUp
                    val mood = st.mood
                    val picked = selectedMood == mood
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .size(if (picked) 56.dp else 46.dp)
                            .clip(CircleShape)
                            .background(
                                if (picked) Color(mood.colorHex).copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (picked) 2.5.dp else 0.dp,
                                color = if (picked) Color(mood.colorHex) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                // 先只选中，点下面「确定」才真的记下来
                                selectedMood = mood
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(st.res),
                            contentDescription = mood.label,
                            modifier = Modifier
                                .size(if (picked) 52.dp else 43.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // 圆心：提示当前选了哪种心情（跟着圆一起上移）
            Text(
                selectedMood?.label ?: "点一个表情",
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedMood != null) c.ink else c.inkSoft,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(0, -offsetUp.roundToInt()) }
            )
        }

        // 选好心情后点确定，再进入下一步
        PrimaryButton(
            text = if (selectedMood != null) "确定 · ${selectedMood!!.label}" else "确定",
            onClick = { selectedMood?.let { onPick(it) } },
            enabled = selectedMood != null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** 紧凑月历：有心情显示表情，只写了日记显示小圆点，今天描边。 */
@Composable
private fun MoodCalendarGrid(
    month: Calendar,
    moodMap: Map<String, Entry>,
    diaryKeys: Set<String>,
    todayKey: String,
    onDateClick: (String) -> Unit
) {
    val c = LocalAppColors.current
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=周一
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val totalCells = firstDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0..6) {
                    val index = row * 7 + col
                    val dayNum = index - firstDayOfWeek + 1
                    if (dayNum in 1..daysInMonth) {
                        val dateKey = DateKey.of(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            dayNum
                        )
                        val entry = moodMap[dateKey]
                        val mood = entry?.mood?.let { Mood.of(it) }
                        val hasDiary = dateKey in diaryKeys
                        val isToday = dateKey == todayKey
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDateClick(dateKey) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isToday) c.accentSoft else Color.Transparent
                                    )
                                    .then(
                                        if (isToday) Modifier.border(
                                            1.5.dp, c.accent, RoundedCornerShape(14.dp)
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$dayNum",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isToday) c.accent else c.inkSoft,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    mood != null -> {
                                        // 日历上直接用手绘的那张表情（记住的时候冒出来一下）
                                        val res = stickerOf(mood)
                                        if (res != null) {
                                            Image(
                                                painter = painterResource(res),
                                                contentDescription = mood.label,
                                                modifier = Modifier.size(40.dp).then(popIn())
                                            )
                                        } else {
                                            Text(
                                                mood.emoji,
                                                fontSize = 34.sp,
                                                modifier = popIn()
                                            )
                                        }
                                    }
                                    hasDiary -> Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(c.accent.copy(alpha = 0.5f))
                                            .then(popIn())
                                    )
                                    isToday -> Text(
                                        "\u2B50",
                                        fontSize = 14.sp,
                                        modifier = popIn()
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
