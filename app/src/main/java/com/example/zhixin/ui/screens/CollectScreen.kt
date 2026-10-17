package com.example.zhixin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.zhixin.data.Category
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Mood
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.EntryCard
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.categoryColor

private val DIARY_CATEGORIES = listOf(Category.STORY, Category.GOOD, Category.DAILY)

@Composable
fun CollectScreen(
    entries: List<Entry>,
    onOpenEntry: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
    onExport: () -> Unit,
    onAdd: () -> Unit
) {
    val c = LocalAppColors.current
    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var moodFilter by remember { mutableStateOf<String?>(null) }

    val diaryEntries = remember(entries) {
        entries.filter { it.category != Category.NEW.key }
    }

    val list = remember(diaryEntries, query, categoryFilter, moodFilter) {
        diaryEntries.filter { e ->
            val matchCat = categoryFilter == null || e.category == categoryFilter
            val matchMood = moodFilter == null || e.mood == moodFilter
            val q = query.trim()
            val matchQuery = q.isBlank() ||
                e.text.contains(q, true) ||
                e.summary.contains(q, true) ||
                e.tags.any { it.contains(q, true) }
            matchCat && matchMood && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("📔 小屋日记", style = MaterialTheme.typography.titleLarge, color = c.ink)
                    Text("${diaryEntries.size} 篇日记 · 记录生活的点点滴滴", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                }
            }
        }

        item {
            PrimaryButton(text = "✍️ 写一篇日记", onClick = onAdd)
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                singleLine = true,
                placeholder = { Text("搜索日记内容、标签…", color = c.inkSoft) },
                shape = ChipShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.line,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }

        // 心情筛选
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("按心情", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                // 心情种类多，横着滑着选
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        text = "全部",
                        selected = moodFilter == null,
                        color = c.accent
                    ) { moodFilter = null }
                    Mood.entries.forEach { mood ->
                        FilterChip(
                            text = mood.emoji,
                            selected = moodFilter == mood.key,
                            color = Color(mood.colorHex)
                        ) { moodFilter = if (moodFilter == mood.key) null else mood.key }
                    }
                }
            }
        }

        // 分类筛选
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("按分类", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(text = "全部", selected = categoryFilter == null, color = c.accent) {
                        categoryFilter = null
                    }
                    DIARY_CATEGORIES.forEach { cat ->
                        FilterChip(
                            text = "${cat.emoji} ${cat.short}",
                            selected = categoryFilter == cat.key,
                            color = categoryColor(cat.key)
                        ) { categoryFilter = if (categoryFilter == cat.key) null else cat.key }
                    }
                }
            }
        }

        if (list.isEmpty()) {
            item { EmptyHint("🍂", "还没有日记。点上面的按钮写下第一篇吧～") }
        } else {
            items(list) { entry ->
                EntryCard(
                    entry = entry,
                    onClick = { onOpenEntry(entry) },
                    onLongClick = { onDelete(entry) }
                )
            }
        }

        item {
            Text(
                "长按某条日记可以快速收进回收站",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.25f) else c.cardAlt)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.ink else c.inkSoft
        )
    }
}
