package com.example.zhixin.ui.screens

import com.example.zhixin.BuildConfig
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.CUSTOM_TAG
import com.example.zhixin.data.Category
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.ImageStore
import com.example.zhixin.data.Store
import com.example.zhixin.ui.components.LevelNowBlock
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SectionTitle
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.StatCard
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors

/** 内置的表情头像，用户信息页里也能选。 */
val AVATARS = listOf(
    "\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF3B", "\uD83C\uDF41",
    "\uD83D\uDC39", "\uD83D\uDC31", "\uD83D\uDC36", "\uD83E\uDD8A",
    "☁", "⭐", "\uD83C\uDF08", "\uD83C\uDF19"
)

data class Badge(val emoji: String, val name: String, val need: String, val got: Boolean)

@Composable
fun MineScreen(
    settings: AppSettings,
    entries: List<Entry>,
    onChange: (AppSettings) -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSettingsHome: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    /** 点头像 / 昵称 → 打开用户信息页。 */
    onOpenProfile: () -> Unit,
    // 提到外面保存：进出设置页回来还停在原来的位置
    listState: LazyListState = rememberLazyListState()
) {
    val c = LocalAppColors.current
    val logged = settings.accountUser.isNotBlank()

    val streak = remember(entries) { Store.streak(entries) }
    val dayCount = remember(entries) { entries.map { it.dateKey }.distinct().size }
    val totalHeat = remember(entries) { Heat.total(entries) }
    val newCount = remember(entries) { entries.count { it.category == Category.NEW.key } }
    val joinDays = remember(entries) {
        val first = entries.minByOrNull { it.time }?.time
        if (first == null) 0 else DateKey.daysBetween(first, System.currentTimeMillis()) + 1
    }
    val badges = remember(entries) { buildBadges(mineStats(entries)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = listState,
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("\uD83D\uDC64 我的", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Text("你的知新小档案", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
            }
        }

        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MineAvatar(
                        settings = settings,
                        size = 62.dp,
                        onClick = onOpenProfile
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                settings.nickname.ifBlank { "旅人" },
                                style = MaterialTheme.typography.titleMedium,
                                color = c.ink,
                                modifier = Modifier.clickable(onClick = onOpenProfile)
                            )
                            Spacer(Modifier.width(6.dp))
                            TinyButton("改资料", onClick = onOpenProfile)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${Heat.levelEmoji(totalHeat)} ${Heat.levelName(totalHeat)} · 加入第 ${joinDays} 天",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkSoft
                        )
                        Text(
                            settings.motto.ifBlank { "每天都有新的收获" },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.accent
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.cardAlt)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (logged) "\uD83D\uDC64 已登录：${settings.accountUser}"
                        else "\uD83E\uDDF3 游客模式 · 记录保存在本机",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (logged) {
                        TinyButton("退出登录", onClick = onLogout)
                    } else {
                        TinyButton("\uD83D\uDD11 登录", onClick = onLogin)
                        Spacer(Modifier.width(6.dp))
                        TinyButton("✨ 注册", onClick = onLogin)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LevelNowBlock(totalHeat = totalHeat)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("🔥", "${streak}天", "连续打卡", Modifier.weight(1f))
                StatCard("📅", "${dayCount}天", "有记录的日子", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("\uD83D\uDD25", String.format("%,d", totalHeat), "累计热度", Modifier.weight(1f))
                StatCard("\uD83C\uDF31", "${newCount}条", "新收获", Modifier.weight(1f))
            }
        }

        item {
            EntryRow(
                emoji = "🏆",
                title = "我的成就",
                subtitle = "已点亮 ${badges.count { it.got }}/${badges.size}",
                onClick = onOpenAchievements
            )
        }

        item {
            EntryRow(
                emoji = "🏠",
                title = "小屋设置",
                subtitle = "皮肤、提醒、标签、备份与关于",
                onClick = onOpenSettingsHome
            )
        }

        item {
            Text(
                "知新 v${BuildConfig.VERSION_NAME} · 温故而知新\n所有记录只保存在本机，不上传云端",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }

}


/**
 * 头像：有上传的图片就显示图片，没有就显示表情头像。
 * 「我的」页和用户信息页共用。
 */
@Composable
fun MineAvatar(
    settings: AppSettings,
    size: Dp,
    onClick: (() -> Unit)? = null
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val file = settings.avatarFile
    val bitmap = remember(file) {
        if (file.isBlank()) null else ImageStore.loadBitmap(context, file, 512)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * 0.32f).dp))
            .background(c.cardAlt)
            .border(2.dp, c.accentSoft, RoundedCornerShape((size.value * 0.32f).dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(settings.avatar, fontSize = (size.value * 0.48f).sp)
        }
    }
}

