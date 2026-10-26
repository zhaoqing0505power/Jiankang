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

    fun favorites(): Set<String> = prefs.getStringSet("favorites", emptySet())?.toSet().orEmpty()

    fun toggleFavorite(name: String) {
        val favorites = favorites().toMutableSet()
        if (!favorites.add(name)) favorites.remove(name)
        prefs.edit().putStringSet("favorites", favorites).apply()
    }
}
