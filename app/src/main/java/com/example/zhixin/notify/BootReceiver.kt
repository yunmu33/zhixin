package com.example.zhixin.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.zhixin.data.Prefs

/**
 * 开机 / 重装 App 之后把提醒重新排上。
 * AlarmManager 的闹钟重启就没了，不重新排的话提醒会悄悄消失。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) return

        Prefs.load(context)
        val s = Prefs.settings.value
        if (s.remindEnabled) {
            ReminderReceiver.schedule(context, s.remindHour, s.remindMinute)
        }
    }
}
