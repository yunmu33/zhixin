package com.example.zhixin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Heat
import com.example.zhixin.data.LevelStep
import com.example.zhixin.ui.theme.LocalAppColors

/**
 * 当前等级明示：大图标 + Lv + 等级名 + 百分比 + 进度条 + 离下一级还差多少。
 * 「我的」页和用户信息页共用。
 */
@Composable
fun LevelNowBlock(totalHeat: Int) {
    val c = LocalAppColors.current
    val step = Heat.levelStep(totalHeat)
    val next = Heat.nextLevelStep(totalHeat)
    val percent = (Heat.progress(totalHeat) * 100).toInt()
    // 打开页面时进度条自己涨到当前位置，看着更明白现在攒到哪了
    val bar by animateFloatAsState(
        targetValue = Heat.progress(totalHeat),
        animationSpec = tween(700),
        label = "levelBar"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.cardAlt),
            contentAlignment = Alignment.Center
        ) {
            Text(step.emoji, fontSize = 26.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Lv.${step.index}",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.accent,
                    modifier = Modifier
                        .border(1.dp, c.accent, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    step.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.ink
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                "累计 ${String.format("%,d", totalHeat)} 热度",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
        }
        Text(
            "$percent%",
            style = MaterialTheme.typography.titleMedium,
            color = c.accent
        )
    }
    Spacer(Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(c.line)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(bar.coerceIn(0.02f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(c.accent)
        )
    }
    Spacer(Modifier.height(6.dp))
    Text(
        if (next != null) {
            "离「${next.name}」还差 ${String.format("%,d", Heat.needToNext(totalHeat))} 热度" +
                " · 到 ${String.format("%,d", next.at)} 就升级"
        } else "已经到顶了，你太稳了！",
        style = MaterialTheme.typography.labelSmall,
        color = c.inkSoft
    )
}

/**
 * 各等级一览：每个等级要攒多少热度、哪些已经点亮、现在卡在哪一级。
 * 「我的」页和用户信息页共用。
 */
@Composable
fun LevelLadderCard(totalHeat: Int, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val nowIndex = Heat.levelIndex(totalHeat)
    SoftCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("\uD83E\uDE93", fontSize = 18.sp)
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "各等级",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.ink
                )
                Text(
                    "每记一次 +${Heat.BASE} 热度，连续打卡还有额外加成",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
            Text(
                "${Heat.levelStep(totalHeat).name} · ${nowIndex}/${Heat.LEVELS.size - 1}",
                style = MaterialTheme.typography.labelSmall,
                color = c.accent
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Heat.LEVELS.forEach { s ->
                LevelLadderRow(totalHeat = totalHeat, step = s, isNow = s.index == nowIndex)
            }
        }
    }
}

@Composable
private fun LevelLadderRow(totalHeat: Int, step: LevelStep, isNow: Boolean) {
    val c = LocalAppColors.current
    val s = step
    val reached = totalHeat >= s.at
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isNow) c.accentSoft else Color.Transparent)
            .border(
                width = if (isNow) 1.dp else 0.dp,
                color = if (isNow) c.accent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            s.emoji,
            fontSize = 20.sp,
            modifier = Modifier.alpha(if (reached) 1f else 0.35f)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reached) c.ink else c.inkSoft
                )
                if (isNow) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "当前",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.accent,
                        modifier = Modifier
                            .border(1.dp, c.accent, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                when {
                    s.at == 0 -> "刚起步，记第一笔就开始"
                    reached -> "累计 ${String.format("%,d", s.at)} 热度 · 已点亮"
                    else -> "累计 ${String.format("%,d", s.at)} 热度 · 还差 " +
                        String.format("%,d", s.at - totalHeat)
                },
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
        }
        Text(
            when {
                isNow -> "${(Heat.progress(totalHeat) * 100).toInt()}%"
                reached -> "✓"
                else -> "\uD83D\uDD12"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (reached) c.accent else c.inkSoft
        )
    }
}
