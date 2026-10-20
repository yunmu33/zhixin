package com.example.zhixin.ui

import android.content.Context
import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.Account
import com.example.zhixin.data.CUSTOM_TAG
import com.example.zhixin.data.Category
import com.example.zhixin.data.CustomItem
import com.example.zhixin.data.DateKey
import com.example.zhixin.data.Entry
import com.example.zhixin.data.Heat
import com.example.zhixin.data.Mood
import com.example.zhixin.data.Prefs
import com.example.zhixin.data.Store
import com.example.zhixin.data.TodoItem
import com.example.zhixin.data.inCheckWindow
import com.example.zhixin.notify.applyReminder
import com.example.zhixin.ui.components.EntryDetailDialog
import com.example.zhixin.ui.components.SkyDecor
import com.example.zhixin.ui.screens.AccountDialog
import com.example.zhixin.ui.screens.CheckInScreen
import com.example.zhixin.ui.screens.MoodScreen
import com.example.zhixin.ui.screens.ProfileScreen
import com.example.zhixin.ui.screens.EditorScreen
import com.example.zhixin.ui.screens.GuideDialog
import com.example.zhixin.ui.screens.LockScreen
import com.example.zhixin.ui.screens.MineScreen
import com.example.zhixin.ui.screens.MoodJarScreen
import com.example.zhixin.ui.screens.NewScreen
import com.example.zhixin.ui.screens.SettingsFocus
import com.example.zhixin.ui.screens.SettingsScreen
import com.example.zhixin.ui.screens.TrashScreen
import com.example.zhixin.ui.screens.TaskCheckInScreen
import com.example.zhixin.ui.screens.AchievementScreen
import com.example.zhixin.ui.screens.AddTaskScreen
import com.example.zhixin.ui.screens.Badge
import com.example.zhixin.ui.screens.buildBadges
import com.example.zhixin.ui.screens.mineStats
import com.example.zhixin.ui.screens.SettingsHomeScreen
import com.example.zhixin.ui.screens.TaskDetailScreen
import com.example.zhixin.ui.screens.TaskHeatScreen
import com.example.zhixin.ui.screens.TasksScreen
import com.example.zhixin.ui.screens.TodoScreen
import com.example.zhixin.ui.screens.taskColor
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.ZhixinTheme
import com.example.zhixin.ui.theme.fadeTransition
import com.example.zhixin.ui.theme.pageTransition
import com.example.zhixin.ui.theme.tabTransition
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

/** 一条待办的 id：时间 + 随机数，够用就行。 */
private fun newTodoId(): String =
    "t" + System.currentTimeMillis().toString() + (0..999).random()

private enum class Page { Main, CheckIn, TaskCheckIn, Settings, Trash, Heat, Jar }

/** 顶层显示的是哪一层（用来决定页面切换动画往哪个方向走）。 */
private enum class TopPage {
    Lock, Main, Editor, AddTask, TaskDetail,
    CheckIn, TaskCheckIn, Settings, Trash, Heat, Jar
}

/** 「我的」里点开的二级页面。 */
private enum class MineSubPage { None, Achievements, SettingsHome, Profile }

private data class EditorArg(
    val entry: Entry?,
    val text: String,
    val category: String,
    val dateKey: String = "",
    val prompt: String = ""
)

