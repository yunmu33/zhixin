package com.example.zhixin.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.example.zhixin.ai.AiClient
import com.example.zhixin.BuildConfig
import com.example.zhixin.data.AI_PRESETS
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.RemindRepeat
import com.example.zhixin.data.WEEKDAY_KEYS
import com.example.zhixin.data.WEEKDAY_LABELS
import com.example.zhixin.data.weekdayIndex
import com.example.zhixin.data.DEFAULT_CUSTOM_ITEMS
import com.example.zhixin.data.DEFAULT_TAGS
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Store
import com.example.zhixin.notify.ReminderReceiver
import com.example.zhixin.notify.applyReminder
import com.example.zhixin.notify.hasNotifyPermission
import com.example.zhixin.notify.isBatteryOptimized
import com.example.zhixin.notify.nextReminderText
import com.example.zhixin.notify.notificationsBlocked
import com.example.zhixin.notify.openBatterySettings
import com.example.zhixin.notify.openNotificationSettings
import com.example.zhixin.notify.openExactAlarmSettings
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.ConfirmDialog
import com.example.zhixin.ui.components.EmptyHint
import com.example.zhixin.ui.components.InputDialog
import com.example.zhixin.ui.components.ScreenTopBar
import com.example.zhixin.ui.components.SectionTitle
import com.example.zhixin.ui.components.SoftCard
import com.example.zhixin.ui.components.TagChip
import com.example.zhixin.ui.components.TinyButton
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.dialogEnter
import com.example.zhixin.ui.theme.SKINS
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 「我的」里点哪个分类，就只打开对应的那几项设置。 */
enum class SettingsFocus {
    ALL, APPEARANCE, USAGE, REMINDER, PRIVACY, TAGS, BACKUP, ABOUT, AI
}

/** 「使用设置」是提醒 + 标签 + 隐私 + AI 合在一起的一页。 */
private val USAGE_KEYS = setOf(
    SettingsFocus.REMINDER,
    SettingsFocus.TAGS,
    SettingsFocus.PRIVACY,
    SettingsFocus.AI
)

private fun SettingsFocus.show(key: SettingsFocus): Boolean =
    when (this) {
        SettingsFocus.ALL -> true
        SettingsFocus.USAGE -> key in USAGE_KEYS
        else -> this == key
    }

/** 分区小标题：全部设置，或把好几类放一页的「使用设置」才需要。 */
private fun SettingsFocus.showSectionTitle(): Boolean =
    this == SettingsFocus.ALL || this == SettingsFocus.USAGE

