package com.example.zhixin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Category
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Mood
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.stickerOf
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/** 罐子里的一颗心情：哪天、什么心情、当天写的字。 */
private data class JarBean(
    val dateKey: String,
    val mood: Mood,
    val text: String,
    val diary: String
)

/**
 * 「心情罐」：把当月记下的心情一颗颗装进玻璃罐里。
 * - 罐子是半透明的，里面的心情像糖一样堆着，还会轻轻晃
 * - 进页面时会一颗一颗从瓶口掉进去（按日期顺序），看着像真的被装起来
 * - 点任意一颗：下面显示那天的日期、心情和当天写的话
 * - 「年」视图：一整年的心情装进同一个罐子，糖粒会自动变小变密
 */
@Composable
fun MoodJarScreen(
    entries: List<Entry>,
    initialMonth: Calendar? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val c = LocalAppColors.current

    var month by remember {
        mutableStateOf(initialMonth?.clone() as? Calendar ?: Calendar.getInstance())
    }
    // 按月装罐 / 按整年装罐
    var byYear by remember { mutableStateOf(false) }
    val year = month.get(Calendar.YEAR)
    val mon = month.get(Calendar.MONTH)
    val prefix = if (byYear) String.format(Locale.CHINA, "%04d", year)
    else String.format(Locale.CHINA, "%04d-%02d", year, mon + 1)

    // 当月有心情的日子，按日期排好顺序装罐
    val beans = remember(entries, prefix) {
        val diaries = entries.filter {
            !it.deleted && it.category == Category.DIARY.key
        }.associateBy { it.dateKey }
        entries
            .filter { !it.deleted && !it.mood.isNullOrBlank() && it.dateKey.startsWith(prefix) }
            .sortedBy { it.dateKey }
            .mapNotNull { e ->
                val mood = Mood.entries.firstOrNull { it.key == e.mood } ?: return@mapNotNull null
                JarBean(
                    dateKey = e.dateKey,
                    mood = mood,
                    text = e.text.trim(),
                    diary = diaries[e.dateKey]?.summary.orEmpty()
                        .ifBlank { diaries[e.dateKey]?.text.orEmpty() }
                )
            }
    }
    // 换月份时重新掉一遍
    var selected by remember(prefix) { mutableStateOf<JarBean?>(null) }
    LaunchedEffect(prefix) { selected = null }

    val counts = remember(beans) {
        beans.groupingBy { it.mood }.eachCount().toList().sortedByDescending { it.second }
    }
    val avgScore = remember(beans) {
        if (beans.isEmpty()) 0 else beans.sumOf { it.mood.score } / beans.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(c.cardAlt)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 22.sp, color = c.inkSoft)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "\uD83E\uDED9 心情罐",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink
                )
                Crossfade(
                    targetState = byYear,
                    animationSpec = tween(320),
                    label = "jarSubtitle"
                ) { y ->
                    Text(
                        if (y) "这一整年的心情都装进来啦" else "${mon + 1} 月的心情都装进来啦",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        // 月份切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (byYear) "%d年".format(year) else "%d年%d月".format(year, mon + 1),
                style = MaterialTheme.typography.titleMedium,
                color = c.ink,
                modifier = Modifier.weight(1f)
            )
            // 按月 / 按年
            listOf(false to "月", true to "年").forEach { (v, label) ->
                val pillBg by animateColorAsState(
                    if (byYear == v) c.inkSoft.copy(alpha = 0.24f) else c.cardAlt,
                    animationSpec = tween(280),
                    label = "pillBg"
                )
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBg)
                        .clickable { byYear = v }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            listOf("‹", "›", "今").forEachIndexed { i, label ->
                if (i > 0) Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.cardAlt)
                        .clickable {
                            val next = month.clone() as Calendar
                            when (label) {
                                // 按年看的时候，左右是换年
                                "‹" -> if (byYear) next.add(Calendar.YEAR, -1) else next.add(Calendar.MONTH, -1)
                                "›" -> if (byYear) next.add(Calendar.YEAR, 1) else next.add(Calendar.MONTH, 1)
                                else -> next.timeInMillis = System.currentTimeMillis()
                            }
                            month = next
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft,
                        fontSize = 19.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        // ── 玻璃罐：月 / 年共用一个罐子，年就是把一整年都装进来 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // 空罐也走同一个 Jar：罐子大小始终跟月/年模式走
            Jar(
                beans = beans,
                selected = selected,
                byYear = byYear,
                reloadKey = prefix,
                onPick = { selected = if (selected?.dateKey == it.dateKey) null else it }
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83C\uDF6C",
                title = "罐子里",
                value = "${beans.size} 颗"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                emoji = "\uD83C\uDF21",
                title = "心情温度",
                value = if (beans.isEmpty()) "—" else "$avgScore/10"
            )
        }

        if (counts.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.card)
                    .padding(14.dp)
            ) {
                Text(
                    if (byYear) "这一年最常出现" else "这个月最常出现",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft
                )
                Spacer(Modifier.height(10.dp))
                counts.take(4).forEach { (mood, n) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val res = stickerOf(mood)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(mood.colorHex).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (res != null) {
                                Image(painterResource(res), null, Modifier.size(24.dp))
                            } else {
                                Text(mood.emoji, fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            mood.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink,
                            modifier = Modifier.width(48.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(c.cardAlt)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(n.toFloat() / beans.size)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(mood.colorHex).copy(alpha = 0.75f))
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "$n 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                }
            }
        }

        // 选中一颗：显示那天的字
        selected?.let { bean ->
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.cardAlt)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val res = stickerOf(bean.mood)
                    if (res != null) {
                        Image(
                            painterResource(res), null,
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(bean.mood.colorHex).copy(alpha = 0.25f))
                                .padding(4.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            dayText(bean.dateKey),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.ink
                        )
                        Text(
                            bean.mood.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(bean.mood.colorHex)
                        )
                    }
                    Text(
                        "点一下收起",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
                val note = bean.text.ifBlank { bean.diary }
                if (note.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        note.take(120) + if (note.length > 120) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

/** 10月5日 周日 这种写法。 */
private fun dayText(dateKey: String): String {
    val parts = dateKey.split("-")
    if (parts.size < 3) return dateKey
    val cal = Calendar.getInstance().apply {
        set(parts[0].toIntOrNull() ?: return dateKey, parts[1].toIntOrNull()?.minus(1) ?: 0,
            parts[2].toIntOrNull() ?: return dateKey)
    }
    val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[
        cal.get(Calendar.DAY_OF_WEEK) - 1
    ]
    return "${parts[1].toInt()}月${parts[2].toInt()}日 $week"
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    value: String
) {
    val c = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .padding(14.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = c.ink,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private val JarLidH = 24.dp
private val JarNeckH = 10.dp
private val JarGlass = Color(0xFFEFE9DE)   // 玻璃本体的奶白米色
private val JarLid = Color(0xFF3D3D45)     // 金属盖子深灰

// 月视图：罐子小一点、糖果大一点；年视图：罐子大一点、糖粒小一点
private val MonthJarFill = 0.84f           // 占可用宽度的比例
private val MonthJarW = 270.dp
private val MonthJarH = 280.dp
private val MonthBean = 46.dp
private val YearJarFill = 0.96f
private val YearJarW = 320.dp
private val YearJarH = 350.dp
private val YearBean = 20.dp

/**
 * Mason jar：金属盖 + 螺纹 + 短颈 + 圆肚子玻璃罐 + 落地阴影。
 * inside 画在罐肚子里（会被罐身裁剪）；overlay 浮在整个罐子上层，
 * 心情糖从瓶口掉下来时才能在颈部露出来。
 */
@Composable
private fun JarShape(
    fillFrac: Float = MonthJarFill,
    jarW: Dp = MonthJarW,
    bodyH: Dp = MonthJarH,
    inside: @Composable BoxWithConstraintsScope.() -> Unit = {},
    overlay: @Composable BoxWithConstraintsScope.(bodyH: Dp) -> Unit = {}
) {
    val c = LocalAppColors.current
    val bodyShape = RoundedCornerShape(
        topStart = 22.dp, topEnd = 22.dp, bottomStart = 38.dp, bottomEnd = 38.dp
    )
    val neckShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
    val lidShape = RoundedCornerShape(8.dp)
    val neckW = jarW * 0.56f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(fillFrac)
            .widthIn(max = jarW),
        contentAlignment = Alignment.TopCenter
    ) {
        val jarScope = this
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // —— 金属盖 ——
            Box(
                modifier = Modifier
                    .width(neckW + 22.dp)
                    .height(JarLidH)
                    .clip(lidShape)
                    .background(JarLid)
            ) {
                // 顶部亮边
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.TopCenter)
                        .background(Color.White.copy(alpha = 0.32f))
                )
                // 中间螺纹线
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.6.dp)
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.30f))
                )
                // 下方一道浅色高光
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.10f))
                )
            }
            // —— 短颈 ——
            Box(
                modifier = Modifier
                    .width(neckW)
                    .height(JarNeckH)
                    .clip(neckShape)
                    .background(JarGlass.copy(alpha = 0.60f))
                    .border(1.dp, c.inkSoft.copy(alpha = 0.18f), neckShape)
            )
            // —— 圆肚玻璃罐 ——
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bodyH)
                    .clip(bodyShape)
                    .background(JarGlass.copy(alpha = 0.45f))
                    .border(1.6.dp, c.inkSoft.copy(alpha = 0.20f), bodyShape)
            ) {
                inside()
                // 左上一条斜高光（用竖条窄长模拟，alpha 低一些）
                Box(
                    Modifier
                        .size(width = 12.dp, height = 70.dp)
                        .offset(x = 22.dp, y = 18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .alpha(0.55f)
                        .background(Color.White)
                )
                // 左下小弧
                Box(
                    Modifier
                        .size(width = 6.dp, height = 28.dp)
                        .offset(x = 18.dp, y = (maxHeight - 60.dp).coerceAtLeast(40.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .alpha(0.35f)
                        .background(Color.White)
                )
                // 右侧一条更细的高光
                Box(
                    Modifier
                        .size(width = 3.dp, height = 90.dp)
                        .offset(x = maxWidth - 18.dp, y = 30.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .alpha(0.28f)
                        .background(Color.White)
                )
            }
            // —— 落地阴影 ——
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .width(jarW * 0.72f)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.08f))
            )
        }
        // 掉落动画浮在整个罐子上层
        Box(
            Modifier
                .fillMaxWidth()
                .height(JarLidH + JarNeckH + bodyH)
        ) {
            jarScope.overlay(bodyH)
        }
    }
}

