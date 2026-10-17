package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Mood
import com.example.zhixin.ui.components.ImagePickGrid
import com.example.zhixin.ui.components.InputDialog
import com.example.zhixin.ui.theme.LocalAppColors
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 一个圆形选项：圆内的表情、圆外的文字、自己的主题色。 */
private data class WizardOption(
    val icon: String,
    val label: String,
    val colorHex: Long
)

/** 天气选项。 */
private val WEATHER_OPTIONS = listOf(
    WizardOption("\u2600\uFE0F", "晴", 0xFFFFC15E),
    WizardOption("\u26C5", "多云", 0xFFA8C6E0),
    WizardOption("\u2601", "阴", 0xFF9BA6B5),
    WizardOption("\uD83C\uDF27", "雨", 0xFF7FA6C9),
    WizardOption("\u26C8", "雷雨", 0xFF8E86C9),
    WizardOption("\uD83C\uDF28", "雪", 0xFFB8DBE8),
    WizardOption("\uD83D\uDCA8", "风", 0xFF9FD6C9),
    WizardOption("\uD83C\uDF2B", "雾", 0xFFBFC5CE)
)

/** 事件标签。 */
private val EVENT_OPTIONS = listOf(
    WizardOption("\u270F\uFE0F", "自定义", 0xFF9BB6D6),
    WizardOption("\uD83C\uDFAC", "电影", 0xFFC48FB0),
    WizardOption("\uD83C\uDFAE", "游戏", 0xFF8FB6C4),
    WizardOption("\uD83C\uDFE0", "家庭", 0xFFF0A24C),
    WizardOption("\u2708", "旅游", 0xFF7FBFA8),
    WizardOption("\uD83D\uDC6C", "关系", 0xFFEB8A90),
    WizardOption("\uD83C\uDF54", "食物", 0xFFFFC15E),
    WizardOption("\uD83C\uDFA7", "音乐", 0xFFA48FD0),
    WizardOption("\uD83D\uDCBB", "工作", 0xFF8E9AAF)
)

/**
 * 记完心情后的连续小向导：天气 → 事件 → 写日记。
 * 页面样式和心情选择页一致，天气 / 事件作为标签存进当天记录。
 */
@Composable
fun MoodWizard(
    dateKey: String,
    existing: Entry?,
    /** 当天已记的心情，日记页会显示成标签。 */
    mood: Mood?,
    onUpdateEntry: (Entry) -> Unit,
    /** 向导最后一步写的日记：日期、标题、正文、配图文件名。 */
    onAddDiary: (String, String, String, List<String>) -> Unit,
    /** 第一页的返回 → 退回心情选择页。 */
    onBackToMood: () -> Unit,
    /** true = 改标签路线：天气 → 原因，选完原因就收工，不进日记页。 */
    tagFlow: Boolean = false,
    onClose: () -> Unit
) {
    val c = LocalAppColors.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    // 改标签时要带着原来选过的天气，不能覆盖掉
    var weather by rememberSaveable { mutableStateOf(existing?.tags?.getOrNull(0).orEmpty()) }
    var event by rememberSaveable { mutableStateOf(existing?.tags?.getOrNull(1).orEmpty()) }
    var diaryTitle by rememberSaveable { mutableStateOf("") }
    var diaryText by rememberSaveable { mutableStateOf("") }
    var diaryImages by remember { mutableStateOf(listOf<String>()) }
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }

    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance().apply { timeInMillis = DateKey.toMillis(dateKey) }
    val dateText = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    val weekText = "星期${weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]}"

    fun finish() {
        val tags = listOf(weather, event).filter { it.isNotBlank() }
        val base = existing
        if (base != null) onUpdateEntry(base.copy(tags = tags))
        if (diaryTitle.isNotBlank() || diaryText.isNotBlank() || diaryImages.isNotEmpty()) {
            onAddDiary(dateKey, diaryTitle.trim(), diaryText.trim(), diaryImages)
        }
        onClose()
    }

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
        // ── 顶栏：日期 + 返回 / 跳过（和心情页同一行排版）──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.cardAlt)
                    .clickable { if (step == 0) onBackToMood() else step-- },
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
            // 日记页的标题挪到日期旁边
            if (step == 2) {
                Text(
                    "写点什么留给今天",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.accent
                )
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.cardAlt)
                    .clickable { finish() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (step == 2) "完成" else "跳过",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 标题：和心情页一样的两行居中（日记页的标题放在日期旁，这里就不重复了）
        if (step < 2) {
            Text(
                if (step == 0) "你那里天气如何" else "是什么事情让你记住它",
                style = MaterialTheme.typography.headlineSmall,
                color = c.ink,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (step == 0) "天气如何呢？" else "是哪一件事呢？",
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }

        when (step) {
            0 -> CircleOptionGroup(
                options = WEATHER_OPTIONS,
                selected = weather,
                modifier = Modifier.weight(1f)
            ) { key ->
                // 再点一次取消，点新选项直接进下一步
                if (weather == key) weather = "" else {
                    weather = key
                    step++
                }
            }

            1 -> CircleOptionGroup(
                options = EVENT_OPTIONS,
                selected = event,
                modifier = Modifier.weight(1f)
            ) { key ->
                when {
                    // 自定义 → 弹窗输入
                    key == "自定义" -> showCustomDialog = true
                    event == key -> event = ""
                    else -> {
                        event = key
                        // 改标签路线：选完原因就收工
                        if (tagFlow) finish() else step++
                    }
                }
            }

            else -> DiaryStep(
                tags = buildList {
                    if (weather.isNotBlank()) add(weather)
                    mood?.let { add(it.label) }
                    if (event.isNotBlank()) add(event)
                },
                title = diaryTitle,
                text = diaryText,
                images = diaryImages,
                modifier = Modifier.weight(1f),
                onTitle = { diaryTitle = it },
                onText = { diaryText = it },
                onImages = { diaryImages = it }
            )
        }

        if (showCustomDialog) {
            InputDialog(
                title = "自定义事件",
                label = "是什么事呢？",
                onConfirm = { value ->
                    showCustomDialog = false
                    val t = value.trim()
                    if (t.isNotBlank()) {
                        event = t.take(10)
                        if (tagFlow) finish() else step++
                    }
                },
                onDismiss = { showCustomDialog = false }
            )
        }

    }
}

