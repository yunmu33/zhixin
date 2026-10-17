package com.example.zhixin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Mood
import com.example.zhixin.data.parseHm
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.categoryColor
import com.example.zhixin.ui.theme.moodColorOf
import com.example.zhixin.ui.theme.stickerOf
import java.util.Locale

@Composable
fun EntryDetailDialog(
    entry: Entry,
    onDismiss: () -> Unit,
    onEdit: (Entry) -> Unit,
    onDelete: (Entry) -> Unit
) {
    val c = LocalAppColors.current
    val mood = Mood.of(entry.mood)
    val cat = Category.of(entry.category)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "${DateKey.monthDayLabel(entry.time)} ${DateKey.weekdayLabel(entry.time)} · ${DateKey.timeLabel(entry.time)}",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mood != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(moodColorOf(mood.key).copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val res = stickerOf(mood)
                                if (res != null) {
                                    Image(
                                        painter = painterResource(res),
                                        contentDescription = mood.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    mood.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.ink
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(categoryColor(entry.category).copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("${cat.emoji} ${cat.short}", style = MaterialTheme.typography.labelMedium, color = c.ink)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("情绪分 ${entry.score}", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                }
                if (entry.text.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("💬 我讲的小故事", style = MaterialTheme.typography.labelMedium, color = c.accent)
                    Spacer(Modifier.height(4.dp))
                    Text(entry.text, style = MaterialTheme.typography.bodyMedium, color = c.ink)
                }
                if (entry.summary.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("💌 小屋整理记录", style = MaterialTheme.typography.labelMedium, color = c.accent)
                    Spacer(Modifier.height(4.dp))
                    Text(entry.summary, style = MaterialTheme.typography.bodyMedium, color = c.ink)
                }
                if (entry.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.tags.forEach { t ->
                            Text("#$t", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEdit(entry) }) {
                Text("编辑", color = c.accent)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(entry) }) {
                    Text("删除", color = Color(0xFFD9736B))
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = c.inkSoft)
                }
            }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = c.inkSoft) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.inkSoft) }
        }
    )
}

@Composable
fun InputDialog(
    title: String,
    label: String,
    initial: String = "",
    singleLine: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth().then(if (!singleLine) Modifier.height(120.dp) else Modifier),
                label = { Text(label, color = c.inkSoft) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.line
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("确定", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.inkSoft) }
        }
    )
}

/** 时间选择弹窗（24 小时制），回调返回 "HH:mm"。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    val hm = parseHm(initial)
    val state = rememberTimePickerState(
        initialHour = hm?.div(60) ?: 8,
        initialMinute = hm?.mod(60) ?: 0,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
            }) { Text("确定", color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.inkSoft) }
        }
    )
}
