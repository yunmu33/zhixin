package com.example.zhixin.ai

import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.Mood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 整理记录：填了 Key 就走 OpenAI 兼容接口，没填就用小屋模板兜底。
 */
object AiClient {

    suspend fun summarize(
        text: String,
        moodKey: String?,
        listenOnly: Boolean,
        settings: AppSettings
    ): String = withContext(Dispatchers.IO) {
        if (settings.aiEnabled()) {
            runCatching { call(text, moodKey, listenOnly, settings) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: LocalTemplate.summarize(text, moodKey, listenOnly)
        } else {
            LocalTemplate.summarize(text, moodKey, listenOnly)
        }
    }

    suspend fun testConnection(settings: AppSettings): String = withContext(Dispatchers.IO) {
        if (!settings.aiEnabled()) return@withContext "还没有填写 API Key"
        runCatching {
            val url = URL(settings.aiBaseUrl.trimEnd('/') + "/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${settings.aiKey}")
                doOutput = true
            }
            val body = JSONObject().apply {
                put("model", settings.aiModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "回复两个字：好的")
                    })
                })
                put("max_tokens", 16)
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code in 200..299) "连接成功 ✅" else "接口返回 $code：${text.take(160)}"
        }.getOrElse { "连接失败：${it.message}" }
    }

    private fun call(
        text: String,
        moodKey: String?,
        listenOnly: Boolean,
        settings: AppSettings
    ): String {
        val mood = Mood.of(moodKey)
        val system = buildString {
            append("你是「情绪小屋」里温柔的整理助手，用中文回复，语气像午后阳光下的日记本，轻声、不评判。")
            if (listenOnly) append("现在开启「只倾听模式」：只温柔接住用户的话，做简短复述和陪伴，不做分析、不给建议、不换视角。")
            else append("请输出四段：📌 一句话小结 / 💛 小屋想说 / 🌱 可以试试 / ✨ 明天的小期待。每段 1-3 句。")
            append("不要使用 Markdown 标题语法，每段以 emoji 开头换行即可。总长度控制在 260 字以内。")
        }
        val user = buildString {
            mood?.let { append("今天的心情：${it.emoji} ${it.label}\n") }
            append(text)
        }

        val conn = (URL(settings.aiBaseUrl.trimEnd('/') + "/chat/completions").openConnection()
                as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${settings.aiKey}")
            doOutput = true
        }
        val body = JSONObject().apply {
            put("model", settings.aiModel)
            put("temperature", 0.8)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", system) })
                put(JSONObject().apply { put("role", "user"); put("content", user) })
            })
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val raw = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) error("HTTP $code")
        return JSONObject(raw)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    /** 没填 Key 时的本地兜底模板。 */
    object LocalTemplate {
        private val KEYWORDS = listOf(
            listOf("加班", "工作", "deadline", "项目") to "工作把你的电量抽走了一些",
            listOf("睡", "失眠", "熬夜") to "身体在悄悄提醒你，该好好歇一歇了",
            listOf("考试", "成绩", "复习") to "你把很多力气花在了「要做得好」这件事上",
            listOf("吵架", "生气", "委屈", "不理解") to "你在意这段关系，所以才会这么难受",
            listOf("孤独", "一个人", "没人") to "想被看见、被接住，是很正常的需要",
            listOf("累", "疲惫", "好累") to "今天你已经做得够多了",
            listOf("开心", "顺利", "喜欢", "谢谢") to "生活里有一小块糖，被你接住了",
            listOf("焦虑", "紧张", "担心") to "你在为还没发生的事提前用力"
        )

        fun summarize(text: String, moodKey: String?, listenOnly: Boolean): String {
            val mood = Mood.of(moodKey)
            val t = text.trim()
            val hit = KEYWORDS.firstOrNull { (keys, _) -> keys.any { t.contains(it) } }?.second
            val core = hit ?: when (mood) {
                Mood.JOY -> "今天有光落在你身上，值得被记下来"
                Mood.CALM -> "今天稳稳的，这种平静也很珍贵"
                Mood.DOWN -> "今天心里像下了一场小雨"
                Mood.ANGRY -> "有些火气堵在胸口，它其实是在替你说话"
                Mood.SLEEPY -> "今天电量偏低，想躲进被窝里"
                null -> "你把自己的一小段时光交给了小屋"
            }
            val first = t.lineSequence().firstOrNull { it.isNotBlank() }?.take(46) ?: "（今天只想贴个贴纸）"

            return buildString {
                appendLine("📌 一句话小结")
                appendLine("${core}。你说：「${first}${if (t.length > 46) "…" else ""}」")
                appendLine()
                appendLine("💛 小屋想说")
                appendLine(
                    when (mood) {
                        Mood.JOY -> "这样的小确幸，值得被反复回看。先让它在这里躺一会儿吧。"
                        Mood.DOWN, Mood.ANGRY -> "你已经撑过来了，情绪不需要马上变好，被写下来就已经轻了一点点。"
                        Mood.SLEEPY -> "困意也是身体的语言，别责怪自己不够有精神。"
                        else -> "小屋听到了，也接住了。你不需要立刻振作。"
                    }
                )
                if (!listenOnly) {
                    appendLine()
                    appendLine("🌱 可以试试")
                    appendLine(
                        when (mood) {
                            Mood.JOY -> "把这份心情拍张照、或写一句给未来的自己。"
                            Mood.DOWN, Mood.ANGRY -> "喝口温水，把肩膀松开三次；如果还想说，就继续写下去。"
                            Mood.SLEEPY -> "比平时早睡 20 分钟，把灯调暗一点。"
                            else -> "给自己五分钟，什么都不做，只是呼吸。"
                        }
                    )
                    appendLine()
                    appendLine("✨ 明天的小期待")
                    appendLine("明天给自己留一件很小、只属于自己的事。")
                }
            }.trim()
        }
    }
}
