package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.AppSettings
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.dialogEnter

@Composable
fun LockScreen(settings: AppSettings, onUnlock: () -> Unit) {
    val c = LocalAppColors.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun press(key: String) {
        when {
            key == "清空" -> { input = ""; error = false }
            key == "⌫" -> { input = input.dropLast(1); error = false }
            input.length >= 8 -> Unit
            else -> {
                val next = input + key
                input = next
                error = false
                if (next.length >= settings.password.length) {
                    if (next == settings.password) onUnlock() else {
                        error = true
                        input = ""
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "\uD83C\uDFE1", fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text("我的情绪小屋", style = MaterialTheme.typography.titleLarge, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            if (error) "密码不对哦，再试一次～" else "请输入小屋的密码",
            style = MaterialTheme.typography.bodySmall,
            color = if (error) c.accent else c.inkSoft
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(settings.password.length.coerceAtLeast(4)) { i ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (i < input.length) c.accent else c.line)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "清空", "0", "⌫")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { k ->
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(c.card)
                                .clickable { press(k) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                k,
                                fontSize = if (k.length > 1) 16.sp else 24.sp,
                                color = c.ink,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "🔑 密码只是防误触，不是加密",
            style = MaterialTheme.typography.labelSmall,
            color = c.inkSoft
        )
    }
}

@Composable
fun GuideDialog(
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onClose: (neverShowAgain: Boolean) -> Unit
) {
    val c = LocalAppColors.current
    val steps = listOf(
        "⚡" to "今日打卡：主页点「去打卡」，选心情、勾事件，一键完成打卡。",
        "\uD83D\uDCC5" to "日历 · 热度：心情色块日历、年度热力图、情绪趋势都在这一页。",
        "\uD83C\uDF31" to "每日一新：每天记一条新收获，哪怕一句话也算数。",
        "\uD83D\uDCD2" to "收藏本：所有记录都在这里，可搜索、编辑、导出备份。",
        "\uD83D\uDC64" to "我的：注册登录后解锁头像昵称、成就徽章与数据备份。"
    )
    androidx.compose.material3.AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = { onClose(false) },
        containerColor = c.card,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        title = {
            Text("👋 欢迎来到知新小屋", style = MaterialTheme.typography.titleSmall, color = c.ink)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                steps.forEachIndexed { i, (emoji, text) ->
                    Row {
                        Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = c.accent)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$emoji $text", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (loggedIn) "账号已登录，随时可以在「我的」里查看你的档案。"
                    else "现在是游客模式，注册一个账号就能保存你的昵称、头像和成就。",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.accent
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { if (loggedIn) onClose(false) else onLogin() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    if (loggedIn) "我知道了" else "\uD83D\uDD11 注册 / 登录",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.accent
                )
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { onClose(true) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("不再自动显示", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
            }
        }
    )
}
