package com.example.zhixin.notify

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.zhixin.data.AppSettings
import com.example.zhixin.data.RemindRepeat
import com.example.zhixin.data.WEEKDAY_LABELS
import com.example.zhixin.data.nextReminderTime
import com.example.zhixin.data.weekdayIndex
import java.util.Calendar

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
        ReminderReceiver.schedule(
            context,
            settings.remindHour,
            settings.remindMinute,
            settings.remindRepeat,
            settings.remindWeekday
        )
        // 系统里要是没排上（被省电策略 / 划后台清掉了），这里立刻补回来
        ReminderGuard.scheduleGuard(context)
        runCatching { ReminderGuard.check(context, catchUp = false) }
        // 没开「闹钟和提醒」权限时只能走不精确闹钟，提示一下
        if (!ReminderReceiver.hasExactPermission(context)) onNeedExactAlarm()
    } else {
        ReminderReceiver.cancel(context)
    }
}

/** 有没有拿到发通知的权限（Android 13 起要先给）。 */
fun hasNotifyPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * 系统省电有没有限制我们。
 * 国产 ROM（小米 / 华为 / OPPO 等）一旦限制后台，App 被划掉后闹钟会被清掉，
 * 提醒就再也不来了 —— 这是「关了 App 就没提醒」最常见的原因。
 */
fun isBatteryOptimized(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return !pm.isIgnoringBatteryOptimizations(context.packageName)
}

/** 让用户把小屋加入省电白名单；不给这个入口时就退到省电列表页。 */
fun openBatterySettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (direct.resolveActivity(context.packageManager) != null) {
        runCatching { context.startActivity(direct) }
        return
    }
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** 通知被关掉了（全局关掉，或这个提醒渠道被单独关掉）。 */
fun notificationsBlocked(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!nm.areNotificationsEnabled()) return true
        val channel = nm.getNotificationChannel(ReminderReceiver.CHANNEL)
        if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return true
    }
    return false
}

/** 去系统的通知设置页把提醒打开。 */
fun openNotificationSettings(context: Context) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}

/**
 * 下一次提醒是什么时候（按循环规则往后找，今天的时间已过就算到下一个该提醒的日子）。
 * 同一天显示「今天」，明天显示「明天」，再往后显示「周三」这样的星期。
 */
fun nextReminderText(
    hour: Int,
    minute: Int,
    repeatKey: String = RemindRepeat.DAILY.key,
    weekday: Int = Calendar.MONDAY
): String {
    val target = nextReminderTime(hour, minute, repeatKey, weekday)
    val now = Calendar.getInstance()
    val sameDay = target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = target.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
        target.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)
    val hm = String.format("%02d:%02d", hour, minute)
    return when {
        sameDay -> "今天 $hm"
        isTomorrow -> "明天 $hm"
        else -> "${WEEKDAY_LABELS[weekdayIndex(target.get(Calendar.DAY_OF_WEEK))]} $hm"
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
