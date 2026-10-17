package com.example.zhixin.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.ImageStore
import com.example.zhixin.data.Store
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.StatCard
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors

/**
 * 用户信息页：点「我的」的头像进来。
 * 右上角「编辑」切换编辑态，可以换头像（上传图片 / 选表情）、改昵称和签名。
 */
@Composable
fun ProfileScreen(
    settings: AppSettings,
    entries: List<Entry>,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var nameDraft by remember(settings.nickname) { mutableStateOf(settings.nickname) }
    var mottoDraft by remember(settings.motto) { mutableStateOf(settings.motto) }
    var emojiPicker by remember { mutableStateOf(false) }

    // 从相册挑一张做头像
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = ImageStore.saveFromUri(context, uri)
        if (name == null) {
            Toast.makeText(context, "这张图没读到，换一张试试", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        // 换掉上一张，别在手机里堆一堆旧头像
        if (settings.avatarFile.isNotBlank()) ImageStore.delete(context, settings.avatarFile)
        onChange(settings.copy(avatarFile = name))
        Toast.makeText(context, "头像换好啦 ✨", Toast.LENGTH_SHORT).show()
    }

    val totalHeat = remember(entries) { Heat.total(entries) }
    val dayCount = remember(entries) { entries.map { it.dateKey }.distinct().size }
    val newCount = remember(entries) { entries.count { it.category == Category.NEW.key } }
    val joinDays = remember(entries) {
        val first = entries.minByOrNull { it.time }?.time
        if (first == null) 0 else DateKey.daysBetween(first, System.currentTimeMillis()) + 1
    }

    // 顶栏固定，下面内容滚动。
    // 这页在主界面 Scaffold 里，系统栏的留白外面已经算过了，这里不能再叠一层
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "\uD83D\uDC64 我的信息",
            subtitle = "头像、昵称、签名都在这里改",
            onBack = onBack,
            trailing = {
                TinyButton(if (editing) "完成" else "编辑") {
                    if (editing) {
                        onChange(
                            settings.copy(
                                nickname = nameDraft.trim().ifBlank { "旅人" },
                                motto = mottoDraft.trim()
                            )
                        )
                        editing = false
                    } else {
                        nameDraft = settings.nickname
                        mottoDraft = settings.motto
                        editing = true
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // 头像 + 昵称
            SoftCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 只有点了右上「编辑」之后，点头像才弹选择弹窗（末尾 ➕ 从相册加照片）
                    MineAvatar(
                        settings = settings,
                        size = 96.dp,
                        onClick = if (editing) { { emojiPicker = true } } else null
                    )
                    Spacer(Modifier.height(12.dp))
                    if (editing) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
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
                            value = mottoDraft,
                            onValueChange = { mottoDraft = it },
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
                    } else {
                        Text(
                            settings.nickname.ifBlank { "旅人" },
                            style = MaterialTheme.typography.titleLarge,
                            color = c.ink
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            settings.motto.ifBlank { "每天都有新的收获" },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.accent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 档案数据
            SoftCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("\uD83D\uDCC5", "${joinDays}天", "加入小屋", Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    StatCard("\uD83D\uDD25", "${Store.streak(entries)}天", "连续打卡", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCard("📅", "${dayCount}天", "有记录的日子", Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    StatCard("\uD83C\uDF31", "${newCount}条", "新收获", Modifier.weight(1f))
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
                        "${Heat.levelEmoji(totalHeat)} 当前等级：${Heat.levelName(totalHeat)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        String.format("%,d", totalHeat),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.accent
                    )
                }
            }

            // 账号
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83D\uDD11", fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (settings.accountUser.isBlank()) "游客模式" else "已登录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink
                        )
                        Text(
                            if (settings.accountUser.isBlank()) {
                                "记录保存在这台设备上"
                            } else {
                                "用户名：${settings.accountUser}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (emojiPicker) {
        AlertDialog(
            onDismissRequest = { emojiPicker = false },
            containerColor = c.card,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("换个头像", style = MaterialTheme.typography.titleSmall, color = c.ink)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 末尾那个 ➕ 用来从相册添加照片
                    (AVATARS + AVATAR_ADD).chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { cell ->
                                if (cell == AVATAR_ADD) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(c.cardAlt)
                                            .border(1.5.dp, c.accent, RoundedCornerShape(16.dp))
                                            .clickable {
                                                emojiPicker = false
                                                pickImage.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) { Text(AVATAR_ADD, fontSize = 22.sp) }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (cell == settings.avatar && settings.avatarFile.isBlank()) {
                                                    c.accentSoft
                                                } else {
                                                    c.cardAlt
                                                }
                                            )
                                            .clickable {
                                                if (settings.avatarFile.isNotBlank()) {
                                                    ImageStore.delete(context, settings.avatarFile)
                                                }
                                                onChange(settings.copy(avatar = cell, avatarFile = ""))
                                                emojiPicker = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) { Text(cell, fontSize = 24.sp) }
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Text(
                        "点 ➕ 从相册挑一张照片当头像",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { emojiPicker = false }) {
                    Text("关闭", color = c.inkSoft)
                }
            }
        )
    }
}

/** 头像弹窗末尾的「加号」格子。 */
private const val AVATAR_ADD = "\u2795"
