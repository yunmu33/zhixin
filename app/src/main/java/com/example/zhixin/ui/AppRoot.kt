package com.example.zhixin.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import com.example.zhixin.ui.components.EntryDetailDialog
import com.example.zhixin.ui.components.SkyDecor
import com.example.zhixin.ui.screens.AccountDialog
import com.example.zhixin.ui.screens.CalendarScreen
import com.example.zhixin.ui.screens.CheckInScreen
import com.example.zhixin.ui.screens.CollectScreen
import com.example.zhixin.ui.screens.EditorScreen
import com.example.zhixin.ui.screens.GuideDialog
import com.example.zhixin.ui.screens.HomeScreen
import com.example.zhixin.ui.screens.LockScreen
import com.example.zhixin.ui.screens.MineScreen
import com.example.zhixin.ui.screens.NewScreen
import com.example.zhixin.ui.screens.SettingsScreen
import com.example.zhixin.ui.theme.LocalAppColors
import com.example.zhixin.ui.theme.ZhixinTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Page { Main, CheckIn, Settings }

private data class EditorArg(
    val entry: Entry?,
    val text: String,
    val category: String,
    val dateKey: String = "",
    val prompt: String = ""
)

private val TABS = listOf(
    "\uD83C\uDFE0" to "主页",
    "\uD83D\uDCC5" to "日历",
    "\uD83C\uDF31" to "日日新",
    "\uD83D\uDCD6" to "小屋日记",
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
    var tab by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Entry?>(null) }
    var editor by remember { mutableStateOf<EditorArg?>(null) }
    var guide by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }

    LaunchedEffect(settings.guideSeen) {
        if (!settings.guideSeen) guide = true
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

            when {
                settings.lockEnabled() && !unlocked -> {
                    LockScreen(settings = settings, onUnlock = { unlocked = true })
                }

                editor != null -> {
                    val arg = editor!!
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

                page == Page.CheckIn -> {
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

                page == Page.Settings -> {
                    SettingsScreen(
                        settings = settings,
                        entries = entries,
                        onChange = { Prefs.update(context, it) },
                        onExport = { export() },
                        onImport = { importLauncher.launch("application/json") },
                        onShowGuide = { guide = true },
                        onBack = { page = Page.Main }
                    )
                }

                else -> {
                    MainScaffold(
                        entries = entries,
                        settings = settings,
                        tab = tab,
                        onTabChange = { tab = it },
                        onOpenSettings = { page = Page.Settings },
                        onGoCheckIn = { page = Page.CheckIn },
                        onWrite = { text, prompt ->
                            editor = EditorArg(null, text, Category.DAILY.key, "", prompt)
                        },
                        onGoNew = { tab = 2 },
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
                        },
                        onAddAt = { key ->
                            editor = EditorArg(
                                entry = null,
                                text = "",
                                category = Category.DAILY.key,
                                dateKey = key,
                                prompt = "记下 $key 这一天"
                            )
                        },
                        onOpenEntry = { detail = it },
                        onDelete = {
                            Store.softDelete(context, it.id)
                            toast(context, "已收进回收站，可在设置里恢复")
                        },
                        onSettingsChange = { Prefs.update(context, it) },
                        onCustomToggle = { item ->
                            val today = DateKey.fromMillis(System.currentTimeMillis())
                            val done = entries.firstOrNull {
                                it.dateKey == today && !it.deleted &&
                                    it.tags.contains(CUSTOM_TAG) && it.tags.contains(item.name)
                            }
                            if (done != null) {
                                Store.softDelete(context, done.id)
                                toast(context, "已取消「${item.name}」")
                            } else {
                                Store.add(
                                    context,
                                    Entry(
                                        time = System.currentTimeMillis(),
                                        category = Category.DAILY.key,
                                        text = "${item.emoji} 完成了「${item.name}」",
                                        tags = listOf(item.name, CUSTOM_TAG),
                                        score = 7
                                    )
                                )
                                toast(context, "${item.emoji} ${item.name} 打卡完成 🔥 +${Heat.BASE}")
                            }
                        },
                        onProfileChange = { Prefs.update(context, it) },
                        onShowGuide = { guide = true },
                        onLogin = { showLogin = true },
                        onLogout = {
                            Account.logout(context)
                            toast(context, "已退出登录")
                        },
                        onExport = { export() },
                        onImport = { importLauncher.launch("application/json") },
                        onUpdateEntry = { entry -> Store.update(context, entry) },
                        onAddDiary = { editor = EditorArg(null, "", Category.DAILY.key, "") }
                    )
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
    }

    BackHandler(enabled = editor != null || page != Page.Main) {
        when {
            editor != null -> editor = null
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
    onOpenSettings: () -> Unit,
    onGoCheckIn: () -> Unit,
    onWrite: (String, String) -> Unit,
    onGoNew: () -> Unit,
    onQuickSave: (String) -> Unit,
    onAddNew: () -> Unit,
    onAddAt: (String) -> Unit,
    onUpdateEntry: (Entry) -> Unit,
    onAddDiary: () -> Unit,
    onOpenEntry: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onCustomToggle: (CustomItem) -> Unit,
    onProfileChange: (AppSettings) -> Unit,
    onShowGuide: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
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
            when (tab) {
                0 -> HomeScreen(
                entries = entries,
                settings = settings,
                onWrite = onWrite,
                onOpenEntry = onOpenEntry,
                onGoNew = onGoNew,
                onGoCheckIn = onGoCheckIn,
                onQuickCheckIn = { mood, tags, note ->
                    val quickEntry = Entry(
                        time = System.currentTimeMillis(),
                        mood = mood.key,
                        score = mood.score,
                        category = Category.DAILY.key,
                        tags = tags,
                        text = note
                    )
                    Store.add(scopeContext, quickEntry)
                },
                onQuickSave = onQuickSave,
                    onSettingsChange = onSettingsChange,
                    onCustomToggle = onCustomToggle,
                    onExport = onExport,
                    onImport = onImport,
                    onSettings = onOpenSettings
                )

                1 -> CalendarScreen(
                    entries = entries,
                    onOpenEntry = onOpenEntry,
                    onAddAt = onAddAt
                )

                2 -> NewScreen(
                    entries = entries,
                    onOpenEntry = onOpenEntry,
                    onQuickSave = onQuickSave,
                    onUpdateEntry = onUpdateEntry,
                    onAdd = onAddNew
                )

                3 -> CollectScreen(
                    entries = entries,
                    onOpenEntry = onOpenEntry,
                    onDelete = onDelete,
                    onExport = onExport,
                    onAdd = onAddDiary
                )

                else -> MineScreen(
                    settings = settings,
                    entries = entries,
                    onChange = onProfileChange,
                    onOpenSettings = onOpenSettings,
                    onShowGuide = onShowGuide,
                    onLogin = onLogin,
                    onLogout = onLogout,
                    onExport = onExport,
                    onImport = onImport
                )
            }
        }
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
