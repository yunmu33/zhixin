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
    val password: String = "",             // 空 = 无密码锁
    val tags: List<String> = listOf("运动", "睡眠", "工作", "社交", "学习", "做饭"),
    val customItems: List<CustomItem> = DEFAULT_CUSTOM_ITEMS,
    val guideSeen: Boolean = false,
    // 本机账号（用户名 -> 密码哈希），accountUser 为空表示游客
    val accounts: Map<String, String> = emptyMap(),
    val accountUser: String = "",
    // 「我的」个人资料
    val nickname: String = "旅人",
    val avatar: String = "\uD83C\uDF31",
    val motto: String = "每天都有新的收获",
    val aiLabel: String = "DeepSeek",
    val aiBaseUrl: String = "https://api.deepseek.com/v1",
    val aiKey: String = "",
    val aiModel: String = "deepseek-chat"
) {
    fun lockEnabled(): Boolean = password.isNotBlank()
    fun aiEnabled(): Boolean = aiKey.isNotBlank() && aiBaseUrl.isNotBlank()
}

/** 默认的自定义打卡项。 */
val DEFAULT_CUSTOM_ITEMS = listOf(
    CustomItem("\uD83D\uDCA7", "喝水"),
    CustomItem("\uD83D\uDCDA", "阅读"),
    CustomItem("🏃", "运动"),
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
        _settings.value = if (raw.isNullOrBlank()) AppSettings()
        else try {
            gson.fromJson(raw, AppSettings::class.java)
        } catch (e: Exception) {
            AppSettings()
        }
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

/** 可选的 AI 接口预设（OpenAI 兼容）。 */
val AI_PRESETS = listOf(
    Triple("OpenAI 兼容", "https://api.openai.com/v1", "gpt-4o-mini"),
    Triple("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
    Triple("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
    Triple("Ollama 本地", "http://10.0.2.2:11434/v1", "qwen2.5:7b")
)
