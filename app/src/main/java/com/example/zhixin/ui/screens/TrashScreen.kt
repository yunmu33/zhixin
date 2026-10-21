package com.example.zhixin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.TodoItem
import com.example.zhixin.data.TodoZone
import java.util.Calendar
import com.example.zhixin.ui.components.ConfirmDialog
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 回收站整页：右上角「清空回收站」，下面先列出被删掉的记录，再列出被删掉的打卡任务。
 * 记录可以恢复 / 彻底删除，任务可以整条恢复。
 */
@Composable
fun TrashScreen(
    trash: List<Entry>,
    onRestore: (Entry) -> Unit,
    onDeleteForever: (Entry) -> Unit,
    /** 被删掉的待办（清掉已做 / 单条删除进来的）。 */
    trashTodos: List<TodoItem>,
    onRestoreTodo: (TodoItem) -> Unit,
    onDeleteTodoForever: (TodoItem) -> Unit,
    onEmpty: () -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    val total = trash.size + trashTodos.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 让开状态栏 / 刘海和底部手势条，返回键和内容都不会被系统栏压住
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
    ) {
        // 和其它二级页面用同一个顶栏：固定在上面，右边挂「清空回收站」
        ScreenTopBar(
            title = "\uD83D\uDDD1 回收站",
            subtitle = if (total == 0) "还没有删掉的记录" else "$total 条记录等着被找回",
            onBack = onBack,
            trailing = {
                if (total > 0) {
                    TinyButton(text = "清空回收站") { confirmEmpty = true }
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        if (total == 0) {
            EmptyHint(
                emoji = "\uD83E\uDDF9",
                text = "回收站空空的\n删掉的记录会先放到这里，随时能找回来"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 先是被删掉的待办
                if (trashTodos.isNotEmpty()) {
                    item {
                        Text(
                            "\uD83D\uDDD3 待办 ${
                                trashTodos.size
                            } 条",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink
                        )
                    }
                    items(trashTodos, key = { it.id }) { todo ->
                        TrashTodoRow(
                            todo = todo,
                            onRestore = { onRestoreTodo(todo) },
                            onDeleteForever = { onDeleteTodoForever(todo) }
                        )
                    }
                }
                if (trash.isNotEmpty()) {
                    item {
                        Text(
                            "\uD83D\uDCC4 记录 ${trash.size} 条",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.ink
                        )
                    }
                    items(trash, key = { it.id }) { entry ->
                        TrashRow(
                            entry = entry,
                            onRestore = { onRestore(entry) },
                            onDeleteForever = { onDeleteForever(entry) }
                        )
                    }
                }
                // 底部：清空所有数据
                item {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TinyButton(text = "🗑️ 清空所有数据") {
                            confirmClearAll = true
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = "清空回收站？",
            message = "这里的东西会被彻底删除，找不回来了。",
            confirmText = "彻底清空",
            onConfirm = {
                confirmEmpty = false
                onEmpty()
            },
            onDismiss = { confirmEmpty = false }
        )
    }

    if (confirmClearAll) {
        ConfirmDialog(
            title = "清空所有数据？",
            message = "全部故事、自己加的打卡任务和标签、密码都会被删除，确定继续吗？",
            confirmText = "清空",
            onConfirm = {
                confirmClearAll = false
                onClearAll()
            },
            onDismiss = { confirmClearAll = false }
        )
    }
}

/** 回收站里的一条待办：能恢复到原来那天那一格。 */
@Composable
private fun TrashTodoRow(
    todo: TodoItem,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val c = LocalAppColors.current
    val zone = TodoZone.of(todo.zone)
    val tint = Color(zone.colorHex)
    val whenText = remember(todo.startKey(), todo.startAt, todo.endAt) {
        if (!todo.hasRange()) {
            DateKey.monthDayLabel(DateKey.toMillis(todo.startKey()))
        } else if (todo.startKey() == todo.endKey()) {
            "${DateKey.monthDayLabel(todo.startAt)} ${hm(todo.startAt)}–${hm(todo.endAt)}"
        } else {
            "${DateKey.monthDayLabel(todo.startAt)} ${hm(todo.startAt)} → " +
                "${DateKey.monthDayLabel(todo.endAt)} ${hm(todo.endAt)}"
        }
    }
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(zone.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    todo.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${zone.label} · $whenText${if (todo.done) " · 已完成" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tint
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    TinyButton(text = "恢复", onClick = onRestore)
                    Spacer(Modifier.width(8.dp))
                    TinyButton(text = "彻底删除", onClick = onDeleteForever)
                }
            }
        }
    }
}

private fun hm(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return String.format(
        Locale.US,
        "%02d:%02d",
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE)
    )
}

@Composable
private fun TrashRow(
    entry: Entry,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val c = LocalAppColors.current
    val category = Category.of(entry.category)
    val whenText = remember(entry.time) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.time))
    }
    val title = entry.summary.ifBlank {
        entry.text.replace("\n", " ").let {
            if (it.length > 40) it.take(40) + "…" else it
        }.ifBlank { category.label }
    }

    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(category.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.ink
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${category.short} · $whenText",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    TinyButton(text = "恢复", onClick = onRestore)
                    Spacer(Modifier.width(8.dp))
                    TinyButton(text = "彻底删除", onClick = onDeleteForever)
                }
            }
        }
    }
}


