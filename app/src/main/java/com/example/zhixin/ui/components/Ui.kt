package com.example.zhixin.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.ImageStore
import com.example.zhixin.data.COMMON_MOODS
import com.example.zhixin.data.Mood
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.categoryColor
import com.example.zhixin.ui.theme.moodColorOf
import com.example.zhixin.ui.theme.stickerOf

val CardShape = RoundedCornerShape(22.dp)
val ChipShape = RoundedCornerShape(12.dp)

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    container: Color = LocalAppColors.current.card,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = Modifier
        .fillMaxWidth()
        .then(modifier)
    Card(
        modifier = if (onClick != null) base.clickable { onClick() } else base,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(padding)) { content() }
    }
}

@Composable
fun SectionTitle(
    emoji: String,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LocalAppColors.current.ink)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.inkSoft
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** 二级页面的顶部返回栏：左侧方块返回键 + 标题。 */
@Composable
fun ScreenTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    /** 矮一点的版本：返回键小一号、标题和副标题都缩一档。 */
    compact: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    val c = LocalAppColors.current
    val pad = if (compact) 5.dp else 10.dp
    val btnSize = if (compact) 32.dp else 40.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 固定在页面顶部：铺一层背景，内容从下面滚过去也不会透出来
            .background(c.bg)
            .padding(horizontal = 16.dp, vertical = pad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(btnSize)
                .clip(RoundedCornerShape(12.dp))
                .background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(12.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "←",
                style = if (compact) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = c.ink
            )
        }
        Spacer(Modifier.width(if (compact) 10.dp else 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = if (compact) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = c.ink
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = if (compact) {
                        MaterialTheme.typography.labelSmall
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                    color = c.inkSoft
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun MoodPicker(
    selected: String?,
    onSelect: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    // 一排最多 4 个，这里只放常用的十二种
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        COMMON_MOODS.chunked(4).forEach { row ->
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { mood ->
            val on = selected == mood.key
            val color = moodColorOf(mood.key)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(CardShape)
                    .background(if (on) color.copy(alpha = 0.34f) else LocalAppColors.current.cardAlt)
                    .border(
                        width = if (on) 2.dp else 1.dp,
                        color = if (on) color else LocalAppColors.current.line,
                        shape = CardShape
                    )
                    .clickable { onSelect(mood) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 有手绘表情就用图，没有才回落 emoji
                val res = stickerOf(mood)
                if (res != null) {
                    Image(
                        painter = painterResource(res),
                        contentDescription = mood.label,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Text(mood.emoji, fontSize = 24.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    mood.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColors.current.ink,
                    textAlign = TextAlign.Center
                )
            }
                }
            }
        }
    }
}

@Composable
fun CategoryRow(
    selected: String,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Category.entries.toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c ->
                    val on = selected == c.key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(ChipShape)
                            .background(
                                if (on) categoryColor(c.key).copy(alpha = 0.32f)
                                else LocalAppColors.current.cardAlt
                            )
                            .border(
                                width = if (on) 2.dp else 1.dp,
                                color = if (on) categoryColor(c.key) else LocalAppColors.current.line,
                                shape = ChipShape
                            )
                            .clickable { onSelect(c) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${c.emoji} ${c.short}",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalAppColors.current.ink
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** 热度进度条。 */
@Composable
fun HeatBar(progress: Float, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(c.line)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(c.accent)
        )
    }
}

@Composable
fun TagChip(
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
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) c.accent else c.line,
                shape = ChipShape
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.accent else c.inkSoft
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = ChipShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = LocalAppColors.current.accent,
            contentColor = Color.White
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TinyButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(LocalAppColors.current.cardAlt)
            .border(1.dp, LocalAppColors.current.line, ChipShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.inkSoft)
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier, padding = PaddingValues(14.dp)) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = LocalAppColors.current.accent
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.inkSoft)
    }
}

