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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.zhixin.data.CustomItem
import com.example.zhixin.data.TASK_CATEGORIES
import com.example.zhixin.data.TASK_DIFFICULTIES
import com.example.zhixin.data.difficultyReward
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.theme.LocalAppColors

/**
 * 添加任务页（右上角「＋」进入）：
 * 名称、图标、颜色、难度（带努力值）、分类、激励语一次填好，
 * 打卡时段 / 每日目标次数可以在任务页里再改。
 */
@Composable
fun AddTaskScreen(
    onConfirm: (CustomItem) -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current

    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(TASK_EMOJIS.first()) }
    var colorIndex by remember { mutableIntStateOf(-1) }
    var difficulty by remember { mutableStateOf("中等") }
    var category by remember { mutableStateOf("任意") }
    var note by remember { mutableStateOf("") }

    val accent = if (colorIndex >= 0) {
        Color(TASK_COLORS[colorIndex % TASK_COLORS.size])
    } else c.accent

    // 顶栏固定在上面，只有下面的内容滚动
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        ScreenTopBar(
            title = "\uD83C\uDFAF 添加任务",
            subtitle = "把想坚持的小事放进来",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // ── 名称 + 预览 ──
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 28.sp) }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("名称，例如：喝水", color = c.inkSoft) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = c.line,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        }

        // ── 图标 ──
        SoftCard {
            Text(
                "\uD83E\uDDE9 挑选图标：",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "往左滑动选择更多图标",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TASK_EMOJIS.forEach { e ->
                    val picked = e == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (picked) accent.copy(alpha = 0.18f) else c.cardAlt)
                            .border(1.5.dp, if (picked) accent else c.line, RoundedCornerShape(14.dp))
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) { Text(e, fontSize = 20.sp) }
                }
            }
        }

        // ── 颜色 ──
        SoftCard {
            Text(
                "\uD83D\uDCD0 挑选颜色：",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "不挑就按顺序自动分配",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorDot(color = null, picked = colorIndex < 0) { colorIndex = -1 }
                TASK_COLORS.indices.forEach { i ->
                    ColorDot(color = Color(TASK_COLORS[i]), picked = colorIndex == i) {
                        colorIndex = i
                    }
                }
            }
        }

        // ── 难度 ──
        SoftCard {
            Text(
                "\uD83C\uDFAE 难度（决定努力值）",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TASK_DIFFICULTIES.forEach { d ->
                    val picked = d == difficulty
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (picked) accent else c.cardAlt)
                            .border(1.dp, if (picked) accent else c.line, RoundedCornerShape(12.dp))
                            .clickable { difficulty = d }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$d ${difficultyReward(d)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (picked) Color.White else c.inkSoft
                        )
                    }
                }
            }
        }

        // ── 分类 ──
        SoftCard {
            Text(
                "\uD83C\uDFF7 任务分类：",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TASK_CATEGORIES.forEach { cat ->
                    val picked = cat == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (picked) accent else c.cardAlt)
                            .border(1.dp, if (picked) accent else c.line, RoundedCornerShape(12.dp))
                            .clickable { category = cat }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (picked) Color.White else c.inkSoft
                        )
                    }
                }
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
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                singleLine = false,
                modifier = Modifier.fillMaxWidth().height(110.dp),
                placeholder = {
                    Text("可不写，比如「早点入睡，就能早起。」", color = c.inkSoft)
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = c.line,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }

        PrimaryButton(
            text = "添加任务",
            enabled = name.isNotBlank(),
            onClick = {
                onConfirm(
                    CustomItem(
                        emoji = emoji,
                        name = name.trim(),
                        colorIndex = colorIndex,
                        difficulty = difficulty,
                        reward = difficultyReward(difficulty),
                        category = category,
                        note = note.trim()
                    )
                )
            }
        )

        Text(
            "打卡时段、每日目标次数等，可以在任务页里再改 ✨",
            style = MaterialTheme.typography.labelSmall,
            color = c.inkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        }
    }
}