/**
 * 玻璃罐本体：盖子 + 短颈 + 圆肚，心情糖从瓶口一颗颗涌出来，掉进罐里堆成小山。
 */
@Composable
private fun Jar(
    beans: List<JarBean>,
    selected: JarBean?,
    byYear: Boolean,
    reloadKey: String,
    onPick: (JarBean) -> Unit
) {
    val c = LocalAppColors.current
    // 轻轻晃：所有糖共用同一个正弦波，各自相位不同
    val wave = rememberInfiniteTransition(label = "jarWave")
    val t by wave.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "wave"
    )
    // 月/年切换：罐子大小平滑缩放
    val jarSizeSpec = tween<Dp>(450, easing = FastOutSlowInEasing)
    val fillFrac by animateFloatAsState(
        if (byYear) YearJarFill else MonthJarFill,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "jarFill"
    )
    val jarW by animateDpAsState(if (byYear) YearJarW else MonthJarW, jarSizeSpec, label = "jarW")
    val bodyH by animateDpAsState(if (byYear) YearJarH else MonthJarH, jarSizeSpec, label = "jarH")
    // 切换月/年时先等罐子缩放完，再开始倒糖
    var prevByYear by remember { mutableStateOf(byYear) }
    val jarResized = prevByYear != byYear
    SideEffect { prevByYear = byYear }

    JarShape(fillFrac = fillFrac, jarW = jarW, bodyH = bodyH) { h ->
        // 空罐：罐子照常画（大小跟当前模式一致），里面只放提示文字
        if (beans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "空空的小屋",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (byYear) "这一年还没记心情\n去月历上点一天、贴个表情过来"
                        else "这个月还没记心情\n去月历上点一天、贴个表情过来",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@JarShape
        }
        val sideMargin = 10.dp
        val jarW = maxWidth
        val usableW = jarW - sideMargin * 2
        val n = beans.size
        // —— 堆法：糖果不多堆宽底小山；整年装一罐时堆成密密的三角糖粒 ——
        var beanSize = if (byYear) YearBean else MonthBean
        var unit: Dp = beanSize
        var counts: List<Int> = emptyList()
        if (n <= 45) {
            var bs = if (byYear) YearBean else MonthBean
            while (true) {
                unit = bs * 0.95f                  // 相互贴着，堆出满满一罐的感觉
                val perRow0 = (usableW / unit).toInt().coerceAtLeast(1)
                val maxRows = ((h - 20.dp) / (unit * 0.85f)).toInt().coerceAtLeast(1)
                val tmp = mutableListOf<Int>()
                var rem = n
                var w = perRow0
                while (rem > 0 && tmp.size < maxRows) {
                    val take = minOf(rem, w)
                    tmp.add(take)
                    rem -= take
                    w = maxOf(1, w - 1)            // 往上一层比一层窄
                }
                counts = if (rem > 0) tmp + listOf(rem) else tmp
                if (rem == 0 || bs <= (if (byYear) 8.dp else 14.dp)) break
                bs -= 2.dp                          // 装不下就整罐换小一号
            }
            beanSize = bs
        } else {
            // 整年一罐：底宽约 √(2n) 的三角堆，糖粒变小，正好堆满大半个罐子
            var baseW = ((kotlin.math.sqrt(8.0 * n + 1) - 1) / 2).toInt().coerceAtLeast(2)
            if (baseW * (baseW + 1) / 2 < n) baseW += 1
            beanSize = (usableW / (baseW * 0.95f)).coerceIn(6.dp, YearBean)
            unit = beanSize * 0.95f
            val tmp = mutableListOf<Int>()
            var rem = n
            var w = baseW
            while (rem > 0) {
                val take = minOf(rem, w)
                tmp.add(take)
                rem -= take
                w = maxOf(1, w - 1)
            }
            counts = tmp
        }
        val bodyTop = JarLidH + JarNeckH              // 罐肚顶上，瓶口就在这上面
        val mouthY = JarLidH + 1.dp                   // 释放点：瓶口
        val floorY = h - 10.dp - beanSize
        // 预先算好每颗的落点：一层一层居中收窄，就是山的形状
        val spots = mutableListOf<Pair<Dp, Dp>>()
        for ((layer, m) in counts.withIndex()) {
            val layerW = unit * m
            val rowStart = sideMargin + (usableW - layerW) / 2
            repeat(m) { i ->
                spots.add(Pair(
                    rowStart + unit * i + unit / 2 - beanSize / 2,
                    floorY - unit * (0.85f * layer)
                ))
            }
        }
        // 糖越多，出瓶口的间隔越短：一股细细的糖流
        val step = (1600f / n.coerceAtLeast(1)).coerceIn(5f, 24f)
        // 刚切完月/年（罐子在缩放）时先憋一下，等罐子站定再倒
        val baseDelay = if (jarResized) 460L else 0L
        beans.forEachIndexed { index, bean ->
            val (tx, ty) = spots[index]
            val h = bean.dateKey.hashCode()
            // 每颗大小、歪的角度都不同，挤在一起才像真的
            val thisSize = beanSize * (1f - (((h shr 14) and 0x0F) / 15f) * 0.12f)
            val rot = (((h shr 18) and 0x3F) / 63f) * 26f - 13f
            // 少许抖动，自然但不明显叠在一起
            val jx = 3.dp * (((h and 0x7F) / 127f) * 2f - 1f)
            val jy = 2.dp * ((((h shr 7) and 0x7F) / 127f) * 2f - 1f)
            GrainInJar(
                bean = bean,
                size = thisSize,
                mouthX = jarW / 2 - thisSize / 2 +
                    7.dp * (((h shr 10) and 0x0F) / 15f - 0.5f) * 2f,   // 出瓶口时左右偏一点
                mouthY = mouthY,
                targetX = tx + jx,
                targetY = bodyTop + ty + jy,
                rotation = rot,
                tumble = (((h shr 22) and 0x1F) / 31f - 0.5f) * 46f,    // 下落时翻滚的幅度
                delayMs = baseDelay + (index * step).toLong(),   // 一颗接一颗从瓶口涌出
                phase = (index % 7) / 7f,
                waveT = t,
                reloadKey = reloadKey,
                selected = selected?.dateKey == bean.dateKey,
                onPick = { onPick(bean) }
            )
        }
    }
}

