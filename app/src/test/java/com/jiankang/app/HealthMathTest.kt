package com.jiankang.app

import com.jiankang.app.model.HealthMath
import com.jiankang.app.model.MealEntry
import org.junit.Assert.*
import org.junit.Test

class HealthMathTest {
    @Test fun crossMidnightSleep() {
        assertEquals(480, HealthMath.sleepMinutes("23:00", "07:00"))
        assertEquals(30, HealthMath.sleepMinutes("23:40", "00:10"))
        assertEquals(450, HealthMath.sleepMinutes("22:30", "06:00"))
        assertNull(HealthMath.sleepMinutes("23:00", "23:00"))
        assertNull(HealthMath.sleepMinutes(null, "07:00"))
    }
    @Test fun bmiIsCalculatedFromCentimeters() {
        assertEquals(23.10, HealthMath.bmi(168.0, 65.2)!!, 0.05)
        assertNull(HealthMath.bmi(null, 65.2))
        assertNull(HealthMath.bmi(0.0, 65.2))
    }
    @Test fun calorieEstimateUsesMeasuredWeight() {
        assertEquals(120, HealthMath.exerciseKcal("快走", 30, 65.0)!!, 5)
        assertNull(HealthMath.exerciseKcal("快走", 30, null))
    }
    @Test fun emptyMealsAreNotGivenFakeDietScore() {
        assertNull(HealthMath.dietScore(emptyList()))
        val good = listOf(
            MealEntry(date="2026-10-26",meal="lunch",name="蔬菜",grams=200,calories=64,group="veg",emoji="🥬"),
            MealEntry(date="2026-10-26",meal="lunch",name="鱼",grams=100,calories=155,group="fish",emoji="🐟")
        )
        val poor = listOf(MealEntry(date="2026-10-26",meal="lunch",name="加工肉",grams=50,calories=180,group="processed",emoji="🥓"))
        assertTrue(HealthMath.dietScore(good)!! > HealthMath.dietScore(poor)!!)
    }
}
