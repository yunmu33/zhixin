package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.aspectRatio
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Mood
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.StatCard
import com.example.zhixin.ui.theme.LocalAppColors
import androidx.compose.material3.AlertDialog
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MoodScreen(
    entries: List<Entry>,
    onQuickCheckIn: (Mood, String, String) -> Unit,
    onUpdateEntry: (Entry) -> Unit
) {
    val c = LocalAppColors.current
    val today = DateKey.fromMillis(System.currentTimeMillis())

    val moodEntries = remember(entries) {
        entries.filter { it.category == Category.MOOD.key }
    }
    val moodMap = remember(moodEntries) {
        moodEntries.associateBy { it.dateKey }
    }

    val streak = remember(moodEntries) {
        val keys = moodEntries.map { it.dateKey }.toSet()
        var cursor = System.currentTimeMillis()
        if (DateKey.fromMillis(cursor) !in keys) cursor = DateKey.addDays(cursor, -1)
        var n = 0
        while (DateKey.fromMillis(cursor) in keys) {
            n++
            cursor = DateKey.addDays(cursor, -1)
        }
        n
    }

    // 日历当前月份
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var pickerNote by remember { mutableStateOf("") }

    // 选中某天时打开弹窗
    fun onDateClick(dateKey: String) {
        selectedDate = dateKey
        val existing = moodMap[dateKey]
        pickerNote = existing?.text ?: ""
        showPicker = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("😊 心情日记", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Text(
                    "今天的你感觉如何呢",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            }
        }

        // 日历
        item {
            // 月份切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            val cal = currentMonth.clone() as Calendar
                            cal.add(Calendar.MONTH, -1)
                            currentMonth = cal
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", style = MaterialTheme.typography.titleMedium, color = c.inkSoft)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "%d年%d月".format(
                        currentMonth.get(Calendar.YEAR),
                        currentMonth.get(Calendar.MONTH) + 1
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            val cal = currentMonth.clone() as Calendar
                            cal.add(Calendar.MONTH, 1)
                            currentMonth = cal
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("›", style = MaterialTheme.typography.titleMedium, color = c.inkSoft)
                }
            }
            Spacer(Modifier.height(16.dp))

            // 星期标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(
                            day,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkSoft,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            Spacer(Modifier.height(8.dp))

            // 日历格子
            MoodCalendarGrid(
                month = currentMonth,
                moodMap = moodMap,
                todayKey = today,
                onDateClick = ::onDateClick
            )
        }

        item {
            Text(
                "每一种心情都值得被记录 ✨",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    // 心情选择全屏页面
    if (showPicker && selectedDate != null) {
        val dateKey = selectedDate!!
        val existing = moodMap[dateKey]
        val existingMood = existing?.mood?.let { Mood.of(it) }
        var selectedMood by remember(showPicker) { mutableStateOf(existingMood) }

        val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
        val cal = Calendar.getInstance().apply {
            timeInMillis = DateKey.toMillis(dateKey)
        }
        val monthNum = cal.get(Calendar.MONTH) + 1
        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
        val weekDay = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // 顶部栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${monthNum}月${dayNum}日",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.ink,
                        fontSize = 20.sp
                    )
                    Text(
                        "星期$weekDay",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c.cardAlt)
                        .clickable { showPicker = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", style = MaterialTheme.typography.bodyLarge, color = c.inkSoft)
                }
            }

            Spacer(Modifier.height(40.dp))

            // 标题
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Hi，你今天心情怎么样",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.ink,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(30.dp))

            // 中间圆形表情选择
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val moods = Mood.entries
                val radius = 400.dp
                val itemSize = 64.dp

                // 8个表情围成圆形
                moods.forEachIndexed { index, mood ->
                    val angle = (index * 360f / moods.size) - 90f
                    val radian = Math.toRadians(angle.toDouble())
                    val x = (radius.value * cos(radian)).toFloat()
                    val y = (radius.value * sin(radian) - 200f).toFloat()

                    Box(
                        modifier = Modifier
                            .size(itemSize)
                            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .clip(CircleShape)
                            .background(
                                if (selectedMood?.key == mood.key)
                                    Color(mood.colorHex).copy(alpha = 0.3f)
                                else c.cardAlt
                            )
                            .clickable {
                                selectedMood = mood
                                if (existing != null) {
                                    onUpdateEntry(
                                        existing.copy(
                                            mood = mood.key,
                                            score = mood.score
                                        )
                                    )
                                } else {
                                    onQuickCheckIn(mood, "", dateKey)
                                }
                                showPicker = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mood.emoji, fontSize = 32.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodCalendarGrid(
    month: Calendar,
    moodMap: Map<String, Entry>,
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        val isToday = dateKey == todayKey
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDateClick(dateKey) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 上面：日期小方格
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isToday) c.accentSoft else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$dayNum",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) c.accent else c.inkSoft,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            // 下面：表情标签区块
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    mood != null -> Text(mood.emoji, fontSize = 28.sp)
                                    isToday -> Text("⭐", fontSize = 22.sp)
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

@Composable
private fun MoodPickerItem(
    mood: Mood,
    selected: Boolean,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Color(mood.colorHex).copy(alpha = 0.35f)
                    else Color(mood.colorHex).copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(mood.emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            mood.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) c.ink else c.inkSoft,
            fontSize = 11.sp
        )
    }
}
