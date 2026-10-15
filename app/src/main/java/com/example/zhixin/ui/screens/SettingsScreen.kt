package com.example.zhixin.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.zhixin.ai.AiClient
import com.example.zhixin.data.AI_PRESETS
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Store
import com.example.zhixin.notify.ReminderReceiver
import com.example.zhixin.notify.applyReminder
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.ConfirmDialog
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.InputDialog
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SectionTitle
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.TagChip
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.SKINS
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettings,
    entries: List<Entry>,
    onChange: (AppSettings) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onShowGuide: () -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current

    var showTags by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showClearAll by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var aiBase by remember(settings) { mutableStateOf(settings.aiBaseUrl) }
    var aiKey by remember(settings) { mutableStateOf(settings.aiKey) }
    var aiModel by remember(settings) { mutableStateOf(settings.aiModel) }
    var aiResult by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val dayCount = remember(entries) { entries.map { it.dateKey }.distinct().size }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenTopBar(
                title = "⚙️ 小屋设置",
                subtitle = "皮肤、提醒、密码锁、备份都在这里",
                onBack = onBack
            )
        }

        // ---------- 外观与展示 ----------
        item {
            Text(
                "🎨 外观与展示",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = c.accent
            )
        }
        item {
            SettingsSection("🎨", "小屋皮肤", "切换后界面配色 + 文字氛围一起变") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SKINS.forEach { skin ->
                        ChoiceChip(
                            text = "${skin.emoji} ${skin.name}",
                            selected = settings.skin == skin.key,
                            modifier = Modifier.weight(1f)
                        ) { onChange(settings.copy(skin = skin.key)) }
                    }
                }
            }
        }

        item {
            SettingsSection("🌓", "深色模式", "跟随系统 / 始终浅色 / 始终深色") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("自动", settings.darkMode == "auto", Modifier.weight(1f)) {
                        onChange(settings.copy(darkMode = "auto"))
                    }
                    ChoiceChip("浅色", settings.darkMode == "light", Modifier.weight(1f)) {
                        onChange(settings.copy(darkMode = "light"))
                    }
                    ChoiceChip("深色", settings.darkMode == "dark", Modifier.weight(1f)) {
                        onChange(settings.copy(darkMode = "dark"))
                    }
                }
            }
        }

        item {
            SettingsSection("🔤", "字体大小", "挑一个看着最舒服的字号") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("小", settings.fontScale < 0.95f, Modifier.weight(1f)) {
                        onChange(settings.copy(fontScale = 0.9f))
                    }
                    ChoiceChip("标准", settings.fontScale in 0.95f..1.05f, Modifier.weight(1f)) {
                        onChange(settings.copy(fontScale = 1.0f))
                    }
                    ChoiceChip("大", settings.fontScale > 1.05f, Modifier.weight(1f)) {
                        onChange(settings.copy(fontScale = 1.15f))
                    }
                }
            }
        }

        item {
            SettingsSection("☁️", "小屋装饰元素", "飘动的云朵 / 月亮星星") {
                SwitchRow(settings.decor) { onChange(settings.copy(decor = it)) }
            }
        }

        // ---------- 陪伴与提醒 ----------
        item {
            Text(
                "🤍 陪伴与提醒",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = c.accent
            )
        }
        item {
            SettingsSection("🎧", "只倾听模式", "开启后整理记录只温柔接住，不做分析不开导") {
                SwitchRow(settings.listenOnly) { onChange(settings.copy(listenOnly = it)) }
            }
        }

        item {
            SettingsSection("⏰", "每日记录提醒", "到点提醒自己来小屋坐坐") {
                SwitchRow(settings.remindEnabled) { enable ->
                    if (enable) {
                        val next = settings.copy(remindEnabled = true)
                        onChange(next)
                        applyReminder(context, next, onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        })
                    } else {
                        ReminderReceiver.cancel(context)
                        onChange(settings.copy(remindEnabled = false))
                    }
                }
                if (settings.remindEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("时间", style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
                        Spacer(Modifier.width(10.dp))
                        Stepper("-") {
                            val next = shiftReminder(settings, -10)
                            onChange(next)
                            applyReminder(context, next) {}
                        }
                        Text(
                            String.format("%02d:%02d", settings.remindHour, settings.remindMinute),
                            style = MaterialTheme.typography.titleMedium,
                            color = c.accent,
                            modifier = Modifier.width(70.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Stepper("+") {
                            val next = shiftReminder(settings, 10)
                            onChange(next)
                            applyReminder(context, next) {}
                        }
                    }
                }
            }
        }

        item {
            SettingsSection("🏷", "活动标签", "打卡与记录时可勾选的活动") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.tags.take(4).forEach { t ->
                        Text("#$t", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                    }
                }
                Spacer(Modifier.height(10.dp))
                TinyButton("管理活动") { showTags = true }
            }
        }

        // ---------- 安全与数据 ----------
        item {
            Text(
                "🔐 安全与数据",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = c.accent
            )
        }
        item {
            SettingsSection("🔒", "小屋密码锁", "打开小屋需输入密码（仅防误触）") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (settings.lockEnabled()) "已开启" else "未开启",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSoft,
                        modifier = Modifier.weight(1f)
                    )
                    TinyButton("设置 / 修改") { showPassword = true }
                    Spacer(Modifier.width(8.dp))
                    TinyButton("关闭") { onChange(settings.copy(password = "")) }
                }
            }
        }

        item {
            SettingsSection("🧹", "回收站", "已删除的故事在这里，可恢复或彻底清空") {
                TinyButton("打开回收站") { showTrash = true }
            }
        }

        item {
            SettingsSection("🗑️", "清空所有数据", "删除收藏本全部故事与设置") {
                TinyButton("一键清空") { showClearAll = true }
            }
        }

        item {
            SettingsSection("📖", "使用指南", "重新显示首页的新手引导卡片") {
                TinyButton("打开引导") {
                    onChange(settings.copy(guideSeen = false))
                    onShowGuide()
                }
            }
        }

        item {
            SettingsSection("💾", "备份数据", "导出 JSON 备份，或从备份导入") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TinyButton("📤 导出备份", onClick = onExport)
                    TinyButton("📥 导入备份", onClick = onImport)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "故事 ${entries.size} 篇 · 有记录的日子 $dayCount 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            }
        }

        // ---------- AI 整理 ----------
        item {
            Text(
                "🤖 AI 整理",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = c.accent
            )
        }
        item {
            SettingsSection(
                "🤖",
                "AI 分析（可选）",
                "填 OpenAI 兼容 Key 后由 AI 整理记录；不填则用本地模板兜底"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AI_PRESETS.forEach { (label, url, model) ->
                        ChoiceChip(label, settings.aiBaseUrl == url, Modifier.weight(1f)) {
                            aiBase = url
                            aiModel = model
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SettingsField("接口地址", aiBase) { aiBase = it }
                Spacer(Modifier.height(8.dp))
                SettingsField("模型名", aiModel) { aiModel = it }
                Spacer(Modifier.height(8.dp))
                SettingsField("API Key", aiKey) { aiKey = it }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TinyButton("保存 AI 设置") {
                        onChange(settings.copy(aiBaseUrl = aiBase.trim(), aiModel = aiModel.trim(), aiKey = aiKey.trim()))
                        aiResult = "已保存"
                    }
                    TinyButton("清除 Key") {
                        aiKey = ""
                        onChange(settings.copy(aiKey = ""))
                    }
                    TinyButton("测试连接") {
                        aiResult = "测试中…"
                        val s = settings.copy(aiBaseUrl = aiBase.trim(), aiModel = aiModel.trim(), aiKey = aiKey.trim())
                        CoroutineScope(Dispatchers.Main).launch {
                            aiResult = AiClient.testConnection(s)
                        }
                    }
                }
                if (aiResult.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(aiResult, style = MaterialTheme.typography.bodySmall, color = c.accent)
                }
            }
        }

        // ---------- 关于知新 ----------
        item {
            Text(
                "💫 关于知新",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = c.accent
            )
        }
        item {
            SoftCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAbout = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💫", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("关于知新", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        Text("版本 v1.1.0 · 情绪小屋", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                    }
                    Text("›", fontSize = 20.sp, color = c.inkSoft)
                }
            }
        }

        item {
            Text(
                "🔒 隐私说明：你写下的所有故事只保存在本机，不会上传到任何云端；AI 分析仅在填写并保存 Key 后才会把单篇故事发送给你配置的接口。建议定期「导出」备份。密码锁只是防误触，不是真正的加密。\n🗂️ 数据版本：v1 · 本地 JSON 存储",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
        }
    }

    if (showTags) {
        TagManagerDialog(
            tags = settings.tags,
            onDismiss = { showTags = false },
            onCommit = { onChange(settings.copy(tags = it)) }
        )
    }

    if (showPassword) {
        InputDialog(
            title = "设置小屋密码",
            label = "输入 4-6 位数字（留空 = 关闭）",
            initial = settings.password,
            onDismiss = { showPassword = false },
            onConfirm = { value ->
                showPassword = false
                onChange(settings.copy(password = value.trim()))
            }
        )
    }

    if (showClearAll) {
        ConfirmDialog(
            title = "清空所有数据？",
            message = "收藏本里的全部故事都会被删除（可先在回收站恢复前会丢失），确定继续吗？",
            confirmText = "清空",
            onDismiss = { showClearAll = false },
            onConfirm = {
                showClearAll = false
                Store.clearAll(context)
                onChange(settings.copy(password = ""))
            }
        )
    }

    if (showTrash) {
        TrashDialog(
            onDismiss = { showTrash = false },
            onRestore = { Store.restore(context, it) },
            onEmpty = { Store.emptyTrash(context) }
        )
    }

    if (showAbout) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = c.card,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "💫 关于知新",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("知新 · 情绪小屋", style = MaterialTheme.typography.titleMedium, color = c.accent)
                    Text("版本 v1.1.0", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "让每一天活出新意，而不是把同一天重复365次。",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "纯本地存储 · 不上传云端\n你的情绪和故事，只属于你自己。",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                }
            },
            confirmButton = {
                TinyButton("知道了", onClick = { showAbout = false })
            }
        )
    }
}