@Composable
fun SettingsScreen(
    settings: AppSettings,
    entries: List<Entry>,
    onChange: (AppSettings) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onShowGuide: () -> Unit,
    onOpenTrash: () -> Unit,
    focus: SettingsFocus = SettingsFocus.ALL,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current

    var showTags by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showClearAll by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var aiBase by remember(settings) { mutableStateOf(settings.aiBaseUrl) }
    var aiKey by remember(settings) { mutableStateOf(settings.aiKey) }
    var aiModel by remember(settings) { mutableStateOf(settings.aiModel) }
    var aiResult by remember { mutableStateOf("") }

    // 从系统设置页（省电 / 通知）回来时重新看一眼，该消失的提示就消失
    val view = LocalView.current
    var resumed by remember { mutableIntStateOf(0) }
    DisposableEffect(view) {
        val owner = view.findViewTreeLifecycleOwner()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed++
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    // 提醒有没有被系统拦住、有没有排上
    var batteryBlocked by remember(resumed) { mutableStateOf(isBatteryOptimized(context)) }
    var notifyBlocked by remember(resumed) { mutableStateOf(notificationsBlocked(context)) }
    var scheduled by remember(resumed) { mutableStateOf(ReminderReceiver.isScheduled(context)) }

    // 授权成功后再排一次，确保开关打开当天就生效
    var needExactAlarm by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) applyReminder(context, settings.copy(remindEnabled = true)) {
            needExactAlarm = true
        }
    }

    val dayCount = remember(entries) { entries.map { it.dateKey }.distinct().size }

    val (title, subtitle) = when (focus) {
        SettingsFocus.APPEARANCE -> "🎨 外观与皮肤" to "主题、深色、字号、装饰"
        SettingsFocus.USAGE -> "🛠 使用设置" to "提醒、标签、密码锁和 AI 都在这里"
        SettingsFocus.REMINDER -> "🔔 每日提醒" to "选一个你想被轻轻叫醒的时刻"
        SettingsFocus.PRIVACY -> "🔒 隐私与密码锁" to "打开小屋需要输入密码"
        SettingsFocus.TAGS -> "🏷 活动标签" to "打卡与记录时可勾选的活动"
        SettingsFocus.BACKUP -> "💾 数据备份" to "导出备份、导入备份或清空"
        SettingsFocus.ABOUT -> "💫 关于知新" to "版本、隐私和数据说明"
        SettingsFocus.AI -> "🤖 AI 整理" to "填 Key 后由 AI 帮你整理记录"
        SettingsFocus.ALL -> "⚙️ 小屋设置" to "皮肤、提醒、密码锁、备份都在这里"
    }

    // 顶栏固定在上面，只有内容滚动
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        ScreenTopBar(title = title, subtitle = subtitle, onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // ---------- 外观与展示 ----------
        if (focus == SettingsFocus.ALL) {
            item {
                Text(
                    "🎨 外观与展示",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent
                )
            }
        }
        if (focus.show(SettingsFocus.APPEARANCE)) {
        item {
            SettingsSection("🎨", "小屋皮肤", "切换后界面配色 + 文字氛围一起变") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SKINS.forEach { skin ->
                        ChoiceChip(
                            text = "${skin.emoji} ${skin.name}",
                            selected = settings.skin == skin.key,
                            modifier = Modifier.weight(1f)
                        ) { onChange(settings.copy(skin = skin.key)) }
                    }
                }
            }
        }

        item {
            SettingsSection("🌓", "深色模式", "跟随系统 / 始终浅色 / 始终深色") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("自动", settings.darkMode == "auto", Modifier.weight(1f)) {
                        onChange(settings.copy(darkMode = "auto"))
                    }
                    ChoiceChip("浅色", settings.darkMode == "light", Modifier.weight(1f)) {
                        onChange(settings.copy(darkMode = "light"))
                    }
                    ChoiceChip("深色", settings.darkMode == "dark", Modifier.weight(1f)) {
                        onChange(settings.copy(darkMode = "dark"))
                    }
                }
            }
        }

        item {
            SettingsSection("🔤", "字体大小", "挑一个看着最舒服的字号") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("小", settings.fontScale < 0.95f, Modifier.weight(1f)) {
                        onChange(settings.copy(fontScale = 0.9f))
                    }
                    ChoiceChip("标准", settings.fontScale in 0.95f..1.05f, Modifier.weight(1f)) {
                        onChange(settings.copy(fontScale = 1.0f))
                    }
                    ChoiceChip("大", settings.fontScale > 1.05f, Modifier.weight(1f)) {
                        onChange(settings.copy(fontScale = 1.15f))
                    }
                }
            }
        }

        item {
            SettingsSection("☁️", "小屋装饰元素", "飘动的云朵 / 月亮星星") {
                SwitchRow(settings.decor) { onChange(settings.copy(decor = it)) }
            }
        }
        }

        // ---------- 陪伴与提醒 ----------
        if (focus.showSectionTitle()) {
            item {
                Text(
                    "🤍 陪伴与提醒",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent
                )
            }
        }
        if (focus.show(SettingsFocus.REMINDER)) {
        item {
            SettingsSection("🎧", "只倾听模式", "开启后整理记录只温柔接住，不做分析不开导") {
                SwitchRow(settings.listenOnly) { onChange(settings.copy(listenOnly = it)) }
            }
        }

        item {
            SettingsSection("⏰", "每日记录提醒", "到点提醒自己来小屋坐坐") {
                SwitchRow(settings.remindEnabled) { enable ->
                    if (enable) {
                        val next = settings.copy(remindEnabled = true)
                        onChange(next)
                        applyReminder(context, next, onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }, onNeedExactAlarm = { needExactAlarm = true })
                    } else {
                        ReminderReceiver.cancel(context)
                        onChange(settings.copy(remindEnabled = false))
                    }
                }
                if (settings.remindEnabled) {
                    fun update(next: AppSettings) {
                        onChange(next)
                        applyReminder(context, next) {}
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🕰", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            String.format(
                                "%02d:%02d",
                                settings.remindHour,
                                settings.remindMinute
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            color = c.accent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "上下滑着选",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.cardAlt)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeWheel(
                            values = (0..23).map { String.format("%02d", it) },
                            selected = settings.remindHour,
                            loop = true,
                            modifier = Modifier.weight(1f)
                        ) { update(settings.copy(remindHour = it)) }
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(":", style = MaterialTheme.typography.titleMedium, color = c.inkSoft)
                        }
                        TimeWheel(
                            values = (0..59).map { String.format("%02d", it) },
                            selected = settings.remindMinute,
                            loop = true,
                            modifier = Modifier.weight(1f)
                        ) { update(settings.copy(remindMinute = it)) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔁", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "循环",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.ink
                            )
                            Text(
                                RemindRepeat.of(settings.remindRepeat).hint,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft
                            )
                        }
                        Text(
                            "上下滑着选",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.cardAlt)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeWheel(
                            values = RemindRepeat.ALL.map { "${it.emoji} ${it.label}" },
                            selected = RemindRepeat.ALL
                                .indexOf(RemindRepeat.of(settings.remindRepeat))
                                .coerceAtLeast(0),
                            modifier = Modifier.weight(1f)
                        ) { i -> update(settings.copy(remindRepeat = RemindRepeat.ALL[i].key)) }
                        // 只在「每周」时才需要挑星期几，别的循环方式就不占位置
                        if (RemindRepeat.of(settings.remindRepeat) == RemindRepeat.WEEKLY) {
                            TimeWheel(
                                values = WEEKDAY_LABELS,
                                selected = weekdayIndex(settings.remindWeekday),
                                modifier = Modifier.weight(1f)
                            ) { i -> update(settings.copy(remindWeekday = WEEKDAY_KEYS[i])) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("常用时段", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "🌅 07:00" to (7 to 0),
                            "🌞 12:00" to (12 to 0),
                            "🌇 18:00" to (18 to 0),
                            "🌙 21:00" to (21 to 0),
                            "🌛 22:30" to (22 to 30)
                        ).chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (label, time) ->
                                    ChoiceChip(
                                        text = label,
                                        selected = settings.remindHour == time.first &&
                                            settings.remindMinute == time.second,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        update(
                                            settings.copy(
                                                remindHour = time.first,
                                                remindMinute = time.second
                                            )
                                        )
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    // 没给精确闹钟权限时提示一下，点了去系统设置页
                    if (needExactAlarm || !ReminderReceiver.hasExactPermission(context)) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.cardAlt)
                                .clickable { openExactAlarmSettings(context) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\uD83D\uDCE1", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "提醒可能有几分钟偏差",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.ink
                                )
                                Text(
                                    "点这里去开启「闹钟和提醒」权限，就能准时提醒",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.inkSoft
                                )
                            }
                        }
                    }
                    // 提醒到底排没排上，直接写清楚，方便排查
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.cardAlt)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\uD83D\uDD50", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "下次提醒：${nextReminderText(
                                    settings.remindHour,
                                    settings.remindMinute,
                                    settings.remindRepeat,
                                    settings.remindWeekday
                                )}",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.ink
                            )
                            Text(
                                if (scheduled) {
                                    "已经排上，到点就会来"
                                } else {
                                    "还没排上，重新打开一下 App 就会自动排"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft
                            )
                        }
                    }

                    // 想马上验证收不收得到：点一下立刻弹一条
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.accentSoft)
                            .clickable {
                                if (hasNotifyPermission(context)) {
                                    ReminderReceiver.notifyNow(context)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\uD83D\uDD14", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "立刻试一条提醒",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.ink
                            )
                            Text(
                                if (hasNotifyPermission(context)) {
                                    "点一下马上弹一条通知，看看手机收不收得到"
                                } else {
                                    "还没给通知权限，点这里先开权限，之后每天到点就会来"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkSoft
                            )
                        }
                    }

                    // 被系统省电掐住后台是最常见的原因，这里给个放行入口
                    if (batteryBlocked) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.cardAlt)
                                .clickable { openBatterySettings(context) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\uD83D\uDD0B", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "让提醒在后台也能响",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.ink
                                )
                                Text(
                                    "系统省电会掐掉后台，App 被清掉后提醒就不来了。点这里放行，" +
                                        "另外在多任务页把知新下拉锁上更稳",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.inkSoft
                                )
                            }
                        }
                    }

                    // 通知被关掉同样收不到，提示去打开
                    if (notifyBlocked) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.cardAlt)
                                .clickable { openNotificationSettings(context) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\uD83D\uDD15", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "通知被关掉了",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.ink
                                )
                                Text(
                                    "点这里把通知打开，不然到点也没动静",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.inkSoft
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    // 点一下立刻弹一条，用来确认提醒到底有没有生效
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TinyButton(
                            "\uD83D\uDD14 立刻发一条试试",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    ReminderReceiver.notifyNow(context)
                                }
                            }
                        )
                    }
                }
            }
        }
        }

        if (focus.show(SettingsFocus.TAGS)) {
        item {
            SettingsSection("🏷", "活动标签", "打卡与记录时可勾选的活动") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.tags.take(4).forEach { t ->
                        Text("#$t", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                    }
                }
                Spacer(Modifier.height(10.dp))
                TinyButton("管理活动") { showTags = true }
            }
        }
        }

        // ---------- 安全与数据 ----------
        if (focus.showSectionTitle()) {
            item {
                Text(
                    "🔐 安全与数据",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent
                )
            }
        }
        if (focus.show(SettingsFocus.PRIVACY)) {
        item {
            SettingsSection("🔒", "小屋密码锁", "打开小屋需输入密码（仅防误触）") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (settings.lockEnabled()) "已开启" else "未开启",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkSoft,
                        modifier = Modifier.weight(1f)
                    )
                    TinyButton("设置 / 修改") { showPassword = true }
                    Spacer(Modifier.width(8.dp))
                    TinyButton("关闭") { onChange(settings.copy(password = "")) }
                }
            }
        }
        }

        if (focus == SettingsFocus.ALL) {
            item {
                SettingsSection("🧹", "回收站", "已删除的故事在这里，可恢复或彻底清空") {
                    TinyButton("打开回收站", onClick = onOpenTrash)
                }
            }
        }

        if (focus.show(SettingsFocus.BACKUP)) {
            item {
                SettingsSection("💾", "备份数据", "导出 JSON 备份，或从备份导入") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TinyButton("📤 导出备份", onClick = onExport)
                        TinyButton("📥 导入备份", onClick = onImport)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "故事 ${entries.size} 篇 · 有记录的日子 $dayCount 天",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                }
            }
            item {
                SettingsSection("🗑️", "清空所有数据", "删除全部故事、自己加的任务和标签、密码") {
                    TinyButton("一键清空") { showClearAll = true }
                }
            }
        }

        // 使用指南在「我的」里点了就直接打开引导，这里只在全部设置里留个入口
        if (focus == SettingsFocus.ALL) {
            item {
                SettingsSection("📖", "使用指南", "重新显示首页的新手引导卡片") {
                    TinyButton("打开引导") {
                        onChange(settings.copy(guideSeen = false))
                        onShowGuide()
                    }
                }
            }
        }

        if (focus.show(SettingsFocus.ABOUT)) {
            item {
                SettingsSection("💫", "关于知新", "版本 v${BuildConfig.VERSION_NAME} · 情绪小屋") {
                    Text(
                        "让每一天活出新意，而不是把同一天重复 365 次。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.ink
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "纯本地存储 · 不上传云端\n你的情绪和故事，只属于你自己。",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "🗂️ 数据版本：v1 · 本地 JSON 存储",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "🔒 隐私说明：你写下的所有故事只保存在本机，不会上传到任何云端；AI 分析仅在填写并保存 Key 后才会把单篇故事发送给你配置的接口。建议定期「导出」备份。密码锁只是防误触，不是真正的加密。",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
            }
        }

        // ---------- AI 整理 ----------
        if (focus.showSectionTitle()) {
            item {
                Text(
                    "🤖 AI 整理",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent
                )
            }
        }
        if (focus.show(SettingsFocus.AI)) {
        item {
            SettingsSection(
                "🤖",
                "AI 分析（可选）",
                "填 OpenAI 兼容 Key 后由 AI 整理记录；不填则用本地模板兜底"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AI_PRESETS.forEach { (label, url, model) ->
                        // 选中看的是输入框里现在的值，点一下就真的换过去（不用再点保存）
                        ChoiceChip(label, aiBase.trim() == url, Modifier.weight(1f)) {
                            aiBase = url
                            aiModel = model
                            onChange(settings.copy(aiBaseUrl = url, aiModel = model))
                            aiResult = "已切到「$label」· 模型 $model"
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SettingsField("接口地址", aiBase) { aiBase = it }
                Spacer(Modifier.height(8.dp))
                SettingsField("模型名", aiModel) { aiModel = it }
                Spacer(Modifier.height(8.dp))
                SettingsField("API Key", aiKey) { aiKey = it }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TinyButton("保存 AI 设置") {
                        onChange(settings.copy(aiBaseUrl = aiBase.trim(), aiModel = aiModel.trim(), aiKey = aiKey.trim()))
                        aiResult = "已保存"
                    }
                    TinyButton("清除 Key") {
                        aiKey = ""
                        onChange(settings.copy(aiKey = ""))
                    }
                    TinyButton("测试连接") {
                        aiResult = "测试中…"
                        val s = settings.copy(aiBaseUrl = aiBase.trim(), aiModel = aiModel.trim(), aiKey = aiKey.trim())
                        CoroutineScope(Dispatchers.Main).launch {
                            aiResult = AiClient.testConnection(s)
                        }
                    }
                }
                if (aiResult.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(aiResult, style = MaterialTheme.typography.bodySmall, color = c.accent)
                }
            }
        }
        }

        // ---------- 关于知新 ----------
        if (focus == SettingsFocus.ALL) {
        item {
            Text(
                "💫 关于知新",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = c.accent
            )
        }
        item {
            SoftCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAbout = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💫", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("关于知新", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        Text(
                            "版本 v${BuildConfig.VERSION_NAME} · 情绪小屋",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkSoft
                        )
                    }
                    Text("›", fontSize = 20.sp, color = c.inkSoft)
                }
            }
        }

        item {
            Text(
                "🔒 隐私说明：你写下的所有故事只保存在本机，不会上传到任何云端；AI 分析仅在填写并保存 Key 后才会把单篇故事发送给你配置的接口。建议定期「导出」备份。密码锁只是防误触，不是真正的加密。\n🗂️ 数据版本：v1 · 本地 JSON 存储",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkSoft
            )
        }
        }

        if (focus != SettingsFocus.ALL) {
            item {
                Text(
                    "其他设置可以在「我的」→ 小屋设置里找到",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        }
    }

    if (showTags) {
        TagManagerDialog(
            tags = settings.tags,
            onDismiss = { showTags = false },
            onCommit = { onChange(settings.copy(tags = it)) }
        )
    }

    if (showPassword) {
        InputDialog(
            title = "设置小屋密码",
            label = "输入 4-6 位数字（留空 = 关闭）",
            initial = settings.password,
            onDismiss = { showPassword = false },
            onConfirm = { value ->
                showPassword = false
                onChange(settings.copy(password = value.trim()))
            }
        )
    }

    if (showClearAll) {
            ConfirmDialog(
                title = "清空所有数据？",
                message = "全部故事、自己加的打卡任务和标签、密码都会被删除，确定继续吗？",
                confirmText = "清空",
                onDismiss = { showClearAll = false },
                onConfirm = {
                    showClearAll = false
                    Store.clearAll(context)
                    onChange(
                        settings.copy(
                            password = "",
                            customItems = DEFAULT_CUSTOM_ITEMS,
                            tags = DEFAULT_TAGS
                        )
                    )
                }
            )
    }

    if (showAbout) {
        androidx.compose.material3.AlertDialog(
            modifier = dialogEnter(),
            onDismissRequest = { showAbout = false },
            containerColor = c.card,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "💫 关于知新",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("知新 · 情绪小屋", style = MaterialTheme.typography.titleMedium, color = c.accent)
                    Text(
                        "版本 v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "让每一天活出新意，而不是把同一天重复365次。",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "纯本地存储 · 不上传云端\n你的情绪和故事，只属于你自己。",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkSoft
                    )
                }
            },
            confirmButton = {
                TinyButton("知道了", onClick = { showAbout = false })
            }
        )
    }
}