@Composable
private fun SettingsRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
        }
        Text("›", style = MaterialTheme.typography.titleMedium, color = c.inkSoft)
    }
    Spacer(Modifier.height(2.dp))
}

/** 「我的」里的一张小卡片入口，点一下进下一页。 */
@Composable
private fun EntryRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = c.ink)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
            }
            Text("›", style = MaterialTheme.typography.titleMedium, color = c.inkSoft)
        }
    }
}

/** 「我的」→「我的成就」点开后的整页成就墙。 */
@Composable
fun AchievementScreen(
    entries: List<Entry>,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val groups = remember(entries) { buildBadgeGroups(mineStats(entries)) }
    val badges = remember(groups) { groups.flatMap { it.badges } }
    val got = badges.count { it.got }

    // 顶栏固定，只有下面的内容滚动
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "🏆 我的成就",
            subtitle = "已点亮 $got/${badges.size} · 共 ${groups.size} 类 · 每点亮一个都是你认真生活的痕迹",
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        groups.forEach { group ->
            item {
                SoftCard {
                    BadgeSection(group = group)
                }
            }
        }
        item {
            Text(
                if (got == badges.size) "全部点亮啦，你真的很棒 🌟"
                else "还差 ${badges.size - got} 个就集齐了，慢慢来",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        }
    }
}

/** 「我的」→「小屋设置」点开后的设置分类页。 */
@Composable
fun SettingsHomeScreen(
    settings: AppSettings,
    entries: List<Entry>,
    onOpen: (SettingsFocus) -> Unit,
    onOpenTrash: () -> Unit,
    onShowGuide: () -> Unit,
    onBack: () -> Unit
) {
    val dayCount = remember(entries) { entries.map { it.dateKey }.distinct().size }

    // 顶栏固定，只有下面的内容滚动
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "🏠 小屋设置",
            subtitle = "打造属于你的情绪小屋",
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        item {
            SoftCard {
                SettingsRow("\uD83C\uDFA8", "外观与皮肤", "主题、深色、字号、装饰") {
                    onOpen(SettingsFocus.APPEARANCE)
                }
                SettingsRow("🛠", "使用设置", buildString {
                    append("提醒")
                    append(if (settings.remindEnabled) {
                        String.format(" %02d:%02d", settings.remindHour, settings.remindMinute)
                    } else {
                        " 未开"
                    })
                    append(" · 标签 · ")
                    append(if (settings.lockEnabled()) "已上锁" else "密码锁")
                    append(" · ")
                    append(if (settings.aiEnabled()) settings.aiLabel else "AI 未配置")
                }) {
                    onOpen(SettingsFocus.USAGE)
                }
                SettingsRow("\uD83D\uDDD1", "回收站", "找回误删的记录", onOpenTrash)
                SettingsRow("💾", "数据备份", "故事 $dayCount 天 · 导出 / 导入备份") {
                    onOpen(SettingsFocus.BACKUP)
                }
                SettingsRow("📖", "使用指南", "再看一次新手引导", onShowGuide)
                SettingsRow("💫", "关于知新", "版本 v${BuildConfig.VERSION_NAME} · 情绪小屋") {
                    onOpen(SettingsFocus.ABOUT)
                }
            }
        }
        }
    }
}

/** 一类成就：小标题 + 已点亮 / 总数，下面还是四列网格。 */
@Composable
private fun BadgeSection(group: BadgeGroup) {
    val c = LocalAppColors.current
    val got = group.badges.count { it.got }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(group.emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                group.title,
                style = MaterialTheme.typography.titleSmall,
                color = c.ink,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (got == group.badges.size) c.accentSoft else c.cardAlt)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "$got/${group.badges.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (got == group.badges.size) c.accent else c.inkSoft
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        BadgeGrid(group.badges)
    }
}