private val TABS = listOf(
    "\uD83C\uDFAF" to "任务",
    "\uD83D\uDDD3" to "待办",
    "\uD83C\uDF31" to "日日新",
    "\uD83D\uDE0A" to "心情日记",
    "\uD83D\uDC64" to "我的"
)

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val settings by Prefs.settings.collectAsState()
    val rawEntries by Store.all.collectAsState()
    val entries = remember(rawEntries) {
        rawEntries.filter { !it.deleted }.sortedByDescending { it.time }
    }

    var unlocked by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(Page.Main) }
    // 刚点亮的成就排队提示，一次显示一个
    var badgeQueue by remember { mutableStateOf(listOf<Badge>()) }
    var tab by remember { mutableIntStateOf(0) }
    // 从心情日记点到「心情罐」时，跟着打开它当时正在看的那个月
    var jarMonth by remember { mutableStateOf<Calendar?>(null) }
    // 任务主页日历选中的日期，进详细打卡页时带过去
    var taskCheckDateKey by remember {
        mutableStateOf(DateKey.fromMillis(System.currentTimeMillis()))
    }
    var detail by remember { mutableStateOf<Entry?>(null) }
    var editor by remember { mutableStateOf<EditorArg?>(null) }
    // 打开了哪个任务的设置页（存名字，改名后跟着变）
    var taskDetailName by remember { mutableStateOf<String?>(null) }
    // 是否打开添加任务页
    var addingTask by remember { mutableStateOf(false) }
    // 从「我的」点进来的设置分类，默认是全部设置
    var settingsFocus by remember { mutableStateOf(SettingsFocus.ALL) }
    // 「我的」的滚动位置，进出设置页回来时不会跳回顶部
    val mineListState = rememberLazyListState()
    var mineSubPage by remember { mutableStateOf(MineSubPage.None) }
    // 待办页看到哪一天
    var todoDateKey by remember {
        mutableStateOf(DateKey.fromMillis(System.currentTimeMillis()))
    }
    var guide by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }

    LaunchedEffect(settings.guideSeen) {
        if (!settings.guideSeen) guide = true
    }

    // 通知权限（Android 13 起必须给，否则提醒一条都发不出来）
    val notifyPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // 给了权限就立刻把提醒排上，当天就能生效
        if (granted) applyReminder(context, settings)
    }
    // 每次回到前台都补排一次：系统清掉的闹钟、改过的时间，都能在这里补回来
    var resumed by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(
        settings.remindEnabled,
        settings.remindHour,
        settings.remindMinute,
        settings.remindRepeat,
        settings.remindWeekday,
        resumed
    ) {
        if (!settings.remindEnabled) return@LaunchedEffect
        applyReminder(context, settings) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifyPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 成就解锁提示：记录变化时算一遍，只提示「这次新点亮」的那些
    val unlockedNames = settings.unlockedBadges
    val badgesSeeded = settings.badgesSeeded
    LaunchedEffect(rawEntries, unlockedNames, badgesSeeded) {
        val all = buildBadges(mineStats(rawEntries))
        val got = all.filter { it.got }.map { it.name }
        if (!badgesSeeded) {
            // 第一次（或老数据升级）先默默记个底，不打扰
            Prefs.patch(context) { it.copy(unlockedBadges = got, badgesSeeded = true) }
            return@LaunchedEffect
        }
        val fresh = got.filterNot { it in unlockedNames }
        if (fresh.isNotEmpty()) {
            Prefs.patch(context) {
                it.copy(unlockedBadges = (it.unlockedBadges + fresh).distinct())
            }
            // 一次解锁太多（比如导入旧数据）就只弹前几个，别刷屏
            badgeQueue = (badgeQueue + all.filter { it.name in fresh })
                .distinctBy { it.name }
                .take(5)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?.toString(Charsets.UTF_8)
        }.getOrNull()
        if (json.isNullOrBlank()) {
            toast(context, "没能读到这个文件")
            return@rememberLauncherForActivityResult
        }
        val incoming = Store.fromJson(json)
        if (incoming.isEmpty()) {
            toast(context, "文件里没有可导入的记录")
            return@rememberLauncherForActivityResult
        }
        val existing = Store.all.value
        val ids = existing.map { it.id }.toSet()
        Store.replaceAll(context, existing + incoming.filter { it.id !in ids })
        toast(context, "导入完成：新增 ${incoming.size} 条")
    }

    fun export() {
        Store.flush(context)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.CHINA).format(Date())
        val file = File(context.cacheDir, "情绪小屋备份-$stamp.json")
        runCatching {
            file.writeText(Store.toJson())
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "导出备份"))
        }.onFailure {
            toast(context, "导出失败：${it.message}")
        }
    }

    fun saveEntry(entry: Entry) {
        if (entry.id.isBlank()) Store.add(context, entry) else Store.update(context, entry)
        editor = null
        toast(context, "已经好好收进收藏本啦 ✓")
    }

    /**
     * 任务打卡 / 取消：一天可以打好几次（次数由「每日目标」决定），并遵守打卡时段。
     *
     * @param note 弹窗里写的打卡内容，会记进这条记录
     * @param showToast 打卡页自己会浮激励语小框，就不用系统 Toast 了
     * @return true = 真的打上了；false = 取消打卡或不在打卡时段
     */
    fun quickToggle(item: CustomItem, note: String = "", showToast: Boolean = true): Boolean {
        val now = System.currentTimeMillis()
        if (!inCheckWindow(item, now)) {
            toast(
                context,
                "「${item.name}」只能 ${item.windowStart} ~ ${item.windowEnd} 打卡哦"
            )
            return false
        }
        val today = todayKey()
        val todays = Store.all.value.filter {
            !it.deleted && it.dateKey == today &&
                it.tags.contains(CUSTOM_TAG) && it.tags.contains(item.name)
        }
        val target = item.target()
        if (todays.size >= target) {
            // 撤销：没写内容的直接清掉不留痕，写了内容的才挪进回收站（误删还能找回）
            val auto = item.autoText()
            var kept = 0
            todays.forEach {
                if (it.text == auto) {
                    Store.remove(context, it.id)
                } else {
                    Store.softDelete(context, it.id)
                    kept++
                }
            }
            if (showToast) {
                toast(
                    context,
                    if (kept > 0) "已取消「${item.name}」，写了内容的记录放进回收站了"
                    else "已取消「${item.name}」"
                )
            }
            return false
        } else {
            Store.add(
                context,
                Entry(
                    time = now,
                    category = Category.DAILY.key,
                    text = if (note.isBlank()) {
                        item.autoText()
                    } else {
                        "${item.emoji} ${note.trim()}"
                    },
                    tags = listOf(item.name, CUSTOM_TAG),
                    score = item.rewardValue()
                )
            )
            if (showToast) {
                toast(
                    context,
                    if (target > 1) "${item.emoji} 「${item.name}」${todays.size + 1}/$target ✓"
                    else "${item.emoji} 「${item.name}」打卡成功 ✓"
                )
            }
            return true
        }
    }

    ZhixinTheme(settings) {
        val c = LocalAppColors.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
        ) {
            if (settings.decor) {
                SkyDecor(modifier = Modifier.align(Alignment.TopCenter))
            }

            // 当前停在哪一层页面：变了就走一段切换动画
            val topPage = when {
                settings.lockEnabled() && !unlocked -> TopPage.Lock
                editor != null -> TopPage.Editor
                addingTask -> TopPage.AddTask
                taskDetailName != null -> TopPage.TaskDetail
                page == Page.CheckIn -> TopPage.CheckIn
                page == Page.TaskCheckIn -> TopPage.TaskCheckIn
                page == Page.Settings -> TopPage.Settings
                page == Page.Trash -> TopPage.Trash
                page == Page.Heat -> TopPage.Heat
                page == Page.Jar -> TopPage.Jar
                else -> TopPage.Main
            }
            AnimatedContent(
                targetState = topPage,
                transitionSpec = {
                    when {
                        targetState == TopPage.Lock || initialState == TopPage.Lock ->
                            fadeTransition()
                        targetState == TopPage.Main -> pageTransition(forward = false)
                        initialState == TopPage.Main -> pageTransition(forward = true)
                        else -> fadeTransition()
                    }
                },
                label = "topPage"
            ) { key ->
                // 按「目标层级」渲染：动画期间退场的那一页还是它自己的内容，不会闪
            when (key) {
                TopPage.Lock -> {
                    LockScreen(settings = settings, onUnlock = { unlocked = true })
                }

                TopPage.Editor -> {
                    // 退回主页时 editor 已经清空了，这里别硬取
                    editor?.let { arg ->
                        EditorScreen(
                            initial = arg.entry,
                            presetText = arg.text,
                            presetCategory = arg.category,
                            presetDateKey = arg.dateKey,
                            prompt = arg.prompt,
                            settings = settings,
                            onSave = { saveEntry(it) },
                            onBack = { editor = null }
                        )
                    }
                }

                TopPage.AddTask -> {
                    AddTaskScreen(
                        onConfirm = { item ->
                            if (settings.customItems.any { it.name == item.name }) {
                                toast(context, "已经有同名任务啦")
                            } else {
                                Prefs.patch(context) {
                                    it.copy(customItems = it.customItems + item)
                                }
                                addingTask = false
                                toast(context, "已添加任务「${item.name}」")
                            }
                        },
                        onBack = { addingTask = false }
                    )
                }

                TopPage.TaskDetail -> {
                    val name = taskDetailName
                    val item = name?.let { n ->
                        settings.customItems.firstOrNull { it.name == n }
                    }
                    if (name != null && item == null) {
                        // 任务被删掉了 → 自动退回
                        LaunchedEffect(name) { taskDetailName = null }
                    } else if (item != null) {
                        TaskDetailScreen(
                            item = item,
                            accent = taskColor(
                                item,
                                settings.customItems.indexOfFirst { it.name == name }
                            ),
                            entries = entries,
                            onChange = { updated ->
                                val oldName = item.name
                                if (updated.name != oldName &&
                                    settings.customItems.any { it.name == updated.name }
                                ) {
                                    toast(context, "已经有同名任务啦")
                                } else {
                                    Prefs.patch(context) { s ->
                                        s.copy(
                                            customItems = s.customItems.map {
                                                if (it.name == oldName) updated else it
                                            }
                                        )
                                    }
                                    // 改了名字 → 老记录上的标签一起换，历史不断
                                    if (updated.name != oldName) {
                                        Store.all.value
                                            .filter {
                                                it.tags.contains(CUSTOM_TAG) &&
                                                    it.tags.contains(oldName)
                                            }
                                            .forEach {
                                                Store.update(
                                                    context,
                                                    it.copy(
                                                        tags = it.tags.map { t ->
                                                            if (t == oldName) updated.name else t
                                                        }
                                                    )
                                                )
                                            }
                                        taskDetailName = updated.name
                                    }
                                }
                            },
                            onToggleToday = { quickToggle(item) },
                            onDelete = {
                                Prefs.patch(context) { s ->
                                    s.copy(
                                        customItems = s.customItems.filterNot { it.name == name }
                                    )
                                }
                                taskDetailName = null
                                toast(context, "已删除「$name」")
                            },
                            onBack = { taskDetailName = null }
                        )
                    }
                }

                TopPage.CheckIn -> {
                    CheckInScreen(
                        settings = settings,
                        entries = entries,
                        onSubmit = { dateKey, mood, tags, note ->
                            val time = if (dateKey == DateKey.fromMillis(System.currentTimeMillis()))
                                System.currentTimeMillis()
                            else DateKey.toMillis(dateKey) + 12 * 60 * 60 * 1000L
                            Store.add(
                                context,
                                Entry(
                                    time = time,
                                    dateKey = dateKey,
                                    mood = mood.key,
                                    score = mood.score,
                                    category = Category.DAILY.key,
                                    tags = tags,
                                    text = note
                                )
                            )
                        },
                        onBack = { page = Page.Main }
                    )
                }

                TopPage.Jar -> {
                    MoodJarScreen(
                        entries = entries,
                        initialMonth = jarMonth,
                        onBack = { page = Page.Main }
                    )
                }

                TopPage.TaskCheckIn -> {
                    TaskCheckInScreen(
                        settings = settings,
                        entries = entries,
                        initialDateKey = taskCheckDateKey,
                        onSubmit = { dateKey, summary, newItems, images ->
                            val today = todayKey()
                            val time = if (dateKey == today)
                                System.currentTimeMillis()
                            else DateKey.toMillis(dateKey) + 12 * 60 * 60 * 1000L
                            // 每一条新勾选的任务 → 一条 DAILY entry（与主页打卡并存）
                            newItems.forEach { item ->
                                Store.add(
                                    context,
                                    Entry(
                                        time = time,
                                        dateKey = dateKey,
                                        category = Category.DAILY.key,
                                        text = item.autoText(),
                                        tags = listOf(item.name, CUSTOM_TAG),
                                        score = 7
                                    )
                                )
                            }
                            // 文字总结 + 配图 → 一条 DAILY entry（图片文件名随 JSON 持久化）
                            if (summary.isNotBlank() || images.isNotEmpty()) {
                                Store.add(
                                    context,
                                    Entry(
                                        time = time,
                                        dateKey = dateKey,
                                        category = Category.DAILY.key,
                                        text = summary,
                                        tags = if (newItems.isNotEmpty())
                                            listOf(CUSTOM_TAG)
                                        else emptyList(),
                                        images = images,
                                        score = 5
                                    )
                                )
                            }
                        },
                        // 成功弹窗里补写的那句话，也记成一条
                        onNotePromptChange = { on ->
                            Prefs.patch(context) { it.copy(checkNotePrompt = on) }
                        },
                        onAddNote = { dateKey, text ->
                            val time = if (dateKey == todayKey())
                                System.currentTimeMillis()
                            else DateKey.toMillis(dateKey) + 12 * 60 * 60 * 1000L
                            Store.add(
                                context,
                                Entry(
                                    time = time,
                                    dateKey = dateKey,
                                    category = Category.DAILY.key,
                                    text = text,
                                    tags = listOf(CUSTOM_TAG),
                                    score = 5
                                )
                            )
                            toast(context, "记下了 ✨")
                        },
                        onBack = { page = Page.Main }
                    )
                }

                TopPage.Settings -> {
                    SettingsScreen(
                        settings = settings,
                        entries = entries,
                        onChange = { Prefs.update(context, it) },
                        onExport = { export() },
                        onImport = { importLauncher.launch("application/json") },
                        onShowGuide = { guide = true },
                        onOpenTrash = { page = Page.Trash },
                        focus = settingsFocus,
                        onBack = { page = Page.Main }
                    )
                }

                TopPage.Trash -> {
                    TrashScreen(
                        trash = remember(rawEntries) {
                            // 任务打卡删掉后也进回收站，误删还能恢复
                            rawEntries
                                .filter { it.deleted }
                                .sortedByDescending { it.time }
                        },
                        onRestore = {
                            Store.restore(context, it.id)
                            toast(context, "已恢复 ✨")
                        },
                        onDeleteForever = {
                            Store.remove(context, it.id)
                            toast(context, "已彻底删除")
                        },
                        trashTodos = remember(settings) {
                            settings.todos.filter { it.deleted }
                                .sortedByDescending { it.createdAt }
                        },
                        onRestoreTodo = {
                            Prefs.patch(context) { s ->
                                s.copy(
                                    todos = s.todos.map { t ->
                                        if (t.id == it.id) t.copy(deleted = false) else t
                                    }
                                )
                            }
                            toast(context, "待办已恢复 ✨")
                        },
                        onDeleteTodoForever = {
                            Prefs.patch(context) { s ->
                                s.copy(todos = s.todos.filterNot { t -> t.id == it.id })
                            }
                            toast(context, "已彻底删除")
                        },
                        onEmpty = {
                            Store.emptyTrash(context)
                            Prefs.patch(context) {
                                it.copy(todos = it.todos.filterNot { t -> t.deleted })
                            }
                            toast(context, "回收站已清空")
                        },
                        onBack = { page = Page.Main }
                    )
                }

                TopPage.Heat -> {
                    TaskHeatScreen(
                        entries = entries,
                        settings = settings,
                        onBack = { page = Page.Main }
                    )
                }

                TopPage.Main -> {
                    MainScaffold(
                        entries = entries,
                        settings = settings,
                        tab = tab,
                        onTabChange = {
                            tab = it
                            mineSubPage = MineSubPage.None
                        },
                        onOpenSettings = { focus ->
                            settingsFocus = focus
                            page = Page.Settings
                        },
                        mineSubPage = mineSubPage,
                        onMineSubPage = { mineSubPage = it },
                        onOpenTrash = { page = Page.Trash },
                        // 打卡页自己浮激励语，这里就不弹系统 Toast 了
                        onQuickToggle = { item, note -> quickToggle(item, note, false) },
                        onOpenDetail = { taskDetailName = it.name },
                        onOpenAdd = { addingTask = true },
                        onOpenHeat = { page = Page.Heat },
                        // 只改打卡里写的内容，打卡本身留着
                        onEditRecord = { entry, text ->
                            Store.update(context, entry.copy(text = text))
                        },
                        // 成功弹窗里补写的那句话，挂到刚打的那条记录上
                        onQuickNote = { item, note ->
                            val last = Store.all.value.filter {
                                !it.deleted && it.dateKey == todayKey() &&
                                    it.tags.contains(CUSTOM_TAG) && it.tags.contains(item.name)
                            }.maxByOrNull { it.time }
                            if (last != null) {
                                Store.update(
                                    context,
                                    last.copy(text = "${item.emoji} ${note.trim()}")
                                )
                                toast(context, "记下了 \u2728")
                            }
                        },
                        onDeleteCustom = { item ->
                            val updated = settings.customItems.filterNot {
                                it.name == item.name
                            }
                            Prefs.patch(context) { it.copy(customItems = updated) }
                            toast(context, "已删除「${item.name}」")
                        },
                        todos = settings.todos,
                        todoDateKey = todoDateKey,
                        onTodoDateChange = { todoDateKey = it },
                        onOpenEntry = { detail = it },
                        onOpenJar = { cal -> jarMonth = cal; page = Page.Jar },
                        onProfileChange = { Prefs.update(context, it) },
                        onShowGuide = { guide = true },
                        onLogin = { showLogin = true },
                        onLogout = {
                            Account.logout(context)
                            toast(context, "已退出登录")
                        },
                        onExport = { export() },
                        onImport = { importLauncher.launch("application/json") },
                        mineListState = mineListState,
                        onUpdateEntry = { entry -> Store.update(context, entry) },
                        onQuickSave = { text ->
                            Store.add(
                                context,
                                Entry(
                                    time = System.currentTimeMillis(),
                                    category = Category.NEW.key,
                                    text = text,
                                    score = 7
                                )
                            )
                            toast(context, "新收获已记下 \uD83C\uDF31 热度 +${Heat.BASE}")
                        },
                        onAddNew = {
                            editor = EditorArg(null, "", Category.NEW.key, "")
                        }
                    )
                }
            }
            }
        }

        detail?.let { entry ->
            EntryDetailDialog(
                entry = entry,
                onDismiss = { detail = null },
                onEdit = {
                    detail = null
                    editor = EditorArg(it, "", it.category, "")
                },
                onDelete = {
                    Store.softDelete(context, it.id)
                    detail = null
                    toast(context, "已收进回收站")
                }
            )
        }

        if (guide) {
            GuideDialog(
                loggedIn = Account.loggedIn(settings),
                onLogin = { guide = false; showLogin = true }
            ) { never ->
                guide = false
                if (never) Prefs.patch(context) { it.copy(guideSeen = true) }
            }
        }

        if (showLogin) {
            AccountDialog(context = context, onDismiss = { showLogin = false })
        }

        // 成就解锁提示浮在最上层
        BadgeUnlockBanner(
            badge = badgeQueue.firstOrNull(),
            left = badgeQueue.size,
            onDone = { badgeQueue = badgeQueue.drop(1) }
        )
    }

    BackHandler(
        enabled = editor != null || page != Page.Main ||
            taskDetailName != null || addingTask ||
            mineSubPage != MineSubPage.None
    ) {
        when {
            // 一层一层退：先关浮着的东西，再退回上一页，最后才回「我的」
            editor != null -> editor = null
            addingTask -> addingTask = false
            taskDetailName != null -> taskDetailName = null
            // 设置 / 回收站这类二级页：退回进它时的那页（比如「小屋设置」），不是直接回「我的」
            page != Page.Main -> page = Page.Main
            mineSubPage != MineSubPage.None -> mineSubPage = MineSubPage.None
            else -> page = Page.Main
        }
    }
}

