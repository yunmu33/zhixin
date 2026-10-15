package com.example.zhixin.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.CUSTOM_TAG
import com.example.zhixin.data.Category
import com.example.zhixin.data.CustomItem
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.Mood
import com.example.zhixin.data.Store
import com.example.zhixin.notify.applyReminder
import com.example.zhixin.ui.components.CardShape
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.ConfirmDialog
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.EntryCard
import com.example.zhixin.ui.components.HeatBar
import com.example.zhixin.ui.components.MoodPicker
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.SectionTitle
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.StatCard
import com.example.zhixin.ui.components.TagChip
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors

val WRITING_PROMPTS = listOf(
    "今天有没有一件很小、却值得记住的小事？",
    "如果今天是一种天气，它是什么样子的？",
    "有没有一句话，你一直想对自己说？",
    "今天身体最想告诉你什么？",
    "最近一次突然放松下来，是在什么时候？",
    "今天有没有想谢谢的人（包括自己）？"
)

@Composable
fun HomeScreen(
    entries: List<Entry>,
    settings: AppSettings,
    onWrite: (String, String) -> Unit,
    onOpenEntry: (Entry) -> Unit,
    onGoNew: () -> Unit,
    onGoCheckIn: () -> Unit,
    onQuickCheckIn: (Mood, List<String>, String) -> Unit,
    onQuickSave: (String) -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onCustomToggle: (CustomItem) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSettings: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val today = DateKey.fromMillis(System.currentTimeMillis())
    val checkedToday = entries.any { it.dateKey == today }
    val streak = remember(entries) { Store.streak(entries) }
    val totalDays = remember(entries) { entries.map { it.dateKey }.distinct().size }

    val totalHeat = remember(entries) { Heat.total(entries) }
    val todayHeat = remember(entries) { Heat.todayHeat(entries) }
    val heatProgress = remember(totalHeat) { Heat.progress(totalHeat) }
    val nextName = remember(totalHeat) { Heat.nextLevelName(totalHeat) }
    val need = remember(totalHeat) { Heat.needToNext(totalHeat) }

    val todayNew = remember(entries) { entries.count { it.dateKey == today && it.category == Category.NEW.key } }
    val totalNew = remember(entries) { entries.count { it.category == Category.NEW.key } }

    var promptIndex by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var quick by remember { mutableStateOf<Quick?>(null) }

    val recent = remember(entries) { entries.filter { it.text.isNotBlank() }.take(3) }
    val customDone = remember(entries, today) {
        entries.filter { it.dateKey == today && !it.deleted && it.tags.contains(CUSTOM_TAG) }
            .flatMap { it.tags }
            .toSet()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun requestNotifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun setReminder(next: AppSettings) {
        onSettingsChange(next)
        applyReminder(context, next) { requestNotifyPermission() }
    }

    fun shiftReminder(delta: Int) {
        val m = settings.remindMinute + delta
        val next = when {
            m > 50 -> settings.copy(remindHour = (settings.remindHour + 1) % 24, remindMinute = 0)
            m < 0 -> settings.copy(remindHour = (settings.remindHour + 23) % 24, remindMinute = 50)
            else -> settings.copy(remindMinute = m)
        }
        setReminder(next)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDCD6", fontSize = 26.sp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("知新", style = MaterialTheme.typography.titleLarge, color = c.ink)
                    Text(
                        "温故而知新 · 记录每一次打卡的热度",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                }
            }
        }

        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔥 打卡热度", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                        Row(verticalAlignment = Alignment.Bottom) {
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
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "今日 +$todayHeat",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (todayHeat > 0) c.accent else c.inkSoft
                        )
                        Text(
                            "连续 $streak 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                HeatBar(progress = heatProgress)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (nextName == null) "热度已爆表，你太稳了！"
                    else "再攒 $need 热度就能升到「$nextName」",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("📅", "${totalDays}天", "有记录的日子", Modifier.weight(1f))
                StatCard("📒", "${entries.size}篇", "收藏本", Modifier.weight(1f))
                StatCard("\uD83C\uDF31", "${totalNew}条", "新收获", Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(
                    emoji = "\uD83C\uDF31",
                    title = "每日一新",
                    hint = if (todayNew > 0) "今天 ${todayNew} 条" else "还没记录",
                    modifier = Modifier.weight(1f),
                    onClick = { quick = Quick.DailyNew }
                )
                QuickCard(
                    emoji = "\uD83C\uDFAF",
                    title = "任务打卡",
                    hint = if (settings.customItems.isEmpty()) "添加任务"
                    else "${customDone.size}/${settings.customItems.size}",
                    modifier = Modifier.weight(1f),
                    onClick = { quick = Quick.Task }
                )
                QuickCard(
                    emoji = "\uD83D\uDE00",
                    title = "心情打卡",
                    hint = if (checkedToday) "🔥 $todayHeat" else "待打卡",
                    modifier = Modifier.weight(1f),
                    onClick = { quick = Quick.Mood }
                )
            }
        }

        item {
            SoftCard {
                SectionTitle(emoji = "✍️", title = "小屋书桌", subtitle = "写点什么，或讲给我听")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        WRITING_PROMPTS[promptIndex],
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    TinyButton("🎲 换一张") {
                        promptIndex = (promptIndex + 1) % WRITING_PROMPTS.size
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("今天想说点什么…", color = c.inkSoft) },
                    shape = ChipShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.line,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = "放进小屋 · 开始整理",
                    onClick = {
                        val text = draft.trim()
                        if (text.isBlank()) return@PrimaryButton
                        onWrite(text, WRITING_PROMPTS[promptIndex])
                        draft = ""
                    }
                )
            }
        }

        item {
            Text(
                "📖 知新 · 所有记录只保存在这台设备里，不上传任何云端",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    when (quick) {
        Quick.DailyNew -> DailyNewSheet(
            entries = entries,
            onQuickSave = onQuickSave,
            onGoNew = onGoNew,
            onOpenEntry = onOpenEntry,
            onDismiss = { quick = null }
        )

        Quick.Task -> TaskCheckSheet(
            settings = settings,
            entries = entries,
            onToggle = onCustomToggle,
            onAdd = { emoji, name ->
                onSettingsChange(
                    settings.copy(customItems = settings.customItems + CustomItem(emoji, name))
                )
            },
            onDelete = { item ->
                onSettingsChange(
                    settings.copy(customItems = settings.customItems.filter { it.name != item.name })
                )
            },
            onDismiss = { quick = null }
        )

        Quick.Mood -> MoodCheckSheet(
            settings = settings,
            entries = entries,
            onCheckIn = onQuickCheckIn,
            onGoFull = { quick = null; onGoCheckIn() },
            onDismiss = { quick = null }
        )

        null -> Unit
    }
}

private enum class Quick { DailyNew, Task, Mood }

@Composable
private fun QuickCard(
    emoji: String,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(c.card)
            .border(1.dp, c.line, CardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = c.ink)
        Spacer(Modifier.height(2.dp))
        Text(hint, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
    }
}
