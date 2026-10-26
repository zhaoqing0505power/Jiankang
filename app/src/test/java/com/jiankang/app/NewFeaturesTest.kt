package com.jiankang.app

import com.jiankang.app.model.FoodCatalog
import com.jiankang.app.model.MealType
import com.jiankang.app.model.DayRecord
import org.junit.Assert.*
import org.junit.Test

class NewFeaturesTest {
    @Test fun allChineseBreakfastOptionsAreAvailable() {
        assertEquals(
            listOf("小米粥", "牛奶", "鸡蛋灌饼", "小笼包", "馅饼"),
            FoodCatalog.localBreakfast().map { it.name }
        )
        assertTrue(FoodCatalog.localBreakfast().all { it.defaultGrams > 0 && it.per100Kcal > 0 })
        assertEquals(5, FoodCatalog.localBreakfast().map { it.toEntry("2026-10-26", MealType.BREAKFAST) }.size)
    }

    @Test fun reflectionIsImmutableAndCanBeCleared() {
        val day = DayRecord(date = "2026-10-26", heightCm = 168.0)
        val written = day.copy(reflection = "今天运动后心情很好", mood = "平静")
        assertNull(day.reflection)
        assertEquals("今天运动后心情很好", written.reflection)
        assertEquals(168.0, written.heightCm!!, 0.001)
        val cleared = written.copy(reflection = null, mood = null)
        assertNull(cleared.reflection)
        assertNull(cleared.mood)
    }
}
