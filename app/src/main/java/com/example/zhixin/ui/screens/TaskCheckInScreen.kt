package com.example.zhixin.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.CUSTOM_TAG
import com.example.zhixin.data.CustomItem
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.ImageStore
import com.example.zhixin.data.Store
import com.example.zhixin.ui.components.CardShape
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.HeatBar
import com.example.zhixin.ui.components.ImageThumb
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SectionTitle
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors

/** 一次打卡最多带几张图。 */
private const val MAX_IMAGES = 6

/**
 * 详细的任务打卡页（由 TasksScreen 上的"📝 去打卡"入口进入）：
 *  - 上半：日期日历，可选要打卡的日子（不支持未来）
 *  - 下半：选中那天的所有自定义任务 → 勾选 / 取消
 *  - 写一句打卡总结（可选）+ 上传图片（可选，最多 6 张）
 *  - 点"打卡"提交 → 展示"这项内容打卡成功"
 *
 *  onSubmit 一次性收：(dateKey, summary, newCompletedItems, images)，
 *  由 AppRoot 统一入库，图片文件名随 Entry 一起存 JSON。
 */
@Composable
fun TaskCheckInScreen(
    settings: AppSettings,
    entries: List<Entry>,
    /** 由任务主页日历选中的日期传入（完整日历在外面）。 */
    initialDateKey: String,
    onSubmit: (
        dateKey: String,
        summary: String,
        newCompletedItems: List<CustomItem>,
        images: List<String>
    ) -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val todayKey = DateKey.fromMillis(now)

    var selectedDate by remember { mutableStateOf(initialDateKey) }

    val streak = remember(entries) { Store.streak(entries) }
    val totalHeat = remember(entries) { Heat.total(entries) }
    val heatProgress = remember(totalHeat) { Heat.progress(totalHeat) }

    val selectedMillis = DateKey.toMillis(selectedDate)
    val dayGap = DateKey.daysBetween(selectedMillis, now)
    val dateLabel = when (dayGap) {
        0 -> "今天"
        1 -> "昨天"
        2 -> "前天"
        else -> DateKey.monthDayLabel(selectedMillis) + " " + DateKey.weekdayLabel(selectedMillis)
    }
    val selectedHeat = remember(entries, selectedDate) { Heat.dayHeat(entries, selectedDate) }
    val isFuture = selectedDate > todayKey

    // 选中那天已打过的任务名（tags 含 CUSTOM_TAG）
    val selectedCustomDone = remember(entries, selectedDate) {
        entries.filter {
            !it.deleted && it.dateKey == selectedDate && it.tags.contains(CUSTOM_TAG)
        }.flatMap { it.tags }.toSet()
    }
    // 本次勾选的任务名（切换日期时同步成那天已打的）
    val pending = remember { mutableStateListOf<String>() }
    remember(selectedDate, selectedCustomDone) {
        pending.clear()
        pending.addAll(settings.customItems.map { it.name }
            .filter { selectedCustomDone.contains(it) })
    }

    var summary by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }

    // ── 图片：文件名列表（文件已复制到私有目录）──
    val images = remember { mutableStateListOf<String>() }
    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = MAX_IMAGES
        )
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val room = MAX_IMAGES - images.size
        if (room <= 0) {
            Toast.makeText(context, "最多只能带 $MAX_IMAGES 张图", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        uris.take(room).forEach { uri ->
            val name = ImageStore.saveFromUri(context, uri)
            if (name != null) images.add(name)
        }
        Toast.makeText(context, "已添加 ${uris.take(room).size} 张图 🖼", Toast.LENGTH_SHORT).show()
    }

    // 提交后成功卡要展示的内容快照
    var successSummary by remember { mutableStateOf("") }
    var successNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var successImages by remember { mutableStateOf<List<String>>(emptyList()) }

    // 顶栏固定在上面，只有下面的内容滚动
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        ScreenTopBar(
            title = "\uD83D\uDCDD 任务打卡",
            subtitle = "挑个日子 → 选任务 → 写点什么 / 配图 → 打卡",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // ── 打卡日期（完整日历在任务主页，这里只显示 / 微调相邻天）──
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDCC5", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "正在给$dateLabel 打卡",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink
                    )
                    Text(
                        "${DateKey.monthDayLabel(selectedMillis)} " +
                            "${DateKey.weekdayLabel(selectedMillis)}" +
                            if (selectedHeat > 0) " · 当天热度 \uD83D\uDD25$selectedHeat"
                            else " · 这天还没打卡",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TinyButton(text = "‹ 前一天", onClick = {
                    selectedDate = DateKey.fromMillis(DateKey.addDays(selectedMillis, -1))
                    done = false
                })
                if (selectedDate != todayKey) {
                    TinyButton(text = "回到今天", onClick = {
                        selectedDate = todayKey
                        done = false
                    })
                }
                if (selectedDate < todayKey) {
                    TinyButton(text = "后一天 ›", onClick = {
                        selectedDate = DateKey.fromMillis(DateKey.addDays(selectedMillis, 1))
                        done = false
                    })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "要挑更远的日子，回任务主页在日历上点一下",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // ── 下半：选择打卡内容 / 输入 / 图片 / 提交 ──
        if (done) {
            TaskCheckSuccessCard(
                dateLabel = dateLabel,
                dateDesc = DateKey.monthDayLabel(selectedMillis),
                summary = successSummary,
                doneNames = successNames,
                images = successImages,
                streak = streak,
                totalHeat = totalHeat,
                progress = heatProgress,
                onBackHome = onBack,
                onContinue = {
                    done = false
                    summary = ""
                    images.clear()
                }
            )
        } else {
            SoftCard {
                SectionTitle(
                    emoji = "\uD83C\uDFAF",
                    title = "$dateLabel 要打卡的内容",
                    subtitle = if (settings.customItems.isEmpty())
                        "还没有任务，去主页添加几个吧"
                    else
                        "点一下勾选 / 取消，已经打过的会自带勾选 ✨"
                )
                Spacer(Modifier.height(12.dp))

                if (settings.customItems.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(c.cardAlt)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "暂无任务，回到主页「任务打卡」添加吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.inkSoft,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        settings.customItems.forEach { item ->
                            val checked = pending.contains(item.name)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CardShape)
                                    .background(
                                        if (checked) c.accent.copy(alpha = 0.18f)
                                        else c.cardAlt
                                    )
                                    .border(
                                        width = if (checked) 1.5.dp else 1.dp,
                                        color = if (checked) c.accent else c.line,
                                        shape = CardShape
                                    )
                                    .clickable {
                                        if (checked) pending.remove(item.name)
                                        else pending.add(item.name)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (checked) "✅" else item.emoji,
                                        fontSize = 22.sp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (checked) c.accent else c.ink,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        if (checked) "已选" else "待选",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (checked) c.accent else c.inkSoft
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "写点什么（选填）",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (isFuture) "未来还没到，先去闯关吧～"
                            else "今天想对自己说什么？",
                            color = c.inkSoft
                        )
                    },
                    shape = ChipShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.line,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                // ── 图片 ──
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "配图（选填，最多 ${MAX_IMAGES} 张）",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.inkSoft,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${images.size}/$MAX_IMAGES",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (images.size >= MAX_IMAGES) c.accent else c.inkSoft
                    )
                }
                Spacer(Modifier.height(10.dp))

                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(c.cardAlt)
                            .clickable {
                                pickImages.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\uD83D\uDDBC", fontSize = 26.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "点击从相册选择图片",
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
                                        onRemove = {
                                            ImageStore.delete(context, name)
                                            images.remove(name)
                                        }
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (images.size < MAX_IMAGES) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TinyButton(
                                text = "＋ 再加几张",
                                onClick = {
                                    pickImages.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts
                                                .PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                if (isFuture) {
                    Text(
                        "未来还没到，不能打卡哦",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.accent,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    PrimaryButton(
                        text = if (pending.isEmpty())
                            "\uD83D\uDCDD 给$dateLabel 打卡"
                        else
                            "\uD83D\uDCDD 给$dateLabel 打卡（${pending.size}/${settings.customItems.size}）",
                        onClick = {
                            val doneNames = pending.toList()
                            val newItems = doneNames
                                .filter { !selectedCustomDone.contains(it) }
                                .map { name -> settings.customItems.first { it.name == name } }
                            successSummary = summary.trim()
                            successNames = doneNames
                            successImages = images.toList()
                            onSubmit(selectedDate, summary.trim(), newItems, images.toList())
                            done = true
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        }
    }
}



@Composable
private fun TaskCheckSuccessCard(
    dateLabel: String,
    dateDesc: String,
    summary: String,
    doneNames: List<String>,
    images: List<String>,
    streak: Int,
    totalHeat: Int,
    progress: Float,
    onBackHome: () -> Unit,
    onContinue: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("\uD83C\uDF89", fontSize = 52.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "打卡成功！",
                style = MaterialTheme.typography.headlineSmall,
                color = c.ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$dateLabel · $dateDesc",
                style = MaterialTheme.typography.labelMedium,
                color = c.inkSoft
            )
            Spacer(Modifier.height(10.dp))
            if (doneNames.isNotEmpty()) {
                Text(
                    "这项内容已打卡 ✓",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    doneNames.joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.accent,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
            }
            if (summary.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.cardAlt)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("「$summary」", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (images.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    images.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { name ->
                                val bitmap = remember(name) {
                                    ImageStore.loadBitmap(context, name, 360)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(c.cardAlt)
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "打卡配图",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            HeatBar(progress = progress)
            Spacer(Modifier.height(6.dp))
            Text(
                "\uD83D\uDD25 连续打卡 ${streak} 天 · 累计热度 ${totalHeat}",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton(text = "回任务主页", onClick = onBackHome)
            Spacer(Modifier.height(10.dp))
            TinyButton(text = "再打一次", onClick = onContinue)
        }
    }
}
