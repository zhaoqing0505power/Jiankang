package com.jiankang.app.model

import java.time.LocalTime
import kotlin.math.roundToInt

enum class MealType(val code: String, val title: String) {
    BREAKFAST("breakfast", "早餐"),
    LUNCH("lunch", "午餐"),
    DINNER("dinner", "晚餐")
}

data class DayRecord(
    val date: String,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val sleepStart: String? = null,
    val sleepEnd: String? = null,
    val exerciseType: String? = null,
    val exerciseMinutes: Int? = null,
    val exerciseCalories: Int? = null,
    val caloriesEstimated: Boolean = false
)

data class MealEntry(
    val id: Long = 0L,
    val date: String,
    val meal: String,
    val name: String,
    val grams: Int,
    val calories: Int,
    val group: String,
    val emoji: String
)

data class FoodOption(
    val name: String,
    val emoji: String,
    val group: String,
    val per100Kcal: Int,
    val defaultGrams: Int = 100
) {
    fun toEntry(date: String, meal: MealType): MealEntry = MealEntry(
        date = date, meal = meal.code, name = name, grams = defaultGrams,
        calories = (per100Kcal * defaultGrams / 100.0).roundToInt(),
        group = group, emoji = emoji
    )
}

object FoodCatalog {
    val all = listOf(
        FoodOption("燕麦粥", "🥣", "whole", 71, 200),
        FoodOption("鸡蛋", "🥚", "protein", 144, 60),
        FoodOption("希腊酸奶", "🥛", "dairy", 100, 150),
        FoodOption("水果", "🍎", "fruit", 55, 150),
        FoodOption("全麦面包", "🍞", "whole", 247, 60),
        FoodOption("坚果", "🥜", "nuts", 607, 25),
        FoodOption("蔬菜沙拉", "🥗", "veg", 35, 200),
        FoodOption("三文鱼", "🐟", "fish", 208, 150),
        FoodOption("橄榄油", "🫒", "oil", 884, 10),
        FoodOption("糙米/全谷物", "🍚", "whole", 111, 150),
        FoodOption("蔬菜", "🥬", "veg", 32, 200),
        FoodOption("鱼类", "🐟", "fish", 155, 150),
        FoodOption("鸡胸肉", "🍗", "protein", 165, 150),
        FoodOption("豆类", "🫘", "protein", 127, 120),
        FoodOption("西红柿", "🍅", "veg", 18, 150),
        FoodOption("绿叶菜", "🥬", "veg", 25, 180),
        FoodOption("牛油果", "🥑", "oil", 160, 100),
        FoodOption("奶酪", "🧀", "dairy", 310, 30),
        FoodOption("加工肉", "🥓", "processed", 320, 70),
        FoodOption("甜饮料", "🥤", "sugar", 42, 250),
        FoodOption("精制糕点", "🍰", "sugar", 360, 80),
        FoodOption("肥肉", "🍖", "processed", 395, 100)
    )
    fun recommended(meal: MealType): List<FoodOption> {
        val names = when (meal) {
            MealType.BREAKFAST -> listOf("燕麦粥", "鸡蛋", "希腊酸奶", "水果", "全麦面包", "坚果", "蔬菜沙拉", "三文鱼", "橄榄油")
            MealType.LUNCH -> listOf("糙米/全谷物", "蔬菜", "鱼类", "鸡胸肉", "豆类", "橄榄油", "西红柿", "绿叶菜", "牛油果")
            MealType.DINNER -> listOf("鱼类", "蔬菜", "糙米/全谷物", "豆类", "鸡胸肉", "橄榄油", "绿叶菜", "三文鱼", "水果")
        }
        return names.mapNotNull { n -> all.firstOrNull { it.name == n } }
    }
}

object HealthMath {
    fun bmi(heightCm: Double?, weightKg: Double?): Double? =
        if (heightCm != null && weightKg != null && heightCm > 0 && weightKg > 0)
            weightKg / (heightCm * heightCm / 10000.0) else null

    // One sleep record belongs to the waking date. If wake <= bed, it is the next day.
    // Identical times are treated as missing rather than fabricating 24 h sleep.
    fun sleepMinutes(start: String?, end: String?): Int? {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return null
        return try {
            val from = LocalTime.parse(start).toSecondOfDay() / 60
            val until = LocalTime.parse(end).toSecondOfDay() / 60
            val diff = (until - from + 1440) % 1440
            diff.takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
    }

    fun sleepText(minutes: Int?): String {
        if (minutes == null) return "未记录"
        return (minutes / 60).toString() + " 小时 " + (minutes % 60) + " 分钟"
    }

    fun exerciseKcal(type: String, minutes: Int, weightKg: Double?): Int? {
        if (minutes <= 0 || weightKg == null || weightKg <= 0) return null
        val met = when (type) {
            "快走" -> 3.5; "跑步" -> 8.0; "骑行" -> 6.0; "游泳" -> 6.0
            "力量训练" -> 3.5; "瑜伽" -> 2.5; "球类" -> 6.0
            else -> 3.0
        }
        return (met * 3.5 * weightKg / 200.0 * minutes).roundToInt()
    }

    // Non-clinical habit reflection, not a medical nutrition evaluation.
    fun dietScore(meals: List<MealEntry>): Int? {
        if (meals.isEmpty()) return null
        val groups = meals.map { it.group }.toSet()
        var score = 30
        if ("veg" in groups) score += 16
        if ("fruit" in groups) score += 12
        if ("fish" in groups) score += 12
        if ("whole" in groups) score += 11
        if ("protein" in groups) score += 10
        if ("oil" in groups) score += 8
        if ("nuts" in groups) score += 5
        if ("dairy" in groups) score += 3
        score += meals.map { it.meal }.distinct().size * 3
        if ("processed" in groups) score -= 15
        if ("sugar" in groups) score -= 15
        return score.coerceIn(0, 100)
    }
}