@Composable
private fun MainScaffold(
    entries: List<Entry>,
    settings: AppSettings,
    tab: Int,
    onTabChange: (Int) -> Unit,
    onOpenSettings: (SettingsFocus) -> Unit,
    onQuickToggle: (CustomItem, String) -> Boolean,
    onDeleteCustom: (CustomItem) -> Unit,
    onOpenDetail: (CustomItem) -> Unit,
    onOpenAdd: () -> Unit,
    onOpenHeat: () -> Unit,
    /** 改任务打卡里写的内容（打卡记录本身不动）。 */
    onEditRecord: (Entry, String) -> Unit,
    /** 成功弹窗里补写的那句话，挂到刚打的那条记录上。 */
    onQuickNote: (CustomItem, String) -> Unit,
    todos: List<TodoItem>,
    todoDateKey: String,
    onTodoDateChange: (String) -> Unit,
    onUpdateEntry: (Entry) -> Unit,
    onQuickSave: (String) -> Unit,
    onAddNew: () -> Unit,
    onOpenEntry: (Entry) -> Unit,
    /** 心情日记页右上角：打开心情罐。 */
    onOpenJar: (Calendar) -> Unit,
    onProfileChange: (AppSettings) -> Unit,
    onShowGuide: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    mineSubPage: MineSubPage,
    onMineSubPage: (MineSubPage) -> Unit,
    onOpenTrash: () -> Unit,
    mineListState: LazyListState
) {
    val c = LocalAppColors.current
    val scopeContext = LocalContext.current
    Scaffold(
        containerColor = Color.Transparent,
        // 用 safeDrawing（含状态栏 + 刘海/打孔 cutout + 手势条）而不是默认的 systemBars
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = c.card,
                contentColor = c.ink
            ) {
                TABS.forEachIndexed { index, (emoji, label) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { onTabChange(index) },
                        icon = { Text(emoji, fontSize = 18.sp) },
                        label = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.accent,
                            selectedTextColor = c.accent,
                            unselectedIconColor = c.inkSoft,
                            unselectedTextColor = c.inkSoft,
                            indicatorColor = c.accentSoft
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 底部切 tab 时也滑一下：往左点就往左滑，跟手指方向一致
            AnimatedContent(
                targetState = tab,
                transitionSpec = { tabTransition(targetState > initialState) },
                label = "tab"
            ) { t ->
            when (t) {
                0 -> TasksScreen(
                    entries = entries,
                    settings = settings,
                    onQuickToggle = onQuickToggle,
                    onDeleteCustom = onDeleteCustom,
                    onOpenDetail = onOpenDetail,
                    onOpenAdd = onOpenAdd,
                    onOpenHeat = onOpenHeat,
                    onEditRecord = onEditRecord,
                    onQuickNote = onQuickNote
                )

                1 -> TodoScreen(
                    todos = todos,
                    dateKey = todoDateKey,
                    onDateChange = onTodoDateChange,
                    onAdd = { text, zone, start, end ->
                        Prefs.patch(scopeContext) { s ->
                            s.copy(
                                todos = s.todos + TodoItem(
                                    id = newTodoId(),
                                    text = text,
                                    zone = zone,
                                    // 没设时间就落在清单这天，设了就按开始那天算
                                    dateKey = if (start > 0L) {
                                        DateKey.fromMillis(start)
                                    } else todoDateKey,
                                    createdAt = System.currentTimeMillis(),
                                    startAt = start,
                                    endAt = end
                                )
                            )
                        }
                    },
                    onToggle = { id ->
                        Prefs.patch(scopeContext) { s ->
                            s.copy(
                                todos = s.todos.map {
                                    if (it.id == id) {
                                        it.copy(
                                            done = !it.done,
                                            doneAt = if (it.done) 0L else System.currentTimeMillis()
                                        )
                                    } else it
                                }
                            )
                        }
                    },
                    onEdit = { item, text, start, end ->
                        Prefs.patch(scopeContext) { s ->
                            s.copy(
                                todos = s.todos.map {
                                    if (it.id == item.id) {
                                        it.copy(
                                            text = text,
                                            startAt = start,
                                            endAt = end,
                                            dateKey = if (start > 0L) {
                                                DateKey.fromMillis(start)
                                            } else {
                                                item.dateKey.ifBlank { todoDateKey }
                                            }
                                        )
                                    } else it
                                }
                            )
                        }
                    },
                    // 删掉先收进回收站，别直接抹了
                    onDelete = { item ->
                        Prefs.patch(scopeContext) { s ->
                            s.copy(
                                todos = s.todos.map {
                                    if (it.id == item.id) it.copy(deleted = true) else it
                                }
                            )
                        }
                        toast(scopeContext, "已放进回收站")
                    },
                    onMove = { item, zone ->
                        Prefs.patch(scopeContext) { s ->
                            s.copy(
                                todos = s.todos.map {
                                    if (it.id == item.id) it.copy(zone = zone) else it
                                }
                            )
                        }
                    },
                    onClearDone = {
                        Prefs.patch(scopeContext) { s ->
                            s.copy(
                                // 同样收进回收站，误清了还能在回收站里找回
                                todos = s.todos.map {
                                    if (it.covers(todoDateKey) && it.done) {
                                        it.copy(deleted = true)
                                    } else it
                                }
                            )
                        }
                        toast(scopeContext, "做完的收进回收站啦")
                    }
                )

                2 -> NewScreen(
                    entries = entries,
                    onOpenEntry = onOpenEntry,
                    onQuickSave = onQuickSave,
                    onUpdateEntry = onUpdateEntry,
                    onAdd = onAddNew
                )

                3 -> MoodScreen(
                    entries = entries,
                    onOpenJar = onOpenJar,
                    onQuickCheckIn = { mood, text, dateKey, tags ->
                        Store.add(
                            scopeContext,
                            Entry(
                                time = DateKey.toMillis(dateKey) + 12 * 3600 * 1000,
                                dateKey = dateKey,
                                category = Category.MOOD.key,
                                mood = mood.key,
                                score = mood.score,
                                text = text,
                                tags = tags
                            )
                        )
                        toast(scopeContext, "心情已记录 ${mood.emoji} 热度 +${Heat.BASE}")
                    },
                    onUpdateEntry = onUpdateEntry,
                    onAddDiary = { dateKey, title, text, images, audios ->
                        val time = if (dateKey == todayKey()) System.currentTimeMillis()
                        else DateKey.toMillis(dateKey) + 12 * 3600 * 1000
                        Store.add(
                            scopeContext,
                            Entry(
                                time = time,
                                dateKey = dateKey,
                                category = Category.DIARY.key,
                                text = text,
                                summary = title,
                                images = images,
                                audio = audios,
                                score = 5
                            )
                        )
                        toast(scopeContext, "日记已记下 ✨")
                    }
                )

                // 「我的」里再往下一层（档案 / 成就 / 小屋设置）同样滑着进出
                else -> AnimatedContent(
                    targetState = mineSubPage,
                    transitionSpec = {
                        pageTransition(forward = targetState != MineSubPage.None)
                    },
                    label = "mineSubPage"
                ) { sub ->
                when (sub) {
                    MineSubPage.None -> MineScreen(
                        settings = settings,
                        entries = entries,
                        onChange = onProfileChange,
                        onOpenAchievements = {
                            onMineSubPage(MineSubPage.Achievements)
                        },
                        onOpenSettingsHome = {
                            onMineSubPage(MineSubPage.SettingsHome)
                        },
                        onOpenProfile = { onMineSubPage(MineSubPage.Profile) },
                        onLogin = onLogin,
                        onLogout = onLogout,
                        listState = mineListState
                    )

                    MineSubPage.Profile -> ProfileScreen(
                        settings = settings,
                        entries = entries,
                        onChange = onProfileChange,
                        onBack = { onMineSubPage(MineSubPage.None) }
                    )

                    MineSubPage.Achievements -> AchievementScreen(
                        entries = entries,
                        onBack = { onMineSubPage(MineSubPage.None) }
                    )

                    MineSubPage.SettingsHome -> SettingsHomeScreen(
                        settings = settings,
                        entries = entries,
                        onOpen = onOpenSettings,
                        onOpenTrash = onOpenTrash,
                        onShowGuide = onShowGuide,
                        onBack = { onMineSubPage(MineSubPage.None) }
                    )
                }
                }
            }
            }
        }
    }
}

/** 解锁成就时从顶部滑下来的小条：一次显示一个，几个连着解锁就排队显示。 */
@Composable
private fun BadgeUnlockBanner(
    badge: Badge?,
    left: Int,
    onDone: () -> Unit
) {
    val c = LocalAppColors.current
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(badge?.name) {
        if (badge == null) return@LaunchedEffect
        show = true
        delay(2600)
        show = false
        delay(320)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = badge != null && show,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
        if (badge != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.card)
                    .border(1.dp, c.accent, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(badge.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "\uD83C\uDF89 解锁新成就",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.accent
                    )
                    Text(
                        badge.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink
                    )
                }
                if (left > 1) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "+${left - 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkSoft
                    )
                }
            }
        }
        }
    }
}

private fun todayKey(): String =
    DateKey.fromMillis(System.currentTimeMillis())

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
