package com.example.zhixin.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * 打卡配图：统一放在 app 私有目录 filesDir/images/ 下，
 * Entry.images 只存文件名，随 JSON 一起导出 / 导入。
 */
object ImageStore {
    private const val DIR_NAME = "images"

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /** 文件名 -> 实际文件。 */
    fun file(context: Context, name: String): File = File(dir(context), name)

    /**
     * 把相册选中的图片复制到私有目录，返回保存后的文件名；失败返回 null。
     * 直接复制字节流，不解码不重编码，避免大图占用过多内存。
     */
    fun saveFromUri(context: Context, uri: Uri): String? = try {
        val name = "img_${System.currentTimeMillis()}_${(0..99999).random()}.jpg"
        val out = File(dir(context), name)
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { src -> out.outputStream().use { dst -> src.copyTo(dst) } }
        if (out.length() <= 0L) null else name
    } catch (e: Exception) {
        null
    }

    fun delete(context: Context, name: String) {
        runCatching { file(context, name).delete() }
    }

    /** 采样解码一张图，避免大图直接进内存导致 OOM。 */
    fun loadBitmap(context: Context, name: String, maxSize: Int = 720): Bitmap? = try {
        val f = file(context, name)
        if (!f.exists()) null
        else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            val sample = calcSample(
                bounds.outWidth,
                bounds.outHeight,
                maxSize
            ).coerceAtLeast(1)
            BitmapFactory.decodeFile(
                f.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        }
    } catch (e: Exception) {
        null
    }

    private fun calcSample(w: Int, h: Int, max: Int): Int {
        if (w <= 0 || h <= 0) return 1
        var sample = 1
        while (w / sample > max && h / sample > max) sample *= 2
        return sample
    }
}
