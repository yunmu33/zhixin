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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Category
import com.example.zhixin.data.Entry
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
    onEmpty: () -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    var confirmEmpty by remember { mutableStateOf(false) }
    val total = trash.size

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
                items(trash, key = { it.id }) { entry ->
                    TrashRow(
                        entry = entry,
                        onRestore = { onRestore(entry) },
                        onDeleteForever = { onDeleteForever(entry) }
                    )
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


