package com.example.zhixin.notify

import android.app.job.JobParameters
import android.app.job.JobService

/**
 * 提醒的后台自检：被系统叫醒时看一眼闹钟还在不在。
 * 被划出后台、被厂商 ROM 清掉闹钟的情况下，靠它把提醒补回来。
 */
class ReminderJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        // 干的都是同步的轻量活儿（读个设置、必要时排个闹钟 / 弹条通知）
        runCatching { ReminderGuard.check(this, catchUp = true) }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