@Composable
fun EmptyHint(emoji: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 34.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.inkSoft,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EntryCard(
    entry: Entry,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    val cat = Category.of(entry.category)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = c.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor(entry.category).copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(Mood.of(entry.mood)?.emoji ?: cat.emoji, fontSize = 19.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${DateKey.monthDayLabel(entry.time)} · ${DateKey.timeLabel(entry.time)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(cat.short, style = MaterialTheme.typography.labelSmall, color = categoryColor(entry.category))
                }
                if (entry.text.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.ink,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (entry.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.tags.take(4).forEach { t ->
                            Text(
                                "#$t",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 一张配图缩略图；传了 onClick 能点开看大图，传了 onRemove 就在右上角显示删除按钮。 */
@Composable
fun ImageThumb(
    name: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val bitmap = remember(name) { ImageStore.loadBitmap(context, name, 360) }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(c.cardAlt)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "配图",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("\uD83D\uDDBC", fontSize = 22.sp)
            }
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(c.card.copy(alpha = 0.85f))
                    .clickable(onClick = onRemove)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 12.sp, color = c.ink)
            }
        }
    }
}

/**
 * 配图区：一张都没有时是个大按钮，有图时是 3 列网格 + 「再加几张」。
 * 选中的图会先复制到 app 私有目录，images 里只存文件名。
 */
@Composable
fun ImagePickGrid(
    images: List<String>,
    onImages: (List<String>) -> Unit,
    maxImages: Int = 6,
    emptyHint: String = "点击添加图片",
    showCounter: Boolean = true,
    onPreview: ((String) -> Unit)? = null
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val pick = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxImages)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val room = maxImages - images.size
        if (room <= 0) {
            Toast.makeText(context, "最多只能带 $maxImages 张图", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val added = uris.take(room).mapNotNull { ImageStore.saveFromUri(context, it) }
        if (added.isNotEmpty()) onImages(images + added)
    }

    fun launch() = pick.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showCounter) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "配图（选填，最多 $maxImages 张）",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${images.size}/$maxImages",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (images.size >= maxImages) c.accent else c.inkSoft
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(c.cardAlt)
                    .clickable { launch() }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDDBC", fontSize = 22.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        emptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSoft
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                images.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { name ->
                            ImageThumb(
                                name = name,
                                modifier = Modifier.weight(1f),
                                onClick = if (onPreview != null) {
                                    { onPreview(name) }
                                } else {
                                    null
                                },
                                onRemove = {
                                    ImageStore.delete(context, name)
                                    onImages(images - name)
                                }
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (images.size < maxImages) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TinyButton(text = "＋ 再加几张") { launch() }
                }
            }
        }
    }
}

/** 全屏看大图：点空白处关闭，多张时左右箭头切换。 */
@Composable
fun ImageViewer(
    images: List<String>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val name = images.getOrNull(index) ?: return
    val bitmap = remember(name) { ImageStore.loadBitmap(context, name, 1600) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "大图",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 16.sp, color = Color.White)
            }
            if (images.size > 1) {
                ViewerArrow(
                    text = "\u2039",
                    alignment = Alignment.CenterStart,
                    enabled = index > 0
                ) { onIndexChange(index - 1) }
                ViewerArrow(
                    text = "\u203A",
                    alignment = Alignment.CenterEnd,
                    enabled = index < images.lastIndex
                ) { onIndexChange(index + 1) }
                Text(
                    "${index + 1}/${images.size}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ViewerArrow(
    text: String,
    alignment: Alignment,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (!enabled) return
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(horizontal = 12.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 20.sp, color = Color.White)
    }
}

/** 飘动的云朵 / 月亮星星装饰。 */
@Composable
fun SkyDecor(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "sky")
    val a by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(34000, easing = LinearEasing), RepeatMode.Restart),
        label = "a"
    )
    val b by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "b"
    )
    Box(modifier = modifier.fillMaxWidth().height(220.dp)) {
        Text(
            "☁", fontSize = 34.sp,
            modifier = Modifier.offset(x = (-40 + a * 380).dp, y = 16.dp).alpha(0.28f)
        )
        Text(
            "☁", fontSize = 22.sp,
            modifier = Modifier.offset(x = (300 - b * 340).dp, y = 92.dp).alpha(0.22f)
        )
        Text(
            "\uD83C\uDF1F", fontSize = 16.sp,
            modifier = Modifier.offset(x = (40 + b * 240).dp, y = 150.dp).alpha(0.25f)
        )
        Text(
            "\uD83C\uDF19", fontSize = 26.sp,
            modifier = Modifier.offset(x = (310 - a * 60).dp, y = 8.dp).alpha(0.20f)
        )
    }
}
