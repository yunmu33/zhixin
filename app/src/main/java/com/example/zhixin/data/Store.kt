package com.example.zhixin.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 全部数据以 JSON 存在私有目录，不上传任何云端。
 */
object Store {
    private const val FILE_NAME = "mood_house.json"
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _all = MutableStateFlow<List<Entry>>(emptyList())
    val all: StateFlow<List<Entry>> = _all.asStateFlow()

    fun load(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        val list: List<Entry> = try {
            if (!file.exists()) emptyList()
            else {
                val type = object : TypeToken<List<Entry>>() {}.type
                (gson.fromJson<List<Entry>>(file.readText(), type) ?: emptyList())
                    .map { it.normalize() }
            }
        } catch (e: Exception) {
            emptyList()
        }
        _all.value = list
    }

    private fun persist(context: Context) {
        val snapshot = _all.value
        scope.launch {
            runCatching {
                File(context.filesDir, FILE_NAME).writeText(gson.toJson(snapshot))
            }
        }
    }

    /** 未删除的记录，按时间倒序。 */
    fun live(): List<Entry> = _all.value.filter { !it.deleted }.sortedByDescending { it.time }

    fun trash(): List<Entry> = _all.value.filter { it.deleted }.sortedByDescending { it.time }

    fun byId(id: String): Entry? = _all.value.firstOrNull { it.id == id }

    fun add(context: Context, entry: Entry) {
        _all.value = _all.value + entry.normalize()
        persist(context)
    }

    fun update(context: Context, entry: Entry) {
        _all.value = _all.value.map { if (it.id == entry.id) entry.normalize() else it }
        persist(context)
    }

    fun softDelete(context: Context, id: String) {
        _all.value = _all.value.map { if (it.id == id) it.copy(deleted = true) else it }
        persist(context)
    }

    fun restore(context: Context, id: String) {
        _all.value = _all.value.map { if (it.id == id) it.copy(deleted = false) else it }
        persist(context)
    }

    fun emptyTrash(context: Context) {
        _all.value = _all.value.filter { !it.deleted }
        persist(context)
    }

    fun clearAll(context: Context) {
        _all.value = emptyList()
        persist(context)
    }

    fun replaceAll(context: Context, list: List<Entry>) {
        _all.value = list.map { it.normalize() }
        persist(context)
    }

    /** 同步写出一次，导出前调用，确保拿到最新数据。 */
    fun flush(context: Context) {
        runBlocking {
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                runCatching {
                    File(context.filesDir, FILE_NAME).writeText(gson.toJson(_all.value))
                }
            }
        }
    }

    fun toJson(): String = gson.toJson(_all.value)

    fun fromJson(json: String): List<Entry> = try {
        val type = object : TypeToken<List<Entry>>() {}.type
        (gson.fromJson<List<Entry>>(json, type) ?: emptyList()).map { it.normalize() }
    } catch (e: Exception) {
        emptyList()
    }

    // ---------- 统计 ----------

    /** dateKey -> 当天分数最高的那条（决定色块颜色）。 */
    fun dayMap(entries: List<Entry>): Map<String, Entry> =
        entries.filter { !it.deleted }
            .groupBy { it.dateKey }
            .mapValues { (_, list) -> list.maxByOrNull { it.score } ?: list.first() }

    /** 连续打卡天数（今天没打卡则从昨天开始算）。 */
    fun streak(entries: List<Entry>): Int {
        val keys = entries.filter { !it.deleted }.map { it.dateKey }.toSet()
        if (keys.isEmpty()) return 0
        var cursor = System.currentTimeMillis()
        if (DateKey.fromMillis(cursor) !in keys) cursor = DateKey.addDays(cursor, -1)
        var count = 0
        while (DateKey.fromMillis(cursor) in keys) {
            count++
            cursor = DateKey.addDays(cursor, -1)
        }
        return count
    }

    /** 近 n 天每天的平均情绪分，缺勤为 null。 */
    fun recentScores(entries: List<Entry>, days: Int = 30): List<Float?> {
        val map = entries.filter { !it.deleted }.groupBy { it.dateKey }
        val today = System.currentTimeMillis()
        return (days - 1 downTo 0).map { offset ->
            val millis = DateKey.addDays(today, -offset)
            val list = map[DateKey.fromMillis(millis)]
            if (list.isNullOrEmpty()) null else list.map { it.score.toFloat() }.average().toFloat()
        }
    }
}
