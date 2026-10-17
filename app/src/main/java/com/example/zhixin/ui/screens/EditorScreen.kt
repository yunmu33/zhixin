package com.example.zhixin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zhixin.ai.AiClient
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.Category
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Mood
import com.example.zhixin.ui.components.CardShape
import com.example.zhixin.ui.components.CategoryRow
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.MoodPicker
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.TagChip
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    initial: Entry?,
    presetText: String = "",
    presetCategory: String = Category.DAILY.key,
    presetDateKey: String = "",
    prompt: String = "",
    settings: AppSettings,
    onSave: (Entry) -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var category by remember { mutableStateOf(initial?.category ?: presetCategory) }
    var mood by remember { mutableStateOf(initial?.mood) }
    var score by remember { mutableFloatStateOf((initial?.score ?: 5).toFloat()) }
    var text by remember { mutableStateOf(initial?.text ?: presetText) }
    var summary by remember { mutableStateOf(initial?.summary ?: "") }
    val tags = remember { mutableStateListOf<String>().apply { initial?.tags?.let { addAll(it) } } }
    var loading by remember { mutableStateOf(false) }

    // 顶栏固定在上面，只有下面的内容滚动
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        ScreenTopBar(
            title = if (initial == null) "✏️ 写点什么" else "✏️ 编辑这个故事",
            subtitle = if (initial == null) "写完就收进收藏本" else "改完记得点保存",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        if (prompt.isNotBlank()) {
            Text("💭 $prompt", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
        }

        SoftCard {
            Text("放进哪个抽屉？", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
            Spacer(Modifier.height(10.dp))
            CategoryRow(selected = category, onSelect = { category = it.key })
        }

        SoftCard {
            Text("今天的心情贴纸", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
            Spacer(Modifier.height(10.dp))
            MoodPicker(selected = mood, onSelect = {
                mood = it.key
                score = it.score.toFloat()
            })
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            placeholder = { Text("想说什么就写什么，小屋会认真读完…", color = c.inkSoft) },
            shape = ChipShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.line,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "\uD83D\uDE36‍\uD83D\uDCA8 情绪分（1-10）",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft
                )
                Spacer(Modifier.weight(1f))
                Text(score.roundToInt().toString(), style = MaterialTheme.typography.titleMedium, color = c.accent)
            }
            Slider(
                value = score,
                onValueChange = { score = it },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = c.accent,
                    activeTrackColor = c.accent,
                    inactiveTrackColor = c.line
                )
            )
            Text("越低越低落，越高越开心", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
        }

        if (settings.tags.isNotEmpty()) {
            SoftCard {
                Text("🏷 活动 / 标签", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.tags.forEach { tag ->
                        TagChip(
                            text = tag,
                            selected = tags.contains(tag),
                            onClick = { if (tags.contains(tag)) tags.remove(tag) else tags.add(tag) }
                        )
                    }
                }
            }
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "💌 小屋整理记录",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft,
                    modifier = Modifier.weight(1f)
                )
                TinyButton(if (loading) "整理中…" else "🔄 换一个视角") {
                    if (loading || text.isBlank()) return@TinyButton
                    scope.launch {
                        loading = true
                        summary = AiClient.summarize(text, mood, settings.listenOnly, settings)
                        loading = false
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (summary.isBlank()) {
                Text(
                    "写完上面的故事后，点「换一个视角」，小屋会帮你温柔整理。",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft
                )
            } else {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = c.ink)
            }
        }

        PrimaryButton(
            text = if (initial == null) "存进 📒 收藏本" else "保存修改",
            onClick = {
                if (text.isBlank() && mood == null) return@PrimaryButton
                val now = System.currentTimeMillis()
                val time = when {
                    initial != null -> initial.time
                    presetDateKey.isNotBlank() ->
                        com.example.zhixin.data.DateKey.toMillis(presetDateKey) + 12 * 60 * 60 * 1000L
                    else -> now
                }
                onSave(
                    Entry(
                        id = initial?.id ?: "",
                        time = time,
                        dateKey = initial?.dateKey ?: presetDateKey,
                        mood = mood,
                        score = score.roundToInt(),
                        category = category,
                        text = text.trim(),
                        summary = summary,
                        tags = tags.toList()
                    )
                )
            }
        )

        if (settings.listenOnly) {
            Text(
                "🎧 现在是「只倾听模式」：小屋只温柔接住你的话，不做分析、不给建议。",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkSoft
            )
        }

        Spacer(Modifier.height(20.dp))
        }
    }
}
