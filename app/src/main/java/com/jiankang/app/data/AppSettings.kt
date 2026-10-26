package com.jiankang.app.data

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("jiankang_settings", Context.MODE_PRIVATE)

    var sleepGoalHours: Int
        get() = prefs.getInt("sleep_goal", 8)
        set(value) { prefs.edit().putInt("sleep_goal", value.coerceIn(1, 16)).apply() }

    var weeklyExerciseGoal: Int
        get() = prefs.getInt("exercise_goal", 150)
        set(value) { prefs.edit().putInt("exercise_goal", value.coerceIn(1, 1000)).apply() }

    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_enabled", false)
        set(value) { prefs.edit().putBoolean("reminder_enabled", value).apply() }

    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)
        set(value) { prefs.edit().putInt("reminder_hour", value.coerceIn(0, 23)).apply() }

    var reminderMinute: Int
        get() = prefs.getInt("reminder_minute", 0)
        set(value) { prefs.edit().putInt("reminder_minute", value.coerceIn(0, 59)).apply() }

    /** All workday break alerts are opt-in. Local wall-clock time, [start,end). */
    var breakReminderEnabled: Boolean
        get() = prefs.getBoolean("break_reminders_enabled", false)
        set(value) { prefs.edit().putBoolean("break_reminders_enabled", value).apply() }

    var breakStartMinutes: Int
        get() = prefs.getInt("break_start_minutes", 8 * 60)
        set(value) { prefs.edit().putInt("break_start_minutes", value.coerceIn(0, 1439)).apply() }

    var breakEndMinutes: Int
        get() = prefs.getInt("break_end_minutes", 17 * 60)
        set(value) { prefs.edit().putInt("break_end_minutes", value.coerceIn(0, 1439)).apply() }

    /** Total interactive screen-time goal, in minutes, default five hours. */
    var screenGoalMinutes: Int
        get() = prefs.getInt("screen_goal_minutes", 300)
        set(value) { prefs.edit().putInt("screen_goal_minutes", value.coerceIn(30, 1440)).apply() }

    var screenAlertEnabled: Boolean
        get() = prefs.getBoolean("screen_alert", false)
        set(value) { prefs.edit().putBoolean("screen_alert", value).apply() }

    /** remind = notifications only; focus = reminders + system Digital Wellbeing guide. */
    var screenLimitMode: String
        get() = prefs.getString("screen_mode", "focus") ?: "focus"
        set(value) {
            require(value == "remind" || value == "focus")
            prefs.edit().putString("screen_mode", value).apply()
        }

    /** Return true just once for each warning tier on each calendar date. */
    fun markScreenAlertOnce(date: String, threshold: Int): Boolean {
        require(threshold == 80 || threshold == 100)
        val key = "screen_warn_" + threshold
        if (prefs.getString(key, null) == date) return false
        return prefs.edit().putString(key, date).commit()
    }

    fun favorites(): Set<String> = prefs.getStringSet("favorites", emptySet())?.toSet().orEmpty()

    fun toggleFavorite(name: String) {
        val favorites = favorites().toMutableSet()
        if (!favorites.add(name)) favorites.remove(name)
        prefs.edit().putStringSet("favorites", favorites).apply()
    }
}
