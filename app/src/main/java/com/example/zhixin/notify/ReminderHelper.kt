package com.example.zhixin.notify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.zhixin.data.AppSettings

/**
 * 统一的提醒开关：开启就排闹钟（缺权限时回调请求），关闭就取消。
 */
fun applyReminder(
    context: Context,
    settings: AppSettings,
    onRequestPermission: () -> Unit = {},
    onNeedExactAlarm: () -> Unit = {}
) {
    // Android 13 起要先拿到通知权限，否则通知根本出不来
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        onRequestPermission()
    }
    if (settings.remindEnabled) {
        ReminderReceiver.schedule(context, settings.remindHour, settings.remindMinute)
        // 没开「闹钟和提醒」权限时只能走不精确闹钟，提示一下
        if (!ReminderReceiver.hasExactPermission(context)) onNeedExactAlarm()
    } else {
        ReminderReceiver.cancel(context)
    }
}

/** 跳到系统的「闹钟和提醒」权限页，让用户给精确闹钟授权。 */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