private fun shiftReminder(current: AppSettings, deltaMinutes: Int): AppSettings {
    val m = current.remindMinute + deltaMinutes
    return if (m > 50) current.copy(remindHour = (current.remindHour + 1) % 24, remindMinute = 0)
    else if (m < 0) current.copy(remindHour = (current.remindHour + 23) % 24, remindMinute = 50)
    else current.copy(remindMinute = m)
}

@Composable
private fun SettingsSection(
    emoji: String,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    SoftCard {
        SectionTitle(emoji = emoji, title = title, subtitle = subtitle)
        Spacer(Modifier.height(12.dp))
        Column { content() }
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(if (selected) c.accentSoft else c.cardAlt)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.accent else c.inkSoft,
            maxLines = 1
        )
    }
}

@Composable
private fun SwitchRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (checked) "已开启" else "关闭",
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkSoft,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = c.line
            )
        )
    }
}

@Composable
private fun Stepper(text: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.cardAlt)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = c.accent)
    }
}

@Composable
private fun SettingsField(label: String, value: String, onChange: (String) -> Unit) {
    val c = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        label = { Text(label, color = c.inkSoft) },
        shape = ChipShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.line,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun TagManagerDialog(
    tags: List<String>,
    onDismiss: () -> Unit,
    onCommit: (List<String>) -> Unit
) {
    val c = LocalAppColors.current
    var adding by remember { mutableStateOf(false) }
    val list = remember(tags) { tags.toMutableStateList() }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text("🏷 活动标签", style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = {
            Column {
                Text("点标签可删除。", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEach { t ->
                        TagChip(text = "✕ $t", selected = false) { list.remove(t) }
                    }
                }
                if (list.isEmpty()) {
                    Text("还没有标签", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                }
            }
        },
        confirmButton = {
            TinyButton("添加") { adding = true }
        },
        dismissButton = {
            Row {
                TinyButton("完成") { onCommit(list.toList()); onDismiss() }
            }
        }
    )

    if (adding) {
        InputDialog(
            title = "添加标签",
            label = "例如：散步",
            onDismiss = { adding = false },
            onConfirm = { v ->
                adding = false
                if (v.isNotBlank() && !list.contains(v.trim())) list.add(v.trim())
            }
        )
    }
}

@Composable
private fun TrashDialog(
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit,
    onEmpty: () -> Unit
) {
    val c = LocalAppColors.current
    val trash = Store.trash()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text("🧹 回收站", style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = {
            if (trash.isEmpty()) {
                Text("回收站是空的～", style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
            } else {
                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    trash.forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                e.text.take(28).ifBlank { "（打卡）" },
                                style = MaterialTheme.typography.bodySmall,
                                color = c.ink,
                                modifier = Modifier.weight(1f)
                            )
                            TinyButton("恢复") { onRestore(e.id) }
                        }
                    }
                }
            }
        },
        confirmButton = { TinyButton("彻底清空") { onEmpty() } },
        dismissButton = { TinyButton("关闭", onClick = onDismiss) }
    )
}
