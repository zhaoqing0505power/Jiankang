package com.jiankang.app.screentime

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Screen interactive time measures the bright interactive display, not device unlocks. */
data class ScreenEvent(val epochMs: Long, val interactive: Boolean)
data class ScreenUsage(val minutes: Int, val partial: Boolean)

object ScreenTimeMath {
    /** Clip observed interactive periods to [startMs, endMs], never invent missing usage. */
    fun estimate(startMs: Long, endMs: Long, events: List<ScreenEvent>): ScreenUsage? {
        if (endMs <= startMs) return null
        val sorted = events.filter { it.epochMs <= endMs }.sortedBy { it.epochMs }
        if (sorted.isEmpty()) return null
        var known = false
        var on = false
        var last = startMs
        var elapsed = 0L
        var partial = false
        for (event in sorted) {
            if (event.epochMs < startMs) {
                on = event.interactive
                known = true
                continue
            }
            if (!known) {
                known = true
                partial = true
                on = event.interactive
                last = event.epochMs
                continue
            }
            if (on && event.epochMs >= last) elapsed += event.epochMs - last
            on = event.interactive
            last = event.epochMs
        }
        if (known && on && endMs >= last) elapsed += endMs - last
        if (!known) return null
        return ScreenUsage((elapsed / 60000.0).roundToInt().coerceAtLeast(0), partial)
    }
}

object ScreenTimeReader {
    fun supported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun hasUsageAccess(context: Context): Boolean {
        if (!supported()) return false
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (_: RuntimeException) { false }
    }

    fun read(context: Context, date: LocalDate): ScreenUsage? {
        if (!supported() || !hasUsageAccess(context)) return null
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), System.currentTimeMillis())
        if (end <= start) return null
        return try {
            val mgr = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = mgr.queryEvents(
                date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), end
            ) ?: return null
            val event = UsageEvents.Event()
            val observations = mutableListOf<ScreenEvent>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.SCREEN_INTERACTIVE -> observations.add(ScreenEvent(event.timeStamp, true))
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> observations.add(ScreenEvent(event.timeStamp, false))
                }
            }
            ScreenTimeMath.estimate(start, end, observations)
        } catch (_: SecurityException) { null } catch (_: RuntimeException) { null }
    }
}
