package com.example.zhixin.ui.screens

import com.example.zhixin.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.Store
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.HeatBar
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.SectionTitle
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.StatCard
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors

private val AVATARS = listOf(
    "\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF3B", "\uD83C\uDF41",
    "\uD83D\uDC39", "\uD83D\uDC31", "\uD83D\uDC36", "\uD83E\uDD8A",
    "☁", "⭐", "\uD83C\uDF08", "\uD83C\uDF19"
)

private data class Badge(val emoji: String, val name: String, val need: String, val got: Boolean)

@Composable
fun MineScreen(
    settings: AppSettings,
    entries: List<Entry>,
    onChange: (AppSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onShowGuide: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val c = LocalAppColors.current
    val logged = settings.accountUser.isNotBlank()
    var avatarPicker by remember { mutableStateOf(false) }
    var nameEditor by remember { mutableStateOf(false) }

    val streak = remember(entries) { Store.streak(entries) }
    val dayCount = remember(entries) { entries.map { it.dateKey }.distinct().size }
    val totalHeat = remember(entries) { Heat.total(entries) }
    val newCount = remember(entries) { entries.count { it.category == Category.NEW.key } }
    val joinDays = remember(entries) {
        val first = entries.minByOrNull { it.time }?.time
        if (first == null) 0 else DateKey.daysBetween(first, System.currentTimeMillis()) + 1
    }
    val badges = remember(entries, streak, totalHeat, newCount) {
        listOf(
            Badge("\uD83C\uDF31", "初次打卡", "记录第 1 条", entries.isNotEmpty()),
            Badge("🔥", "三日成型", "连续打卡 3 天", streak >= 3),
            Badge("🏅", "一周不断", "连续打卡 7 天", streak >= 7),
            Badge("👑", "月度坚持", "连续打卡 30 天", streak >= 30),
            Badge("\uD83C\uDF2B", "热度火热", "累计热度 200", totalHeat >= 200),
            Badge("\uD83C\uDF0B", "热度爆表", "累计热度 1200", totalHeat >= 1200),
            Badge("\uD83D\uDCD6", "笔耕不辍", "累计记录 50 条", entries.size >= 50),
            Badge("✨", "日知一新", "新收获 30 条", newCount >= 30)
        )
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
                Text("\uD83D\uDC64 我的", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Text("你的知新小档案", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
            }
        }

        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.cardAlt)
                            .border(2.dp, c.accentSoft, RoundedCornerShape(20.dp))
                            .clickable { avatarPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(settings.avatar, fontSize = 30.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                settings.nickname.ifBlank { "旅人" },
                                style = MaterialTheme.typography.titleMedium,
                                color = c.ink
                            )
                            Spacer(Modifier.width(6.dp))
                            TinyButton("改名字") { nameEditor = true }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${Heat.levelEmoji(totalHeat)} ${Heat.levelName(totalHeat)} · 加入第 ${joinDays} 天",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkSoft
                        )
                        Text(
                            settings.motto.ifBlank { "每天都有新的收获" },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.accent
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.cardAlt)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (logged) "\uD83D\uDC64 已登录：${settings.accountUser}"
                        else "\uD83E\uDDF3 游客模式 · 记录保存在本机",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (logged) {
                        TinyButton("退出登录", onClick = onLogout)
                    } else {
                        TinyButton("\uD83D\uDD11 登录", onClick = onLogin)
                        Spacer(Modifier.width(6.dp))
                        TinyButton("✨ 注册", onClick = onLogin)
                    }
                }
                Spacer(Modifier.height(12.dp))
                HeatBar(progress = remember(totalHeat) { Heat.progress(totalHeat) })
                Spacer(Modifier.height(6.dp))
                Text(
                    Heat.nextLevelName(totalHeat)?.let { "再攒 ${Heat.needToNext(totalHeat)} 热度升到「$it」" }
                        ?: "热度已爆表，你太稳了！",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("🔥", "${streak}天", "连续打卡", Modifier.weight(1f))
                StatCard("📅", "${dayCount}天", "有记录的日子", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("\uD83D\uDD25", String.format("%,d", totalHeat), "累计热度", Modifier.weight(1f))
                StatCard("\uD83C\uDF31", "${newCount}条", "新收获", Modifier.weight(1f))
            }
        }

        item {
            SoftCard {
                SectionTitle(emoji = "🏆", title = "我的成就", subtitle = "已点亮 ${badges.count { it.got }}/${badges.size}")
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    badges.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { badge ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                if (badge.got) c.accent.copy(alpha = 0.22f) else c.cardAlt
                                            )
                                            .border(
                                                1.dp,
                                                if (badge.got) c.accent else c.line,
                                                RoundedCornerShape(18.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (badge.got) badge.emoji else "🔒",
                                            fontSize = if (badge.got) 22.sp else 16.sp
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        badge.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (badge.got) c.ink else c.inkSoft,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        if (badge.got) "已达成" else badge.need,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.inkSoft,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        item {
            SoftCard {
                SectionTitle(emoji = "🏠", title = "小屋设置", subtitle = "打造属于你的情绪小屋")
                Spacer(Modifier.height(8.dp))
                SettingsRow("✨", "小屋总设置", "主题、提醒、密码锁等全部设置", onOpenSettings)
            }
        }

        item {
            SoftCard {
                SectionTitle(emoji = "⚙️", title = "设置与管理")
                Spacer(Modifier.height(8.dp))
                SettingsRow("\uD83C\uDFA8", "外观与皮肤", "主题、深色、字号、装饰", onOpenSettings)
                SettingsRow("🔔", "每日提醒", "定时提醒你回来打卡", onOpenSettings)
                SettingsRow("🔒", "隐私与密码锁", settings.lockEnabled().let { if (it) "已开启密码锁" else "未开启" }, onOpenSettings)
                SettingsRow("🏷", "活动标签", "管理打卡时的小标签", onOpenSettings)
                SettingsRow("\uD83D\uDDD1", "回收站", "找回误删的记录", onOpenSettings)
                SettingsRow("🤖", "AI 整理", if (settings.aiEnabled()) "已配置 ${settings.aiLabel}" else "未配置", onOpenSettings)
            }
        }

        item {
            SoftCard {
                SectionTitle(emoji = "💾", title = "数据备份", subtitle = "数据只存在这台设备里")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(text = "📤 导出备份", onClick = onExport, modifier = Modifier.weight(1f))
                    PrimaryButton(text = "📥 导入备份", onClick = onImport, modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            SoftCard {
                SectionTitle(emoji = "📖", title = "使用指南", subtitle = "一分钟了解知新怎么用")
                Spacer(Modifier.height(10.dp))
                PrimaryButton(text = "再看一次新手引导", onClick = onShowGuide)
            }
        }

        item {
            Text(
                "知新 v${BuildConfig.VERSION_NAME} · 温故而知新\n所有记录只保存在本机，不上传云端",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (avatarPicker) {
        AlertDialog(
            onDismissRequest = { avatarPicker = false },
            containerColor = c.card,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("换一个头像", style = MaterialTheme.typography.titleSmall, color = c.ink)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AVATARS.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { a ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (a == settings.avatar) c.accentSoft else c.cardAlt
                                        )
                                        .clickable {
                                            onChange(settings.copy(avatar = a))
                                            avatarPicker = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) { Text(a, fontSize = 24.sp) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { avatarPicker = false }) { Text("关闭", color = c.inkSoft) }
            }
        )
    }

    if (nameEditor) {
        var draft by remember { mutableStateOf(settings.nickname) }
        AlertDialog(
            onDismissRequest = { nameEditor = false },
            containerColor = c.card,
            shape = RoundedCornerShape(24.dp),
            title = { Text("改个名字 / 写句签名", style = MaterialTheme.typography.titleSmall, color = c.ink) },
            text = {
                Column {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("昵称", color = c.inkSoft) },
                        shape = ChipShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.line,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = settings.motto,
                        onValueChange = { onChange(settings.copy(motto = it)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("签名", color = c.inkSoft) },
                        shape = ChipShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.line,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onChange(settings.copy(nickname = draft.trim().ifBlank { "旅人" }))
                    nameEditor = false
                }) { Text("保存", color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = { nameEditor = false }) { Text("取消", color = c.inkSoft) }
            }
        )
    }
}

@Composable
private fun SettingsRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
        }
        Text("›", style = MaterialTheme.typography.titleMedium, color = c.inkSoft)
    }
    Spacer(Modifier.height(2.dp))
}
