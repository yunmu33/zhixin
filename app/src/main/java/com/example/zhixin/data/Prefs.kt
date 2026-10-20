package com.example.zhixin.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val skin: String = "cream",            // 小屋皮肤
    val darkMode: String = "auto",         // auto / light / dark
    val fontScale: Float = 1.0f,           // 0.9 / 1.0 / 1.15
    val decor: Boolean = true,             // 云朵 / 星星装饰
    val listenOnly: Boolean = false,       // 只倾听模式
    val remindEnabled: Boolean = false,
    val remindHour: Int = 21,
    val remindMinute: Int = 0,
    /** 提醒怎么循环：daily 每天 / weekday 工作日 / weekend 周末 / weekly 每周指定那天。 */
    val remindRepeat: String = RemindRepeat.DAILY.key,
    /** weekly 时提醒周几（Calendar 的星期，2=周一）。 */
    val remindWeekday: Int = java.util.Calendar.MONDAY,
    /** 上一次真的弹出提醒是几号（yyyy-MM-dd），用来判断今天有没有提醒过。 */
    val lastRemindDateKey: String = "",
    val password: String = "",             // 空 = 无密码锁
    val tags: List<String> = DEFAULT_TAGS,
    val customItems: List<CustomItem> = DEFAULT_CUSTOM_ITEMS,
    val guideSeen: Boolean = false,
    // 本机账号（用户名 -> 密码哈希），accountUser 为空表示游客
    val accounts: Map<String, String> = emptyMap(),
    val accountUser: String = "",
    // 「我的」个人资料
    val nickname: String = "旅人",
    val avatar: String = "\uD83C\uDF31",
    // 自己上传的头像图片文件名（存在私有目录里）；为空就显示上面的表情头像
    val avatarFile: String = "",
    val motto: String = "每天都有新的收获",
    val aiLabel: String = "DeepSeek",
    val aiBaseUrl: String = "https://api.deepseek.com/v1",
    val aiKey: String = "",
    val aiModel: String = "deepseek-chat",
    // 示例任务是否已经收敛过（只做一次，之后不再动用户的列表）
    val samplesTrimmed: Boolean = false,
    /** 打卡后弹不弹窗让我写点什么（关掉就打完直接过）。 */
    val checkNotePrompt: Boolean = true,
    /** 已经点亮过的成就名，用来判断哪些是「刚解锁」的。 */
    val unlockedBadges: List<String> = emptyList(),
    /** 是否已经把当前成就记过一次底（老数据不该一次性弹一堆提示）。 */
    val badgesSeeded: Boolean = false,
    /** 四象限待办，按天存。 */
    val todos: List<TodoItem> = emptyList()
) {
    fun lockEnabled(): Boolean = password.isNotBlank()
    fun aiEnabled(): Boolean = aiKey.isNotBlank() && aiBaseUrl.isNotBlank()
}

/** 默认自带的标签。 */
val DEFAULT_TAGS = listOf("运动", "睡眠", "工作", "社交", "学习", "做饭")

/** 默认自带的示例打卡项（设置里可以增删）。 */
val DEFAULT_CUSTOM_ITEMS = listOf(
    CustomItem("\uD83D\uDCDA", "阅读"),
    CustomItem("\uD83C\uDFC3", "运动")
)

/** 老版本自带的示例项，升级后收敛成 DEFAULT_CUSTOM_ITEMS。 */
private val LEGACY_SAMPLE_ITEMS = listOf(
    CustomItem("\uD83D\uDCA7", "喝水"),
    CustomItem("\uD83E\uDDD8", "静心")
)

object Prefs {
    private const val FILE = "mood_house_prefs"
    private const val KEY = "settings"
    private val gson = Gson()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun load(context: Context) {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
        val loaded = if (raw.isNullOrBlank()) AppSettings()
        else try {
            gson.fromJson(raw, AppSettings::class.java)
        } catch (e: Exception) {
            AppSettings()
        }
        _settings.value = trimLegacySamples(context, loaded)
    }

    fun update(context: Context, value: AppSettings) {
        _settings.value = value
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, gson.toJson(value))
            .apply()
    }

    fun patch(context: Context, block: (AppSettings) -> AppSettings) {
        update(context, block(_settings.value))
    }
}

/**
 * 示例任务收敛成「阅读 + 运动」：只执行一次，
 * 而且只移除没被改过的旧示例，用户自己加的 / 改过的都不动。
 */
private fun trimLegacySamples(context: Context, s: AppSettings): AppSettings {
    if (s.samplesTrimmed) return s
    val kept = s.customItems.filterNot { it in LEGACY_SAMPLE_ITEMS }
    val trimmed = s.copy(customItems = kept, samplesTrimmed = true)
    Prefs.update(context, trimmed)
    return trimmed
}

/** 可选的 AI 接口预设（OpenAI 兼容）。 */
val AI_PRESETS = listOf(
    Triple("OpenAI 兼容", "https://api.openai.com/v1", "gpt-4o-mini"),
    Triple("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
    Triple("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
    Triple("Ollama 本地", "http://10.0.2.2:11434/v1", "qwen2.5:7b")
)