/** 写日记那一页：标签胶囊 + 标题 + 正文 + 字数，参考日记编辑器的排版。 */
@Composable
private fun DiaryStep(
    tags: List<String>,
    title: String,
    text: String,
    images: List<String>,
    modifier: Modifier = Modifier,
    onTitle: (String) -> Unit,
    onText: (String) -> Unit,
    onImages: (List<String>) -> Unit
) {
    val c = LocalAppColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        // 标题：居中大字
        OutlinedTextField(
            value = title,
            onValueChange = onTitle,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    "给这篇日记起个标题吧…",
                    color = c.inkSoft,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            textStyle = LocalTextStyle.current.copy(
                color = c.ink,
                fontSize = 18.sp,
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

        // 已选的天气 / 事件标签，放在标题和正文之间
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
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
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(tag, fontSize = 12.sp, color = c.ink)
                    }
                }
            }
        }

        // 配图：从相册选，最多 6 张
        ImagePickGrid(
            images = images,
            onImages = onImages,
            emptyHint = "给今天配张图"
        )

        Spacer(Modifier.height(10.dp))

        // 正文
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(18.dp),
            placeholder = {
                Text(
                    "今天发生了什么，想到什么就写什么…",
                    color = c.inkSoft,
                    fontSize = 15.sp,
                    lineHeight = 26.sp
                )
            },
            textStyle = LocalTextStyle.current.copy(
                color = c.ink,
                fontSize = 15.sp,
                lineHeight = 26.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.line
            )
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "共 ${text.length} 字",
            fontSize = 12.sp,
            color = c.inkSoft,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

/**
 * 圆形选项组：和心情选择页一模一样的做法——
 * 自己的颜色做底、文字放在圆外面、整圈略微上移。
 */
@Composable
private fun CircleOptionGroup(
    options: List<WizardOption>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val c = LocalAppColors.current
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val radiusPx = with(density) {
            ((if (maxWidth < maxHeight) maxWidth else maxHeight) / 2 - 54.dp).toPx()
        }
        val offsetUp = with(density) { 36.dp.toPx() }

        options.forEachIndexed { index, option ->
            val angle = (index * 360f / options.size) - 90f
            val radian = Math.toRadians(angle.toDouble())
            val x = radiusPx * cos(radian).toFloat()
            val y = radiusPx * sin(radian).toFloat() - offsetUp
            val picked = selected == option.label
            Column(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .clickable { onSelect(option.label) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(if (picked) 64.dp else 56.dp)
                        .clip(CircleShape)
                        .background(
                            if (picked) Color(option.colorHex)
                            else Color(option.colorHex).copy(alpha = 0.16f)
                        )
                        .border(
                            width = if (picked) 3.dp else 1.dp,
                            color = if (picked) Color(option.colorHex).copy(alpha = 0.35f)
                            else Color(option.colorHex).copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option.icon,
                        fontSize = if (picked) 34.sp else 29.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    option.label,
                    fontSize = 11.sp,
                    color = if (picked) c.ink else c.inkSoft,
                    maxLines = 1
                )
            }
        }
    }
}