@Composable
private fun GrainInJar(
    bean: JarBean,
    size: Dp,
    mouthX: Dp,
    mouthY: Dp,
    targetX: Dp,
    targetY: Dp,
    rotation: Float,
    tumble: Float,
    delayMs: Long,
    phase: Float,
    waveT: Float,
    reloadKey: String,
    selected: Boolean,
    onPick: () -> Unit
) {
    val color = Color(bean.mood.colorHex)
    // 在瓶口冒出来，加速掉进罐里，落地弹两下才躺稳
    // reloadKey（当前月/年）变了就整罐重新倒一遍
    var released by remember(reloadKey) { mutableStateOf(false) }
    LaunchedEffect(bean.dateKey, reloadKey) {
        delay(delayMs)
        released = true
    }
    // 整体进度：匀速时间轴，位置上再分别做横向/纵向的缓动
    // released 归零时用 snap 直接跳回起点，避免出现"倒放"回瓶口
    val p by animateFloatAsState(
        targetValue = if (released) 1f else 0f,
        animationSpec = if (released) tween(durationMillis = 680, easing = LinearEasing) else snap(),
        label = "p"
    )
    // 落稳之后跟着整罐轻轻晃（每颗相位不同）
    val bob = kotlin.math.sin((waveT + phase) * 2f * kotlin.math.PI.toFloat()) * 1.2f

    Box(
        modifier = Modifier
            .offset {
                val sX = mouthX.roundToPx()
                val sY = mouthY.roundToPx()
                val tX = targetX.roundToPx()
                val tY = targetY.roundToPx()
                val fall = 0.78f
                val pF = (p / fall).coerceIn(0f, 1f)                  // 下落段进度
                val pb = ((p - fall) / (1f - fall)).coerceIn(0f, 1f)  // 落地弹跳段进度
                val pyE = pF * pF                                     // 纵向重力加速
                // 下落时轻轻飘摆，越接近落点越稳
                val sway = kotlin.math.sin(
                    pF * 3f * kotlin.math.PI.toFloat() + phase * 6.283f
                ) * (1f - pF) * 2.2f
                // 落地后的小弹跳，两三下、越来越轻
                val bounce = if (pb > 0f) {
                    -kotlin.math.abs(
                        kotlin.math.sin(pb * 2.5f * kotlin.math.PI.toFloat())
                    ) * (1f - pb) * 5f
                } else 0f
                IntOffset(
                    (sX + (tX - sX) * pF + sway.dp.roundToPx()).roundToInt(),
                    (sY + (tY - sY) * pyE + bounce.dp.roundToPx() + (bob * 2f).dp.roundToPx()).roundToInt()
                )
            }
            .size(size)
            .graphicsLayer {
                alpha = (p * 5f).coerceAtMost(1f)      // 在瓶口冒出来时快速浮现
                val pF = (p / 0.78f).coerceIn(0f, 1f)
                rotationZ = rotation + (1f - pF) * tumble   // 下落时翻滚，落稳摆正
            }
            .clip(RoundedCornerShape(35))
            .background(
                if (selected) color.copy(alpha = 0.55f)
                else color.copy(alpha = if (size < 15.dp) 0.45f else 0.28f)
            )
            .then(
                if (selected) Modifier.border(2.dp, color, RoundedCornerShape(35)) else Modifier
            )
            .clickable { onPick() },
        contentAlignment = Alignment.Center
    ) {
        if (size >= 15.dp) {
            val res = stickerOf(bean.mood)
            if (res != null) {
                Image(painterResource(res), bean.mood.label, Modifier.size(size - 5.dp))
            } else {
                Text(bean.mood.emoji, fontSize = (size.value * 0.6f).sp)
            }
        }
        // 太小的糖粒不贴表情，就是一颗纯色小糖果
    }
}
