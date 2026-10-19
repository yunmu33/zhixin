package com.example.zhixin.data

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File

/**
 * 日记里的语音：统一放在 app 私有目录 filesDir/audio/ 下，
 * Entry.audio 只存文件名，和图片一样的做法。
 */
object AudioStore {
    private const val DIR_NAME = "audio"

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /** 文件名 -> 实际文件。 */
    fun file(context: Context, name: String): File = File(dir(context), name)

    /** 生成一个新的语音文件名。 */
    fun nextName(): String = "aud_${System.currentTimeMillis()}_${(0..99999).random()}.m4a"

    /** 删掉不再需要的语音。 */
    fun delete(context: Context, name: String) {
        runCatching { file(context, name).delete() }
    }

    /** 读出这条语音有多长（毫秒）；读不出来就返回 0。 */
    fun durationMillis(context: Context, name: String): Long = try {
        val f = file(context, name)
        if (!f.exists()) {
            0L
        } else {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(f.absolutePath)
            val value = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()
            value
        }
    } catch (e: Exception) {
        0L
    }
}
