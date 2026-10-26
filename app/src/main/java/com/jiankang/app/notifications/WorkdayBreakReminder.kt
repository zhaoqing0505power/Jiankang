package com.jiankang.app.notifications

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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.jiankang.app.MainActivity
import com.jiankang.app.R
import com.jiankang.app.data.AppSettings
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Weekly rhythm: Monday-Friday. First vibration at start+60min; end boundary excluded. */
object WorkdayBreakTimes {
    fun validWindow(start: LocalTime, end: LocalTime): Boolean =
        start.isBefore(end) && ChronoUnit.MINUTES.between(start, end) > 60

    fun next(after: ZonedDateTime, start: LocalTime, end: LocalTime): ZonedDateTime {
        require(validWindow(start, end)) { "工作时段至少需要超过1小时" }
        for (offset in 0L..7L) {
            val date = after.toLocalDate().plusDays(offset)
            if (date.dayOfWeek !in DayOfWeek.MONDAY..DayOfWeek.FRIDAY) continue
            var whenToRemind = date.atTime(start).atZone(after.zone).plusHours(1)
            val deadline = date.atTime(end).atZone(after.zone)
            while (whenToRemind.isBefore(deadline)) {
                if (whenToRemind.isAfter(after)) return whenToRemind
                whenToRemind = whenToRemind.plusHours(1)
            }
        }
        error("未来7天未找到工作日")
    }
}

object WorkdayBreakReminder {
    private const val REQUEST_CODE = 4141
    private const val CHANNEL = "workday_movement"
    const val DEFAULT_START = 8 * 60
    const val DEFAULT_END = 17 * 60
    private const val EXTRA_DUE = "due_at_ms"

    private fun time(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)

    fun schedule(context: Context, settings: AppSettings) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, Intent(context, WorkdayBreakReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pending != null) alarm.cancel(pending)
        if (!settings.breakReminderEnabled) return
        val next = WorkdayBreakTimes.next(
            ZonedDateTime.now(), time(settings.breakStartMinutes), time(settings.breakEndMinutes)
        )
        val at = next.toInstant().toEpochMilli()
        val intent = Intent(context, WorkdayBreakReceiver::class.java).putExtra(EXTRA_DUE, at)
        val newPending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Not exact: avoids the special exact-alarm permission and reduces wake-up pressure.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, newPending)
    }

    fun deliver(context: Context, intent: Intent?) {
        val settings = AppSettings(context)
        if (!settings.breakReminderEnabled) return
        val due = intent?.getLongExtra(EXTRA_DUE, -1L) ?: -1L
        val now = System.currentTimeMillis()
        val expectedStart = due > 0L && now >= due && now - due <= 30 * 60 * 1000L
        val here = ZonedDateTime.now()
        val mondayToFriday = here.dayOfWeek.value in 1..5
        val inWindow = here.toLocalTime().isAfter(time(settings.breakStartMinutes)) &&
            here.toLocalTime().isBefore(time(settings.breakEndMinutes))
        try {
            if (expectedStart && mondayToFriday && inWindow) {
                vibrate(context)
                notifyUser(context)
            }
        } finally {
            // Always schedule the next valid weekday alarm, even if this one was delayed.
            schedule(context, settings)
        }
    }

    private fun vibrate(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            }
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0L, 230L, 120L, 230L), -1
                ))
            }
        } catch (_: Exception) {
            // Some hardware has no vibrator or restricts background vibration.
        }
    }

    private fun notifyUser(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "工作日久坐提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(false) // Vibrate only once via Vibrator, not notification as well.
                setSound(null, null)
            }
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            4141, Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_app)
                .setContentTitle("起来活动一下")
                .setContentText("工作间隙站起来走几步、活动肩颈和腿部。")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
    }
}

class WorkdayBreakReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WorkdayBreakReminder.deliver(context.applicationContext, intent)
    }
}
