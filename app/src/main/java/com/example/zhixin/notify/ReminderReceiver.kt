package com.example.zhixin.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.zhixin.MainActivity
import com.example.zhixin.R
import com.example.zhixin.data.Prefs
import java.util.Calendar

/** 每日记录提醒：到点弹一条温柔的通知。 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(TAG, "onReceive")
        ensureChannel(context)

        // 先排好明天的：闹钟链不能断，就算这次通知被系统拦了，明天照常来
        Prefs.load(context)
        val settings = Prefs.settings.value
        if (settings.remindEnabled) {
            schedule(context, settings.remindHour, settings.remindMinute)
        }

        // Android 13 起没有通知权限就不能弹，安静退出
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        notifyNow(context)
    }

    companion object {
        private const val TAG = "ZhixinReminder"
        const val CHANNEL = "mood_house_reminder"
        private const val NOTIFY_ID = 20261014
        private const val REQ = 0

        /** 排下一次提醒（到点后由 Receiver 自己续排，所以每天都会来）。 */
        fun schedule(context: Context, hour: Int, minute: Int) {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pendingIntent(context)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            // 能精确就精确，不给精确权限时退回十分钟窗口，至少保证一定会弹
            if (canExact(alarm)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                } else {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                }
            } else {
                alarm.setWindow(AlarmManager.RTC_WAKEUP, cal.timeInMillis, 10 * 60 * 1000L, pi)
            }
        }

        fun cancel(context: Context) {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.cancel(pendingIntent(context))
        }

        /** 是否已经排上（设置页用来提示）。 */
        fun isScheduled(context: Context): Boolean =
            pendingIntentQuery(context) != null

        /** 立刻弹一条，用来验证提醒有没有生效。 */
        fun notifyNow(context: Context) {
            ensureChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
            if (!areNotificationsEnabled(context)) return
            val tap = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("\uD83C\uDFE1 该来小屋坐坐啦")
                .setContentText("今天的心情，点一下贴纸就记好了～")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(tap)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, notification)
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "小屋提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "每天来小屋坐坐"
                    }
                )
            }
        }

        /** 系统是否允许我们用精确闹钟（Android 12 起要在设置里开）。 */
        fun hasExactPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarm.canScheduleExactAlarms()
        }

        private fun canExact(alarm: AlarmManager): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarm.canScheduleExactAlarms()
            } else true

        private fun areNotificationsEnabled(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return nm.areNotificationsEnabled()
        }

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ReminderReceiver::class.java)
            return PendingIntent.getBroadcast(
                context, REQ, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun pendingIntentQuery(context: Context): PendingIntent? {
            val intent = Intent(context, ReminderReceiver::class.java)
            val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, REQ, intent, flags)
        }
    }
}
