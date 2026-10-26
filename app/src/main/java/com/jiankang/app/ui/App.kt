package com.jiankang.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jiankang.app.data.AppSettings
import com.jiankang.app.data.HealthDatabase
import com.jiankang.app.model.MealType
import java.time.LocalDate

enum class Page(val isTab: Boolean = false) {
    HOME(true), RECORD(true), STATS(true), PROFILE(true),
    BODY, BREAKFAST, LUNCH, DINNER, SLEEP, EXERCISE, DIET,
    GOALS, FAVORITES, SPORTS, REMINDERS, BACKUP, ABOUT
}

@Composable
fun HealthApp() {
    val context = LocalContext.current
    val db = remember { HealthDatabase(context.applicationContext) }
    val settings = remember { AppSettings(context.applicationContext) }
    var page by remember { mutableStateOf(Page.HOME) }
    val history = remember { mutableStateListOf<Page>() }
    var dayString by remember { mutableStateOf(LocalDate.now().toString()) }
    var revision by remember { mutableIntStateOf(0) }
    val date = LocalDate.parse(dayString)
    val day = remember(dayString, revision) { db.day(dayString) }
    val meals = remember(dayString, revision) { db.meals(dayString) }

    val navigate: (Page) -> Unit = { dest ->
        if (page != dest) {
            if (dest.isTab) history.clear() else history.add(page)
            page = dest
        }
    }
    val back: () -> Unit = {
        page = if (history.isNotEmpty()) history.removeAt(history.lastIndex) else Page.HOME
    }
    val chooseDate: () -> Unit = {
        openDatePicker(context, date) { dayString = it.toString() }
    }
    val savedDay: (com.jiankang.app.model.DayRecord) -> Unit = { new ->
        db.saveDay(new)
        revision++
        showMessage(context, "保存成功")
        back()
    }
    val savedMeal: () -> Unit = {
        revision++
        showMessage(context, "保存成功")
        back()
    }
    val settingsUpdated: () -> Unit = { revision++ }

    BackHandler(enabled = !page.isTab) { back() }

    Scaffold(
        containerColor = Pale,
        bottomBar = {
            if (page.isTab) JournalBottomBar(page, navigate)
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                Page.HOME -> HomeScreen(date, day, meals, settings.sleepGoalHours, chooseDate, navigate)
                Page.RECORD -> RecordScreen(date, day, meals, chooseDate, navigate)
                Page.BODY -> BodyScreen(date, day, back, savedDay)
                Page.BREAKFAST -> MealScreen(date, MealType.BREAKFAST, db, settings, back, savedMeal)
                Page.LUNCH -> MealScreen(date, MealType.LUNCH, db, settings, back, savedMeal)
                Page.DINNER -> MealScreen(date, MealType.DINNER, db, settings, back, savedMeal)
                Page.SLEEP -> SleepScreen(date, day, settings.sleepGoalHours, back, savedDay)
                Page.EXERCISE -> ExerciseScreen(date, day, back, savedDay)
                Page.STATS -> StatsScreen(date, db, revision)
                Page.DIET -> DietScreen(date, meals, back)
                Page.PROFILE -> ProfileScreen(date, settings, navigate)
                Page.GOALS -> GoalsScreen(settings, back) { revision++; back() }
                Page.FAVORITES -> FavoritesScreen(db, settings, back, navigate)
                Page.SPORTS -> SportsScreen(back)
                Page.REMINDERS -> RemindersScreen(settings, back, settingsUpdated)
                Page.BACKUP -> BackupScreen(db, settings, back) { revision++; back() }
                Page.ABOUT -> AboutScreen(back)
            }
        }
    }
}
