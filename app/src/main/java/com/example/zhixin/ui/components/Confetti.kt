package com.example.zhixin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** 礼花颜色：暖的冷的都来一点，热闹些。 */
private val CONFETTI_COLORS = listOf(
    Color(0xFFF2C55C), // 暖黄
    Color(0xFFE98B7A), // 橘粉
    Color(0xFF7FBFA8), // 青绿
    Color(0xFF9BB6D6), // 天蓝
    Color(0xFFD98CB3), // 粉
    Color(0xFFB0A6E8), // 淡紫
    Color(0xFF8FD1A8)  // 嫩绿
)

/** 一次礼花放多久（秒）。 */
private const val LIFE = 1.7f
/** 重力：每秒每秒往下掉多少个屏高。 */
private const val GRAVITY = 2.3f

/** 一小片纸屑。位置 / 速度都用「屏宽 / 屏高」的比例，换个屏幕也一样飞。 */
private data class Bit(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float,
    val color: Color,
    val spin: Float,
    val delay: Float,
    val round: Boolean
)

private fun makeBit(r: Random, fromLeft: Boolean): Bit = Bit(
    startX = if (fromLeft) -0.02f else 1.02f,
    // 从屏幕中间偏下的位置斜着往上喷，高低错开才铺得开
    startY = 0.28f + r.nextFloat() * 0.34f,
    vx = (if (fromLeft) 1f else -1f) * (0.22f + r.nextFloat() * 0.52f),
    vy = -(0.68f + r.nextFloat() * 0.68f),
    sizeDp = 6f + r.nextFloat() * 7f,
    color = CONFETTI_COLORS.random(r),
    spin = (if (r.nextBoolean()) 1f else -1f) * (200f + r.nextFloat() * 520f),
    delay = r.nextFloat() * 0.18f,
    round = r.nextFloat() < 0.25f
)

/**
 * 打卡成功的礼花：从屏幕左右两边斜着往上喷一把彩纸，一两秒后自己散掉。
 *
 * @param runId 每次打卡 +1，变了就重新放一次（同一个值不会重放）。
 */
@Composable
fun SideConfetti(runId: Int, modifier: Modifier = Modifier) {
    if (runId <= 0) return
    var elapsed by remember(runId) { mutableFloatStateOf(0f) }
    var alive by remember(runId) { mutableStateOf(true) }
    val bits = remember(runId) {
        val r = Random(runId)
        List(46) { makeBit(r, fromLeft = true) } +
            List(46) { makeBit(r, fromLeft = false) }
    }
    if (!alive) return

    LaunchedEffect(runId) {
        val start = withFrameNanos { it }
        while (true) {
            elapsed = (withFrameNanos { it } - start) / 1_000_000_000f
            if (elapsed >= LIFE) {
                alive = false
                break
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        bits.forEach { b ->
            val t = elapsed - b.delay
            if (t <= 0f) return@forEach
            val x = b.startX * w + b.vx * w * t
            val y = b.startY * h + b.vy * h * t + 0.5f * GRAVITY * h * t * t
            if (y > h + 60f) return@forEach
            // 最后一点点时间淡出，不会突然消失
            val alpha = (1f - (t - (LIFE - 0.45f)) / 0.45f).coerceIn(0f, 1f)
            if (alpha <= 0f) return@forEach
            val px = b.sizeDp.dp.toPx()
            rotate(degrees = b.spin * t, pivot = Offset(x, y)) {
                if (b.round) {
                    drawCircle(
                        color = b.color.copy(alpha = alpha),
                        radius = px / 2,
                        center = Offset(x, y)
                    )
                } else {
                    drawRect(
                        color = b.color.copy(alpha = alpha),
                        topLeft = Offset(x - px / 2, y - px / 4),
                        size = Size(px, px / 2)
                    )
                }
            }
        }
    }
}
