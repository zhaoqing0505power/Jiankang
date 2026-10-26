package com.jiankang.app.model

import kotlin.math.roundToInt

/**
 * Store all servings in grams so existing databases and JSON backups remain compatible.
 * One standard egg is estimated at 50g edible portion; actual size varies.
 */
object FoodPortions {
    const val EGG_GRAMS = 50
    fun countable(name: String) = name == "鸡蛋"

    fun eggGrams(count: Int): Int {
        require(count in 1..200)
        return count * EGG_GRAMS
    }

    fun portionText(entry: MealEntry): String =
        if (countable(entry.name) && entry.grams > 0 && entry.grams % EGG_GRAMS == 0)
            (entry.grams / EGG_GRAMS).toString() + "个（约" + entry.grams + "g）"
        else entry.grams.toString() + "g"

    /** Scale by the entry's saved energy density, preserving any previously customized kcal. */
    fun estimateKcal(entry: MealEntry, newGrams: Int): Int {
        require(entry.grams in 1..10000 && newGrams in 1..10000)
        return (entry.calories.toDouble() * newGrams / entry.grams).roundToInt()
            .coerceIn(0, 100000)
    }
}
