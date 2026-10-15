package com.example.zhixin.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.zhixin.data.AppSettings

/**
 * 统一的提醒开关：开启就排闹钟（缺通知权限时回调请求），关闭就取消。
 */
fun applyReminder(
    context: Context,
    settings: AppSettings,
    onRequestPermission: () -> Unit = {}
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        onRequestPermission()
    }
    if (settings.remindEnabled) {
        ReminderReceiver.schedule(context, settings.remindHour, settings.remindMinute)
    } else {
        ReminderReceiver.cancel(context)
    }
}
