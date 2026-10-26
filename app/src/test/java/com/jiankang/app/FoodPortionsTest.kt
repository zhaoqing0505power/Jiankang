package com.jiankang.app

import com.jiankang.app.model.FoodCatalog
import com.jiankang.app.model.FoodPortions
import com.jiankang.app.model.MealType
import com.jiankang.app.model.MealEntry
import org.junit.Assert.*
import org.junit.Test

class FoodPortionsTest {
    @Test fun eggCountIsConvertedToWeightAndCalories() {
        val food = FoodCatalog.all.first { it.name == "鸡蛋" }
        val entry = food.toEntry("2026-10-26", MealType.BREAKFAST)
        assertEquals(50, entry.grams)
        assertEquals(72, entry.calories)
        assertEquals("1个（约50g）", FoodPortions.portionText(entry))
        assertEquals(100, FoodPortions.eggGrams(2))
        assertEquals(144, FoodPortions.estimateKcal(entry, FoodPortions.eggGrams(2)))
        assertEquals(216, FoodPortions.estimateKcal(entry, FoodPortions.eggGrams(3)))
    }

    @Test fun editingAllFoodsUpdatesCaloriesProportionally() {
        val porridge = FoodCatalog.all.first { it.name == "小米粥" }
            .toEntry("2026-10-26", MealType.BREAKFAST)
        assertEquals(115, porridge.calories)
        assertEquals(230, FoodPortions.estimateKcal(porridge, 500))
        val custom = MealEntry(date="2026-10-26",meal="lunch",name="自制三明治",
            grams=200,calories=330,group="other",emoji="🥪")
        assertEquals(165, FoodPortions.estimateKcal(custom, 100))
    }

    @Test fun historicalEggGramEntriesAreStillValid() {
        val old = MealEntry(date="2026-10-26",meal="breakfast",name="鸡蛋",
            grams=60,calories=86,group="protein",emoji="🥚")
        assertEquals("60g", FoodPortions.portionText(old))
    }
}