/**
 * 一列上下滑动就能选的时间滚轮：滑到中间的那一项即选中。
 * loop = true 时循环滚：小时 23 后面接着 0，分钟 59 后面接着 0。
 */
@Composable
private fun TimeWheel(
    values: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    onSelect: (Int) -> Unit
) {
    val c = LocalAppColors.current
    val size = values.size
    // 循环：把这一列复制很多份，当前值落在中间那份上，来回滑都滑不到头
    val copies = if (loop) 101 else 1
    val total = size * copies
    val middle = size * (copies / 2)
    val targetOf: (Int) -> Int = { if (loop) middle + it else it }
    val state = rememberLazyListState()
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        val target = (targetOf(selected) - 1).coerceAtLeast(0)
        val center = state.firstVisibleItemIndex + 1
        // 第一次组合把当前值摆到中间；之后外面改了值（比如点了常用时段）再挪过去
        if (!settled) {
            settled = true
            state.scrollToItem(target)
        } else if (center % size != selected.coerceIn(0, size - 1)) {
            state.scrollToItem(target)
        }
    }
    // 滑完停下后，把停在中间的那一格当作选中（刚打开时不动，避免改掉原来的时间）
    var userScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            userScrolled = true
        } else if (userScrolled) {
            val center = state.firstVisibleItemIndex + 1
            if (center in 0 until total) {
                val value = center % size
                if (value != selected) onSelect(value)
                // 快滑到头了就悄悄挪回中间那份，这样两头都能继续滑下去
                if (loop && (center < size || center > total - size)) {
                    state.scrollToItem(targetOf(value))
                }
            }
        }
    }
    LazyColumn(
        state = state,
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(total) { index ->
            val text = values[index % size]
            val picked = index % size == selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (picked) c.accentSoft else Color.Transparent)
                    .clickable { onSelect(index % size) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (picked) c.accent else c.inkSoft
                )
            }
        }
    }
}

