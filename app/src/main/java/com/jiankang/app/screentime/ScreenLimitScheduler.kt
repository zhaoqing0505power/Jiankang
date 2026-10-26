package com.jiankang.app.screentime

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.jiankang.app.MainActivity
import com.jiankang.app.R
import com.jiankang.app.data.AppSettings
import java.time.LocalDate

/**
 * Best-effort, inexact half-hourly checks. Never locks the phone or blocks emergency calls.
 */
object ScreenLimitScheduler {
    private const val REQUEST_CODE = 5702
    private const val CHANNEL = "screen_usage"
    fun schedule(context: Context, settings: AppSettings) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, Intent(context, ScreenLimitReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.cancel(pending)
        if (!settings.screenAlertEnabled || !ScreenTimeReader.supported()) return
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR,
            AlarmManager.INTERVAL_HALF_HOUR,
            pending
        )
    }

    fun maybeNotify(context: Context, settings: AppSettings = AppSettings(context)) {
        if (!settings.screenAlertEnabled || !ScreenTimeReader.hasUsageAccess(context)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val usage = ScreenTimeReader.read(context, LocalDate.now()) ?: return
        val goal = settings.screenGoalMinutes
        val tier = when {
            usage.minutes >= goal -> 100
            usage.minutes >= (goal * 0.8).toInt() -> 80
            else -> return
        }
        val today = LocalDate.now().toString()
        val prefs = context.getSharedPreferences("screen_notification_state", Context.MODE_PRIVATE)
        val key = "last_notified_" + tier
        if (prefs.getString(key, null) == today) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "屏幕时间提醒", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val message = if (tier == 100) {
            "今日亮屏约" + usage.minutes + "分钟，已达" + goal + "分钟目标。可以休息一下。"
        } else {
            "今日亮屏约" + usage.minutes + "分钟，接近" + goal + "分钟目标。"
        }
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle(if (tier == 100) "今日屏幕时间已达上限" else "屏幕时间接近上限")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(500 + tier, notification)
        prefs.edit().putString(key, today).apply()
    }
}

class ScreenLimitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ScreenLimitScheduler.maybeNotify(context.applicationContext)
    }
}