@Composable
private fun BadgeGrid(badges: List<Badge>) {
    val c = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        badges.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { badge ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (badge.got) c.accent.copy(alpha = 0.22f) else c.cardAlt
                                )
                                .border(
                                    1.dp,
                                    if (badge.got) c.accent else c.line,
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (badge.got) badge.emoji else "🔒",
                                fontSize = if (badge.got) 22.sp else 16.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            badge.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (badge.got) c.ink else c.inkSoft,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            if (badge.got) "已达成" else badge.need,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** 算成就要用到的一些统计，一次算完给所有徽章用。 */
data class MineStats(
    val total: Int,
    val streak: Int,
    val heat: Int,
    val days: Int,
    val newCount: Int,
    val diary: Int,
    val mood: Int,
    val custom: Int,
    val images: Int,
    val words: Int,
    val night: Int,
    val morning: Int,
    val bestDay: Int,
    val moodKinds: Int,
    val story: Int,
    val good: Int,
    val daily: Int,
    val tagged: Int,
    val weekend: Int,
    val months: Int,
    val bestMonthDays: Int,
    val dayPartsMax: Int,
    val bestDayHeat: Int,
    val longText: Int,
    val imageDiary: Int,
    val afternoon: Int,
    val maxTextLen: Int,
    val maxImages: Int,
    val tagKinds: Int,
    val firstDayAgo: Int,
    val diaryDays: Int,
    val weeks: Int,
    val categoryKinds: Int,
    val customKinds: Int,
    val partsCovered: Int
)

fun mineStats(entries: List<Entry>): MineStats {
    val live = entries.filter { !it.deleted }
    val cal = java.util.Calendar.getInstance()
    var night = 0
    var morning = 0
    var afternoon = 0
    var weekend = 0
    // 单日覆盖的时段（凌晨 / 上午 / 下午 / 晚上）最多有几个
    val partsByDay = HashMap<String, MutableSet<Int>>()
    live.forEach {
        cal.timeInMillis = it.time
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        if (h >= 22 || h < 4) night++
        if (h in 5..7) morning++
        if (h in 12..17) afternoon++
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        if (dow == java.util.Calendar.SATURDAY || dow == java.util.Calendar.SUNDAY) weekend++
        val part = when (h) {
            in 0..5 -> 0
            in 6..11 -> 1
            in 12..17 -> 2
            else -> 3
        }
        partsByDay.getOrPut(it.dateKey) { HashSet() }.add(part)
    }
    val monthDays = live.map { it.dateKey.take(7) }.groupingBy { it }.eachCount()
    return MineStats(
        total = live.size,
        streak = Store.streak(entries),
        heat = Heat.total(entries),
        days = live.map { it.dateKey }.distinct().size,
        newCount = live.count { it.category == Category.NEW.key },
        diary = live.count { it.category == Category.DIARY.key },
        mood = live.count { it.category == Category.MOOD.key },
        custom = live.count { it.tags.contains(CUSTOM_TAG) },
        images = live.sumOf { it.imageList.size },
        words = live.sumOf { (it.summary + it.text).length },
        night = night,
        morning = morning,
        bestDay = live.groupBy { it.dateKey }.values.maxOfOrNull { it.size } ?: 0,
        moodKinds = live.mapNotNull { it.mood }.distinct().size,
        story = live.count { it.category == Category.STORY.key },
        good = live.count { it.category == Category.GOOD.key },
        daily = live.count { it.category == Category.DAILY.key },
        tagged = live.count { it.tags.isNotEmpty() },
        weekend = weekend,
        months = monthDays.size,
        bestMonthDays = monthDays.values.maxOrNull() ?: 0,
        dayPartsMax = partsByDay.values.maxOfOrNull { it.size } ?: 0,
        bestDayHeat = Heat.heatByDay(entries).values.maxOrNull() ?: 0,
        longText = live.count { it.text.length >= 500 },
        imageDiary = live.count {
            it.category == Category.DIARY.key && it.imageList.isNotEmpty()
        },
        afternoon = afternoon,
        maxTextLen = live.maxOfOrNull { (it.summary + it.text).length } ?: 0,
        maxImages = live.maxOfOrNull { it.imageList.size } ?: 0,
        tagKinds = live.flatMap { it.tags }.filterNot { it == CUSTOM_TAG }.distinct().size,
        firstDayAgo = live.minByOrNull { it.time }?.let {
            ((System.currentTimeMillis() - it.time) / 86_400_000L).toInt()
        } ?: 0,
        diaryDays = live.filter { it.category == Category.DIARY.key }
            .map { it.dateKey }.distinct().size,
        weeks = live.map {
            cal.timeInMillis = it.time
            "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.WEEK_OF_YEAR)}"
        }.distinct().size,
        categoryKinds = live.map { it.category }.distinct().size,
        customKinds = live.filter { it.tags.contains(CUSTOM_TAG) }
            .flatMap { it.tags.filterNot { t -> t == CUSTOM_TAG } }
            .distinct().size,
        partsCovered = partsByDay.values.flatten().distinct().size
    )
}

/** 一组同类成就，成就墙按组分块展示。 */
data class BadgeGroup(
    val emoji: String,
    val title: String,
    val badges: List<Badge>
)

/** 全部成就（不分组），用于总数统计。 */
fun buildBadges(s: MineStats): List<Badge> =
    buildBadgeGroups(s).flatMap { it.badges }

fun buildBadgeGroups(s: MineStats): List<BadgeGroup> = listOf(
    BadgeGroup("\uD83C\uDF31", "起步与积累", listOf(
        Badge("\uD83C\uDF31", "初次打卡", "记录第 1 条", s.total >= 1),
        Badge("\uD83C\uDF3F", "再来一条", "累计 3 条", s.total >= 3),
        Badge("\uD83C\uDF3B", "小有收获", "累计 5 条", s.total >= 5),
        Badge("\uD83C\uDF3A", "渐入佳境", "累计 10 条", s.total >= 10),
        Badge("\uD83C\uDF37", "习惯发芽", "累计 20 条", s.total >= 20),
        Badge("\uD83C\uDF33", "站稳脚跟", "累计 30 条", s.total >= 30),
        Badge("\uD83C\uDF34", "小树成荫", "累计 50 条", s.total >= 50),
        Badge("\uD83C\uDF32", "枝繁叶茂", "累计 80 条", s.total >= 80),
        Badge("\uD83C\uDF35", "百条达成", "累计 100 条", s.total >= 100),
        Badge("\uD83C\uDF36", "积少成多", "累计 150 条", s.total >= 150),
        Badge("\uD83D\uDCD6", "笔耕不辍", "累计 200 条", s.total >= 200),
        Badge("\uD83C\uDFF6", "故事收藏家", "累计 300 条", s.total >= 300),
        Badge("\uD83D\uDCDB", "记录大师", "累计 500 条", s.total >= 500),
        Badge("\uD83C\uDFC5", "千条传说", "累计 1000 条", s.total >= 1000)
    )),
    BadgeGroup("🔥", "连续的脚印", listOf(
        Badge("\uD83C\uDF3F", "连着两天", "连续 2 天", s.streak >= 2),
        Badge("\uD83D\uDD25", "三日成型", "连续 3 天", s.streak >= 3),
        Badge("\uD83C\uDF1F", "五天不断", "连续 5 天", s.streak >= 5),
        Badge("🏅", "一周不断", "连续 7 天", s.streak >= 7),
        Badge("\uD83C\uDF1F", "十天连击", "连续 10 天", s.streak >= 10),
        Badge("✨", "两周连击", "连续 14 天", s.streak >= 14),
        Badge("\uD83D\uDD25", "三周之约", "连续 21 天", s.streak >= 21),
        Badge("👑", "月度坚持", "连续 30 天", s.streak >= 30),
        Badge("\uD83C\uDF1F", "五十天", "连续 50 天", s.streak >= 50),
        Badge("\uD83C\uDFC6", "六十六天", "连续 66 天", s.streak >= 66),
        Badge("\uD83C\uDFC6", "百日之约", "连续 100 天", s.streak >= 100),
        Badge("\uD83C\uDF1F", "半年不断", "连续 180 天", s.streak >= 180),
        Badge("\uD83C\uDF96", "全年不断", "连续 365 天", s.streak >= 365)
    )),
    BadgeGroup("\uD83C\uDF2B", "小屋热度", listOf(
        Badge("\uD83D\uDCA7", "有点温度", "热度 10", s.heat >= 10),
        Badge("\uD83C\uDF2B", "热起来了", "热度 30", s.heat >= 30),
        Badge("\uD83C\uDF26", "温热小屋", "热度 60", s.heat >= 60),
        Badge("\uD83C\uDF25", "热度破百", "热度 100", s.heat >= 100),
        Badge("\uD83C\uDF2B", "热度火热", "热度 200", s.heat >= 200),
        Badge("\uD83C\uDF27", "热气腾腾", "热度 400", s.heat >= 400),
        Badge("\uD83C\uDF0B", "热力澎湃", "热度 700", s.heat >= 700),
        Badge("\uD83C\uDF0B", "热度爆表", "热度 1000", s.heat >= 1000),
        Badge("\uD83D\uDD25", "热浪滚滚", "热度 1500", s.heat >= 1500),
        Badge("\uD83C\uDF26", "热力四射", "热度 2000", s.heat >= 2000),
        Badge("\uD83C\uDF0B", "热度满仓", "热度 3000", s.heat >= 3000),
        Badge("\uD83C\uDF0C", "热力无限", "热度 5000", s.heat >= 5000),
        Badge("🌋", "热度传说", "热度 8000", s.heat >= 8000)
    )),
    BadgeGroup("\uD83D\uDCC5", "有记录的日子", listOf(
        Badge("\uD83D\uDCC5", "第一周", "有记录 7 天", s.days >= 7),
        Badge("\uD83D\uDCC6", "两周了", "有记录 14 天", s.days >= 14),
        Badge("\uD83D\uDCC7", "三周了", "有记录 21 天", s.days >= 21),
        Badge("\uD83D\uDDD3", "月月有余", "有记录 30 天", s.days >= 30),
        Badge("\uD83D\uDCC5", "五十天", "有记录 50 天", s.days >= 50),
        Badge("\uD83C\uDF1F", "七十五天", "有记录 75 天", s.days >= 75),
        Badge("\uD83D\uDDD3", "半年陪伴", "有记录 100 天", s.days >= 100),
        Badge("\uD83C\uDF1F", "一百五十天", "有记录 150 天", s.days >= 150),
        Badge("\uD83C\uDF1F", "两百天", "有记录 200 天", s.days >= 200),
        Badge("\uD83C\uDF0D", "三百天", "有记录 300 天", s.days >= 300),
        Badge("\uD83C\uDF0D", "一年陪伴", "有记录 365 天", s.days >= 365),
        Badge("\uD83C\uDF0C", "五百天", "有记录 500 天", s.days >= 500),
        Badge("\uD83D\uDC95", "相识一个月", "第一条记录满 30 天", s.firstDayAgo >= 30),
        Badge("\uD83D\uDC95", "相识百日", "第一条记录满 100 天", s.firstDayAgo >= 100),
        Badge("\uD83E\uDD1D", "老朋友", "第一条记录满 365 天", s.firstDayAgo >= 365),
        Badge("\uD83E\uDD1D", "两年老友", "第一条记录满 730 天", s.firstDayAgo >= 730)
    )),
    BadgeGroup("\uD83D\uDDD3", "月份与星期", listOf(
        Badge("\uD83C\uDF42", "第二个月", "记录跨越 2 个月", s.months >= 2),
        Badge("\uD83C\uDF42", "四季更替", "记录跨越 3 个月", s.months >= 3),
        Badge("\uD83C\uDF40", "半载书香", "记录跨越 6 个月", s.months >= 6),
        Badge("\uD83C\uDF40", "九个月", "记录跨越 9 个月", s.months >= 9),
        Badge("\uD83C\uDF0C", "跨年之旅", "记录跨越 12 个月", s.months >= 12),
        Badge("\uD83C\uDF0C", "十八个月", "记录跨越 18 个月", s.months >= 18),
        Badge("\uD83C\uDF0C", "两整年", "记录跨越 24 个月", s.months >= 24),
        Badge("\uD83D\uDCC6", "第一个月", "单月记录 10 天", s.bestMonthDays >= 10),
        Badge("\uD83D\uDCC6", "满满一月", "单月记录 20 天", s.bestMonthDays >= 20),
        Badge("\uD83D\uDDD2", "全勤月份", "单月记录 28 天", s.bestMonthDays >= 28),
        Badge("\uD83D\uDCC5", "四周连记", "记录跨越 4 周", s.weeks >= 4),
        Badge("\uD83D\uDCC5", "三个月周记", "记录跨越 12 周", s.weeks >= 12),
        Badge("\uD83D\uDCC5", "半年周记", "记录跨越 26 周", s.weeks >= 26),
        Badge("\uD83D\uDCC5", "全年周记", "记录跨越 52 周", s.weeks >= 52)
    )),
    BadgeGroup("✍️", "写下的字", listOf(
        Badge("✏️", "动笔了", "累计 1000 字", s.words >= 1000),
        Badge("✍️", "越写越多", "累计 3000 字", s.words >= 3000),
        Badge("✍️", "千字长文", "累计 5000 字", s.words >= 5000),
        Badge("\uD83D\uDCDD", "万字达成", "累计 10000 字", s.words >= 10000),
        Badge("\uD83D\uDCDD", "两万字", "累计 20000 字", s.words >= 20000),
        Badge("\uD83D\uDCD7", "著作等身", "累计 30000 字", s.words >= 30000),
        Badge("\uD83D\uDCD7", "五万字", "累计 50000 字", s.words >= 50000),
        Badge("\uD83D\uDCD2", "八万字", "累计 80000 字", s.words >= 80000),
        Badge("\uD83D\uDCD2", "十万字俱乐部", "累计 100000 字", s.words >= 100000),
        Badge("\uD83D\uDCDA", "二十万字", "累计 200000 字", s.words >= 200000),
        Badge("\uD83D\uDCCD", "写下一段", "单篇写到 200 字", s.maxTextLen >= 200),
        Badge("\uD83D\uDCCD", "写满一页", "单篇写到 500 字", s.maxTextLen >= 500),
        Badge("\uD83D\uDCCD", "一气呵成", "单篇写到 1000 字", s.maxTextLen >= 1000),
        Badge("\uD83D\uDCDC", "长篇大论", "单篇写到 2000 字", s.maxTextLen >= 2000),
        Badge("\uD83D\uDCDC", "长文一次", "500 字以上写 1 次", s.longText >= 1),
        Badge("\uD83D\uDCDC", "长文常客", "500 字以上写 5 次", s.longText >= 5),
        Badge("\uD83D\uDCDC", "长文达人", "500 字以上写 20 次", s.longText >= 20)
    )),
    BadgeGroup("\uD83D\uDCD4", "日记本", listOf(
        Badge("\uD83D\uDCD3", "第一篇日记", "写 1 篇日记", s.diary >= 1),
        Badge("\uD83D\uDCD4", "日记起步", "写 3 篇日记", s.diary >= 3),
        Badge("\uD83D\uDCD4", "日记新手", "写 5 篇日记", s.diary >= 5),
        Badge("\uD83D\uDCD5", "日记渐多", "写 10 篇日记", s.diary >= 10),
        Badge("\uD83D\uDCD5", "日记成习惯", "写 20 篇日记", s.diary >= 20),
        Badge("\uD83D\uDCDA", "日记达人", "写 30 篇日记", s.diary >= 30),
        Badge("\uD83D\uDCDA", "日记五十篇", "写 50 篇日记", s.diary >= 50),
        Badge("\uD83D\uDCD5", "日记百篇", "写 100 篇日记", s.diary >= 100),
        Badge("\uD83D\uDCC8", "记满一周", "7 天写过日记", s.diaryDays >= 7),
        Badge("\uD83D\uDCC8", "日记不断", "30 天写过日记", s.diaryDays >= 30),
        Badge("\uD83D\uDCC8", "百日日记", "100 天写过日记", s.diaryDays >= 100)
    )),
    BadgeGroup("\uD83D\uDDBC", "有图有真相", listOf(
        Badge("\uD83D\uDDBC", "第一张图", "配图 1 张", s.images >= 1),
        Badge("\uD83C\uDFA8", "有图有真相", "配图 5 张", s.images >= 5),
        Badge("\uD83C\uDFA8", "图文并茂", "配图 15 张", s.images >= 15),
        Badge("\uD83D\uDCC2", "配图渐多", "配图 30 张", s.images >= 30),
        Badge("\uD83D\uDCC2", "相册有点满", "配图 60 张", s.images >= 60),
        Badge("\uD83D\uDCC2", "相册满满", "配图 100 张", s.images >= 100),
        Badge("\uD83D\uDCC1", "两百张", "配图 200 张", s.images >= 200),
        Badge("\uD83C\uDF04", "三张起", "一条记录配 3 张图", s.maxImages >= 3),
        Badge("\uD83C\uDF04", "五张起", "一条记录配 5 张图", s.maxImages >= 5),
        // 一条记录最多配 6 张图，成就门槛不能超过这个上限，否则永远拿不到
        Badge("\uD83C\uDF04", "六张满配", "一条记录配 6 张图", s.maxImages >= 6),
        Badge("\uD83D\uDCD9", "图文日记", "带图日记 1 篇", s.imageDiary >= 1),
        Badge("\uD83D\uDCD9", "爱配图", "带图日记 5 篇", s.imageDiary >= 5),
        Badge("\uD83C\uDFD7", "图文日记家", "带图日记 20 篇", s.imageDiary >= 20)
    )),
    BadgeGroup("\uD83D\uDE06", "心情打卡", listOf(
        Badge("\uD83D\uDE42", "记下心情", "心情打卡 5 次", s.mood >= 5),
        Badge("\uD83D\uDE42", "心情起步", "心情打卡 10 次", s.mood >= 10),
        Badge("\uD83D\uDE06", "心情观察员", "心情打卡 20 次", s.mood >= 20),
        Badge("\uD83D\uDE06", "心情常记录", "心情打卡 50 次", s.mood >= 50),
        Badge("\uD83D\uDE0C", "心情记录者", "心情打卡 100 次", s.mood >= 100),
        Badge("\uD83D\uDE0C", "心情老朋友", "心情打卡 200 次", s.mood >= 200),
        Badge("🎭", "两种心情", "记录过 2 种心情", s.moodKinds >= 2),
        Badge("🎭", "心情百变", "记录过 4 种心情", s.moodKinds >= 4),
        Badge("\uD83C\uDF08", "喜怒哀乐", "记录过 6 种心情", s.moodKinds >= 6),
        Badge("\uD83C\uDF08", "心情全收集", "记录过 8 种心情", s.moodKinds >= 8)
    )),
    BadgeGroup("🌟", "新收获与抽屉", listOf(
        Badge("🌟", "一个新发现", "新收获 5 条", s.newCount >= 5),
        Badge("🌟", "日知一新", "新收获 10 条", s.newCount >= 10),
        Badge("✨", "收获渐多", "新收获 20 条", s.newCount >= 20),
        Badge("✨", "新意满满", "新收获 50 条", s.newCount >= 50),
        Badge("\uD83C\uDF1F", "新知不断", "新收获 100 条", s.newCount >= 100),
        Badge("\uD83C\uDF1F", "日新又新", "新收获 200 条", s.newCount >= 200),
        Badge("\uD83C\uDF27", "愿意倾诉", "闷闷的小故事 5 条", s.story >= 5),
        Badge("\uD83C\uDF27", "说出心事", "闷闷的小故事 15 条", s.story >= 15),
        Badge("\uD83C\uDF27", "倾诉达人", "闷闷的小故事 30 条", s.story >= 30),
        Badge("🌻", "留意美好", "小美好 5 条", s.good >= 5),
        Badge("🌻", "收藏美好", "小美好 15 条", s.good >= 15),
        Badge("🌻", "美好收藏家", "小美好 30 条", s.good >= 30),
        Badge("☁", "记录日常", "小日常 5 条", s.daily >= 5),
        Badge("☁", "日常不断", "小日常 15 条", s.daily >= 15),
        Badge("☁", "平淡是真", "小日常 30 条", s.daily >= 30)
    )),
    BadgeGroup("🏷", "标签与分类", listOf(
        Badge("\uD83D\uDDC2", "打个标签", "带标签的记录 5 条", s.tagged >= 5),
        Badge("🏷", "井井有条", "带标签的记录 20 条", s.tagged >= 20),
        Badge("\uD83D\uDDC2", "标签成习惯", "带标签的记录 50 条", s.tagged >= 50),
        Badge("\uD83D\uDDC3", "用过 3 种标签", "用过 3 种标签", s.tagKinds >= 3),
        Badge("\uD83D\uDDC3", "用过 6 种标签", "用过 6 种标签", s.tagKinds >= 6),
        Badge("\uD83D\uDDC2", "标签达人", "用过 10 种标签", s.tagKinds >= 10),
        Badge("\uD83D\uDDC2", "标签收藏家", "用过 15 种标签", s.tagKinds >= 15),
        Badge("\uD83D\uDDC3", "三个抽屉", "用过 3 种记录分类", s.categoryKinds >= 3),
        Badge("\uD83D\uDDC3", "四个抽屉", "用过 4 种记录分类", s.categoryKinds >= 4),
        Badge("\uD83D\uDDC3", "五个抽屉", "用过 5 种记录分类", s.categoryKinds >= 5),
        Badge("\uD83D\uDDC3", "全都用上", "用过 6 种记录分类", s.categoryKinds >= 6)
    )),
    BadgeGroup("\uD83C\uDFAF", "任务打卡", listOf(
        Badge("\uD83C\uDFAF", "开始坚持", "任务打卡 10 次", s.custom >= 10),
        Badge("\uD83C\uDFAF", "说到做到", "任务打卡 30 次", s.custom >= 30),
        Badge("\uD83E\uDDED", "稳步前进", "任务打卡 50 次", s.custom >= 50),
        Badge("\uD83E\uDDED", "习惯成自然", "任务打卡 100 次", s.custom >= 100),
        Badge("\uD83D\uDE80", "两百次", "任务打卡 200 次", s.custom >= 200),
        Badge("\uD83D\uDE80", "打卡狂人", "任务打卡 300 次", s.custom >= 300),
        Badge("\uD83C\uDFC6", "五百次", "任务打卡 500 次", s.custom >= 500),
        Badge("\uD83C\uDFC5", "打卡里程碑", "任务打卡 800 次", s.custom >= 800),
        Badge("\uD83C\uDFAF", "多线并进", "坚持过 2 个不同任务", s.customKinds >= 2),
        Badge("\uD83C\uDFAF", "全面开花", "坚持过 4 个不同任务", s.customKinds >= 4),
        Badge("\uD83C\uDFAF", "任务收藏家", "坚持过 6 个不同任务", s.customKinds >= 6)
    )),
    BadgeGroup("🌙", "作息与周末", listOf(
        Badge("🌙", "偶尔晚睡", "22 点后记 5 次", s.night >= 5),
        Badge("🌙", "夜猫子", "22 点后记 15 次", s.night >= 15),
        Badge("\uD83C\uDF03", "深夜常客", "22 点后记 30 次", s.night >= 30),
        Badge("\uD83C\uDF03", "越夜越精神", "22 点后记 60 次", s.night >= 60),
        Badge("\uD83C\uDF03", "守夜人", "22 点后记 120 次", s.night >= 120),
        Badge("\uD83C\uDF05", "早起鸟", "清晨记 3 次", s.morning >= 3),
        Badge("\uD83C\uDF05", "早起习惯", "清晨记 10 次", s.morning >= 10),
        Badge("\uD83C\uDF1E", "晨型人", "清晨记 25 次", s.morning >= 25),
        Badge("\uD83C\uDF1E", "日出而作", "清晨记 50 次", s.morning >= 50),
        Badge("\uD83C\uDF24", "午后时光", "下午记 10 次", s.afternoon >= 10),
        Badge("\uD83C\uDF24", "下午也记", "下午记 30 次", s.afternoon >= 30),
        Badge("\uD83C\uDF24", "午后常客", "下午记 60 次", s.afternoon >= 60),
        Badge("🌈", "周末也记", "周末记录 5 次", s.weekend >= 5),
        Badge("🌈", "周末不偷懒", "周末记录 20 次", s.weekend >= 20),
        Badge("\uD83C\uDFD6", "周末也认真", "周末记录 60 次", s.weekend >= 60),
        Badge("\uD83C\uDFD6", "周末常客", "周末记录 150 次", s.weekend >= 150)
    )),
    BadgeGroup("\uD83D\uDD25", "特别的一天", listOf(
        Badge("\uD83D\uDCAC", "一天三记", "单日 3 条记录", s.bestDay >= 3),
        Badge("\uD83D\uDCAC", "话痨的一天", "单日 5 条记录", s.bestDay >= 5),
        Badge("\uD83C\uDF89", "一天八记", "单日 8 条记录", s.bestDay >= 8),
        Badge("\uD83C\uDF89", "高产的一天", "单日 10 条记录", s.bestDay >= 10),
        Badge("\uD83D\uDD70", "早晚都记", "单日覆盖 2 个时段", s.dayPartsMax >= 2),
        Badge("\uD83D\uDD70", "充实的一天", "单日覆盖 3 个时段", s.dayPartsMax >= 3),
        Badge("\uD83D\uDD5B", "全天候", "单日覆盖 4 个时段", s.dayPartsMax >= 4),
        Badge("\uD83D\uDD5B", "走过三个时段", "累计记过 3 个时段", s.partsCovered >= 3),
        Badge("\uD83D\uDD5B", "全天候覆盖", "累计记过 4 个时段", s.partsCovered >= 4),
        Badge("\uD83D\uDD25", "火力初开", "单日热度 5", s.bestDayHeat >= 5),
        Badge("\uD83D\uDD25", "火力全开", "单日热度 10", s.bestDayHeat >= 10),
        Badge("☄", "热情高涨", "单日热度 20", s.bestDayHeat >= 20),
        Badge("☄", "热力爆发", "单日热度 30", s.bestDayHeat >= 30)
    ))
)
