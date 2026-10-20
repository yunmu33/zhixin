package com.example.zhixin.notify

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import com.example.zhixin.data.Prefs
import com.example.zhixin.data.RemindRepeat
import com.example.zhixin.data.matches
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 提醒的「保命」机制。
 *
 * 光靠闹钟有一个前提：系统别把它清掉。手机上一旦点了「强制停止」，
 * 或者部分国产 ROM 在 App 被划出后台时把闹钟一并收走，到点就再也不来了。
 * 这里挂一个周期性的自检任务，每隔一段时间看一眼：
 *   1. 系统里已经没有我们的闹钟了 → 重新排上；
 *   2. 今天该提醒的点已经过了、但今天还没提醒过 → 补一条通知（会晚一点，但不会缺席）。
 */
object ReminderGuard {

    private const val JOB_ID = 20261020
    private const val PERIOD = 15 * 60 * 1000L

    /** 挂上周期自检任务（已经挂着就不重复挂）。重启后由 BootReceiver 再挂一次。 */
    fun scheduleGuard(context: Context) {
        val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (js.getPendingJob(JOB_ID) != null) return
        val info = JobInfo.Builder(JOB_ID, ComponentName(context, ReminderJobService::class.java))
            .setPeriodic(PERIOD)
            // 重启后自动恢复
            .setPersisted(true)
            .build()
        runCatching { js.schedule(info) }
    }

    /**
     * 自检一遍。
     * @param catchUp 是否允许「补提醒」。App 正在前台时不用打扰用户，交给后台任务去做。
     * @return 这次是不是真的补了一条通知。
     */
    fun check(context: Context, catchUp: Boolean = true): Boolean {
        val settings = runCatching { Prefs.load(context); Prefs.settings.value }.getOrNull()
            ?: return false
        if (!settings.remindEnabled) return false
        if (!hasNotifyPermission(context)) return false

        // 闹钟被系统收走了就重新排：这是「划出后台就没提醒」最主要的补救
        if (!ReminderReceiver.isScheduled(context)) {
            runCatching {
                ReminderReceiver.schedule(
                    context,
                    settings.remindHour,
                    settings.remindMinute,
                    settings.remindRepeat,
                    settings.remindWeekday
                )
            }
        }

        if (!catchUp) return false

        val now = Calendar.getInstance()
        // 今天本来就该提醒吗（比如选了「工作日」，周末就不补）
        if (!RemindRepeat.of(settings.remindRepeat).matches(now, settings.remindWeekday)) return false

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.remindHour)
            set(Calendar.MINUTE, settings.remindMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = todayKey(now)
        val missed = now.timeInMillis >= target.timeInMillis && settings.lastRemindDateKey != today
        if (!missed) return false

        ReminderReceiver.notifyNow(context)
        Prefs.patch(context) { it.copy(lastRemindDateKey = today) }
        return true
    }

    /** 标记今天已经提醒过（闹钟正常到点时由 Receiver 调用，免得自检又补一条）。 */
    fun markNotified(context: Context) {
        Prefs.patch(context) { it.copy(lastRemindDateKey = todayKey(Calendar.getInstance())) }
    }

    private fun todayKey(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time)
}
