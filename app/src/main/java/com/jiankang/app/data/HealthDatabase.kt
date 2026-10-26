package com.jiankang.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jiankang.app.model.DayRecord
import com.jiankang.app.model.MealEntry
import com.jiankang.app.model.MealType
import com.jiankang.app.model.HealthMath
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class HealthDatabase(context: Context) : SQLiteOpenHelper(context, "jiankang.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE days (
                date TEXT PRIMARY KEY NOT NULL,
                height_cm REAL, weight_kg REAL, systolic INTEGER, diastolic INTEGER,
                sleep_start TEXT, sleep_end TEXT, exercise_type TEXT,
                exercise_minutes INTEGER, exercise_calories INTEGER, calories_estimated INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL(
            """CREATE TABLE meals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL, meal TEXT NOT NULL, name TEXT NOT NULL,
                grams INTEGER NOT NULL, calories INTEGER NOT NULL, food_group TEXT NOT NULL, emoji TEXT NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX meals_by_date ON meals(date,meal)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Explicit migrations are required when the schema version is increased.
    }

    fun day(date: String): DayRecord {
        readableDatabase.query("days", null, "date=?", arrayOf(date), null, null, null).use {
            return if (it.moveToFirst()) readDay(it) else DayRecord(date)
        }
    }

    fun days(start: String, end: String): List<DayRecord> {
        val result = mutableListOf<DayRecord>()
        readableDatabase.query(
            "days", null, "date>=? AND date<=?", arrayOf(start, end), null, null, "date ASC"
        ).use { c -> while (c.moveToNext()) result.add(readDay(c)) }
        return result
    }

    fun allDays(): List<DayRecord> {
        val result = mutableListOf<DayRecord>()
        readableDatabase.query("days", null, null, null, null, null, "date ASC").use {
            while (it.moveToNext()) result.add(readDay(it))
        }
        return result
    }

    private fun Cursor.nullInt(column: String): Int? {
        val i = getColumnIndexOrThrow(column)
        return if (isNull(i)) null else getInt(i)
    }
    private fun Cursor.nullDouble(column: String): Double? {
        val i = getColumnIndexOrThrow(column)
        return if (isNull(i)) null else getDouble(i)
    }
    private fun Cursor.nullString(column: String): String? {
        val i = getColumnIndexOrThrow(column)
        return if (isNull(i)) null else getString(i)
    }
    private fun readDay(c: Cursor) = DayRecord(
        date = c.getString(c.getColumnIndexOrThrow("date")),
        heightCm = c.nullDouble("height_cm"),
        weightKg = c.nullDouble("weight_kg"),
        systolic = c.nullInt("systolic"),
        diastolic = c.nullInt("diastolic"),
        sleepStart = c.nullString("sleep_start"),
        sleepEnd = c.nullString("sleep_end"),
        exerciseType = c.nullString("exercise_type"),
        exerciseMinutes = c.nullInt("exercise_minutes"),
        exerciseCalories = c.nullInt("exercise_calories"),
        caloriesEstimated = c.getInt(c.getColumnIndexOrThrow("calories_estimated")) != 0
    )

    fun saveDay(record: DayRecord) = putDay(writableDatabase, record)

    private fun putDay(db: SQLiteDatabase, record: DayRecord) {
        LocalDate.parse(record.date)
        val values = ContentValues().apply {
            put("date", record.date)
            record.heightCm?.let { put("height_cm", it) } ?: putNull("height_cm")
            record.weightKg?.let { put("weight_kg", it) } ?: putNull("weight_kg")
            record.systolic?.let { put("systolic", it) } ?: putNull("systolic")
            record.diastolic?.let { put("diastolic", it) } ?: putNull("diastolic")
            record.sleepStart?.let { put("sleep_start", it) } ?: putNull("sleep_start")
            record.sleepEnd?.let { put("sleep_end", it) } ?: putNull("sleep_end")
            record.exerciseType?.let { put("exercise_type", it) } ?: putNull("exercise_type")
            record.exerciseMinutes?.let { put("exercise_minutes", it) } ?: putNull("exercise_minutes")
            record.exerciseCalories?.let { put("exercise_calories", it) } ?: putNull("exercise_calories")
            put("calories_estimated", if (record.caloriesEstimated) 1 else 0)
        }
        db.insertWithOnConflict("days", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun readMeal(c: Cursor) = MealEntry(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        date = c.getString(c.getColumnIndexOrThrow("date")),
        meal = c.getString(c.getColumnIndexOrThrow("meal")),
        name = c.getString(c.getColumnIndexOrThrow("name")),
        grams = c.getInt(c.getColumnIndexOrThrow("grams")),
        calories = c.getInt(c.getColumnIndexOrThrow("calories")),
        group = c.getString(c.getColumnIndexOrThrow("food_group")),
        emoji = c.getString(c.getColumnIndexOrThrow("emoji"))
    )

    fun meals(date: String, meal: MealType? = null): List<MealEntry> {
        val result = mutableListOf<MealEntry>()
        val selector = if (meal == null) "date=?" else "date=? AND meal=?"
        val args = if (meal == null) arrayOf(date) else arrayOf(date, meal.code)
        readableDatabase.query("meals", null, selector, args, null, null, "id ASC").use {
            while (it.moveToNext()) result.add(readMeal(it))
        }
        return result
    }

    fun allMeals(): List<MealEntry> {
        val result = mutableListOf<MealEntry>()
        readableDatabase.query("meals", null, null, null, null, null, "date ASC,id ASC").use {
            while (it.moveToNext()) result.add(readMeal(it))
        }
        return result
    }

    fun replaceMeal(date: String, meal: MealType, entries: List<MealEntry>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("meals", "date=? AND meal=?", arrayOf(date, meal.code))
            entries.forEach { item -> putMeal(db, item.copy(date = date, meal = meal.code)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun putMeal(db: SQLiteDatabase, item: MealEntry) {
        require(item.grams > 0 && item.grams <= 10000 && item.calories >= 0 && item.calories <= 100000)
        require(item.name.isNotBlank() && item.name.length <= 100)
        require(item.group.length <= 50 && item.emoji.length <= 16)
        LocalDate.parse(item.date)
        require(MealType.entries.any { it.code == item.meal })
        db.insertOrThrow("meals", null, ContentValues().apply {
            put("date", item.date); put("meal", item.meal); put("name", item.name)
            put("grams", item.grams); put("calories", item.calories)
            put("food_group", item.group); put("emoji", item.emoji)
        })
    }

    fun exportJson(): String {
        val result = JSONObject().put("format", "Jiankang").put("version", 1)
        val dayArray = JSONArray()
        allDays().forEach { r -> dayArray.put(JSONObject().apply {
            put("date", r.date); put("heightCm", r.heightCm); put("weightKg", r.weightKg)
            put("systolic", r.systolic); put("diastolic", r.diastolic)
            put("sleepStart", r.sleepStart); put("sleepEnd", r.sleepEnd)
            put("exerciseType", r.exerciseType); put("exerciseMinutes", r.exerciseMinutes)
            put("exerciseCalories", r.exerciseCalories); put("caloriesEstimated", r.caloriesEstimated)
        }) }
        val mealArray = JSONArray()
        allMeals().forEach { r -> mealArray.put(JSONObject().apply {
            put("date", r.date); put("meal", r.meal); put("name", r.name)
            put("grams", r.grams); put("calories", r.calories)
            put("group", r.group); put("emoji", r.emoji)
        }) }
        return result.put("days", dayArray).put("meals", mealArray).toString(2)
    }

    private fun JSONObject.doubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf { it.isFinite() }
    private fun JSONObject.intOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)
    private fun JSONObject.stringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    // Atomic replacement: malformed data rolls back, preserving the existing journal.
    fun importJson(text: String) {
        require(text.length <= 12_000_000) { "备份文件过大" }
        val root = JSONObject(text)
        require(root.optString("format") == "Jiankang" && root.optInt("version") == 1) { "备份格式不受支持" }
        val dayArray = root.getJSONArray("days")
        val mealArray = root.getJSONArray("meals")
        require(dayArray.length() <= 100_000 && mealArray.length() <= 1_000_000) { "记录数量超出限制" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("meals", null, null)
            db.delete("days", null, null)
            for (i in 0 until dayArray.length()) {
                val o = dayArray.getJSONObject(i)
                val r = DayRecord(
                    date = o.getString("date"), heightCm = o.doubleOrNull("heightCm"),
                    weightKg = o.doubleOrNull("weightKg"), systolic = o.intOrNull("systolic"),
                    diastolic = o.intOrNull("diastolic"), sleepStart = o.stringOrNull("sleepStart"),
                    sleepEnd = o.stringOrNull("sleepEnd"), exerciseType = o.stringOrNull("exerciseType"),
                    exerciseMinutes = o.intOrNull("exerciseMinutes"),
                    exerciseCalories = o.intOrNull("exerciseCalories"),
                    caloriesEstimated = o.optBoolean("caloriesEstimated", false)
                )
                require(r.sleepStart == null || HealthMath.sleepMinutes(r.sleepStart, r.sleepEnd) != null)
                putDay(db, r)
            }
            for (i in 0 until mealArray.length()) {
                val o = mealArray.getJSONObject(i)
                putMeal(db, MealEntry(
                    date = o.getString("date"), meal = o.getString("meal"),
                    name = o.getString("name"), grams = o.getInt("grams"),
                    calories = o.getInt("calories"), group = o.optString("group", "other"),
                    emoji = o.optString("emoji", "🍽️")
                ))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun exportCsv(): String {
        fun esc(raw: String): String = "\"" + raw.replace("\"", "\"\"") + "\""
        val header = "日期,身高(cm),体重(kg),收缩压(mmHg),舒张压(mmHg),睡眠开始,睡眠结束,睡眠分钟,运动类型,运动分钟,运动大卡,早餐,午餐,晚餐"
        val grouped = allMeals().groupBy { it.date to it.meal }
        val rows = allDays().map { d ->
            val vals = listOf(
                d.date, d.heightCm?.toString().orEmpty(), d.weightKg?.toString().orEmpty(),
                d.systolic?.toString().orEmpty(), d.diastolic?.toString().orEmpty(),
                d.sleepStart.orEmpty(), d.sleepEnd.orEmpty(),
                HealthMath.sleepMinutes(d.sleepStart, d.sleepEnd)?.toString().orEmpty(),
                d.exerciseType.orEmpty(), d.exerciseMinutes?.toString().orEmpty(),
                d.exerciseCalories?.toString().orEmpty()
            ) + MealType.entries.map { type ->
                grouped[d.date to type.code].orEmpty().joinToString("；") {
                    it.name + "(" + it.grams + "g/" + it.calories + "kcal)"
                }
            }
            vals.joinToString(",") { esc(it) }
        }
        return "\uFEFF" + (listOf(header) + rows).joinToString("\r\n")
    }
}