/** 在 0:00–23:50 之间整体平移提醒时间，跨天自动循环。 */
private fun shiftReminder(current: AppSettings, deltaMinutes: Int): AppSettings {
    val day = 24 * 60
    val total = ((current.remindHour * 60 + current.remindMinute + deltaMinutes) % day + day) % day
    return current.copy(remindHour = total / 60, remindMinute = total % 60)
}

@Composable
private fun SettingsSection(
    emoji: String,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    SoftCard {
        SectionTitle(emoji = emoji, title = title, subtitle = subtitle)
        Spacer(Modifier.height(12.dp))
        Column { content() }
    }
}

@Composable
private fun ChoiceChip(
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
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.accent else c.inkSoft,
            maxLines = 1
        )
    }
}

@Composable
private fun SwitchRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (checked) "已开启" else "关闭",
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkSoft,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = c.line
            )
        )
    }
}

@Composable
private fun Stepper(text: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.cardAlt)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = c.accent)
    }
}

@Composable
private fun SettingsField(label: String, value: String, onChange: (String) -> Unit) {
    val c = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        label = { Text(label, color = c.inkSoft) },
        shape = ChipShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.line,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun TagManagerDialog(
    tags: List<String>,
    onDismiss: () -> Unit,
    onCommit: (List<String>) -> Unit
) {
    val c = LocalAppColors.current
    var adding by remember { mutableStateOf(false) }
    val list = remember(tags) { tags.toMutableStateList() }

    androidx.compose.material3.AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text("🏷 活动标签", style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = {
            Column {
                Text("点标签可删除。", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEach { t ->
                        TagChip(text = "✕ $t", selected = false) { list.remove(t) }
                    }
                }
                if (list.isEmpty()) {
                    Text("还没有标签", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                }
            }
        },
        confirmButton = {
            TinyButton("添加") { adding = true }
        },
        dismissButton = {
            Row {
                TinyButton("完成") { onCommit(list.toList()); onDismiss() }
            }
        }
    )

    if (adding) {
        InputDialog(
            title = "添加标签",
            label = "例如：散步",
            onDismiss = { adding = false },
            onConfirm = { v ->
                adding = false
                if (v.isNotBlank() && !list.contains(v.trim())) list.add(v.trim())
            }
        )
    }
}

@Composable
private fun TrashDialog(
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit,
    onEmpty: () -> Unit
) {
    val c = LocalAppColors.current
    val trash = Store.trash()
    androidx.compose.material3.AlertDialog(
        modifier = dialogEnter(),
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = { Text("🧹 回收站", style = MaterialTheme.typography.titleSmall, color = c.ink) },
        text = {
            if (trash.isEmpty()) {
                Text("回收站是空的～", style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
            } else {
                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    trash.forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                e.text.take(28).ifBlank { "（打卡）" },
                                style = MaterialTheme.typography.bodySmall,
                                color = c.ink,
                                modifier = Modifier.weight(1f)
                            )
                            TinyButton("恢复") { onRestore(e.id) }
                        }
                    }
                }
            }
        },
        confirmButton = { TinyButton("彻底清空") { onEmpty() } },
        dismissButton = { TinyButton("关闭", onClick = onDismiss) }
    )
}
