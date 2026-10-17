package com.example.zhixin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.StatCard
import com.example.zhixin.ui.theme.LocalAppColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable

private val EXAMPLES = listOf(
    "今天学会了用快捷键整理表格…",
    "喝到一杯超好喝的桂花拿铁☕",
    "路上看到了很美的晚霞🌇",
    "终于搞懂了那个困扰很久的问题",
    "第一次尝试做了新菜，居然成功了！",
    "听到一首超好听的歌，单曲循环一整天",
    "朋友说我今天气色很好✨"
)

@Composable
fun NewScreen(
    entries: List<Entry>,
    onOpenEntry: (Entry) -> Unit,
    onQuickSave: (String) -> Unit,
    onUpdateEntry: (Entry) -> Unit,
    onAdd: () -> Unit
) {
    val c = LocalAppColors.current
    val today = DateKey.fromMillis(System.currentTimeMillis())

    val news = remember(entries) {
        entries.filter { it.category == Category.NEW.key }.sortedByDescending { it.time }
    }
    val hasToday = news.any { it.dateKey == today }
    val newStreak = remember(news) {
        val keys = news.map { it.dateKey }.toSet()
        var cursor = System.currentTimeMillis()
        if (DateKey.fromMillis(cursor) !in keys) cursor = DateKey.addDays(cursor, -1)
        var n = 0
        while (DateKey.fromMillis(cursor) in keys) {
            n++
            cursor = DateKey.addDays(cursor, -1)
        }
        n
    }

    val randomExample = remember { EXAMPLES.random() }
    var draft by remember { mutableStateOf("") }
    var editingEntry by remember { mutableStateOf<Entry?>(null) }
    var editText by remember { mutableStateOf("") }

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
                    Text("🌱 日日新", style = MaterialTheme.typography.titleLarge, color = c.ink)
                }
                Text(
                    "活三百六十五天，而不是把同一天重复三百六十五次",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            }
        }

        item {
            SoftCard {
                if (hasToday) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "今天已经记录啦 ✓",
                                style = MaterialTheme.typography.titleSmall,
                                color = c.ink
                            )
                            Text(
                                "今天又成为自己生活里的主角了吗？",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.inkSoft
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = "回顾昨天，期待每天",
                        enabled = false,
                        onClick = {}
                    )
                } else {
                    Text(
                        "今天的新收获是什么？",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "一句玩笑、一件小事、尽管是浑浑噩噩的一天，都算",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        placeholder = { Text(randomExample, color = c.inkSoft) },
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
                        text = "记下今天的收获 🌱",
                        onClick = {
                            val text = draft.trim()
                            if (text.isBlank()) return@PrimaryButton
                            onQuickSave(text)
                            draft = ""
                        }
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("🔥", "${newStreak}天", "日日记录，慢慢成长", Modifier.weight(1f))
                StatCard("📚", "${news.size}条", "日日有新，慢慢变厚", Modifier.weight(1f))
            }
        }

        if (news.isEmpty()) {
            item { EmptyHint("🌱", "还没有新收获。哪怕只是今天喝到一杯好喝的茶，也值得记一笔～") }
        } else {
            item {
                Text(
                    "🌿 收获时间线",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(news) { entry ->
                SoftCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingEntry = entry
                            editText = entry.text
                        }
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            DateKey.monthDayLabel(entry.time),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.accent,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            entry.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                "给自己这一天取一个响亮的title吧！",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    if (editingEntry != null) {
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            containerColor = c.card,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("编辑收获", style = MaterialTheme.typography.titleSmall, color = c.ink)
            },
            text = {
                Column {
                    Text(
                        DateKey.monthDayLabel(editingEntry!!.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
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
                    val entry = editingEntry!!
                    onUpdateEntry(entry.copy(text = editText.trim()))
                    editingEntry = null
                }) {
                    Text("保存", color = c.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text("取消", color = c.inkSoft)
                }
            }
        )
    }
}
