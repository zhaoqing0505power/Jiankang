package com.jiankang.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankang.app.model.DayRecord
import com.jiankang.app.model.HealthMath
import java.time.LocalDate
import java.util.Locale

private fun cleanNumber(n: Double?): String =
    n?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()

@Composable
private fun NumberField(
    title: String, value: String, onChange: (String) -> Unit,
    unit: String, decimal: Boolean = true, modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(
            value = value, onValueChange = { new ->
                val filtered = new.filter { it.isDigit() || (decimal && it == '.') }
                if (filtered.length <= 9 && filtered.count { it == '.' } <= 1) onChange(filtered)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("请输入", fontSize = 14.sp, color = Slate) },
            suffix = { Text(unit, color = Slate, fontSize = 12.sp) },
            singleLine = true, shape = RoundedCornerShape(9.dp),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Green, unfocusedBorderColor = Line,
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
            )
        )
    }
}

@Composable
fun BodyScreen(date: LocalDate, day: DayRecord, onBack: () -> Unit, save: (DayRecord) -> Unit) {
    val context = LocalContext.current
    var height by remember(date, day) { mutableStateOf(cleanNumber(day.heightCm)) }
    var weight by remember(date, day) { mutableStateOf(cleanNumber(day.weightKg)) }
    var systolic by remember(date, day) { mutableStateOf(day.systolic?.toString().orEmpty()) }
    var diastolic by remember(date, day) { mutableStateOf(day.diastolic?.toString().orEmpty()) }
    val h = height.toDoubleOrNull()
    val w = weight.toDoubleOrNull()
    val bmi = HealthMath.bmi(h, w)
    PageColumn("身体数据", date, onBack = onBack) {
        Spacer(Modifier.height(12.dp))
        NumberField("身高", height, { height = it }, "cm")
        Spacer(Modifier.height(10.dp))
        NumberField("体重", weight, { weight = it }, "kg")
        Spacer(Modifier.height(10.dp))
        Text("血压", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField("收缩压", systolic, { systolic = it }, "mmHg", false, Modifier.weight(1f))
            NumberField("舒张压", diastolic, { diastolic = it }, "mmHg", false, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton {
            val s = systolic.toIntOrNull()
            val d = diastolic.toIntOrNull()
            if ((height.isNotBlank() && (h == null || h !in 80.0..250.0)) ||
                (weight.isNotBlank() && (w == null || w !in 15.0..400.0)) ||
                (systolic.isNotBlank() && (s == null || s !in 60..260)) ||
                (diastolic.isNotBlank() && (d == null || d !in 30..180)) ||
                (s != null && d != null && s <= d)
            ) {
                showMessage(context, "请检查身高、体重或血压数值")
            } else if (h == null && w == null && s == null && d == null) {
                showMessage(context, "请至少填写一项")
            } else {
                save(day.copy(heightCm = h, weightKg = w, systolic = s, diastolic = d))
            }
        }
        Spacer(Modifier.height(9.dp))
        AppCard(background = Color(0xFFE3F9F2)) {
            Text("体重指数（BMI）", color = DarkGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    bmi?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "--",
                    fontWeight = FontWeight.Bold, color = Ink, fontSize = 25.sp
                )
                Spacer(Modifier.width(14.dp))
                if (bmi != null) {
                    Tag(
                        when {
                            bmi < 18.5 -> "偏低"
                            bmi < 24 -> "正常"
                            bmi < 28 -> "偏高"
                            else -> "肥胖范围"
                        }
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            SmallLabel("参考范围：18.5–23.9（成年人）")
        }
    }
}

@Composable
fun SleepScreen(
    date: LocalDate, day: DayRecord, sleepGoal: Int,
    onBack: () -> Unit, save: (DayRecord) -> Unit
) {
    val context = LocalContext.current
    var start by remember(date, day) { mutableStateOf(day.sleepStart ?: "23:00") }
    var end by remember(date, day) { mutableStateOf(day.sleepEnd ?: "07:00") }
    val duration = HealthMath.sleepMinutes(start, end)
    PageColumn("睡眠记录", date, onBack = onBack) {
        Spacer(Modifier.height(12.dp))
        AppCard {
            TimeEntry("入睡时间", start) {
                openTimePicker(context, start) { start = it }
            }
            Spacer(Modifier.height(8.dp))
            TimeEntry("起床时间", end) {
                openTimePicker(context, end) { end = it }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().background(
                    Color(0xFFE5F5EE), RoundedCornerShape(10.dp)
                ).padding(14.dp)
            ) {
                SmallLabel("睡眠时长（自动计算）")
                Spacer(Modifier.height(8.dp))
                Text(HealthMath.sleepText(duration), fontSize = 22.sp, color = Ink, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton {
                if (duration == null) showMessage(context, "请选择不同的入睡与起床时间")
                else save(day.copy(sleepStart = start, sleepEnd = end))
            }
        }
        AppCard(background = Color(0xFFE9F5FF)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, null, tint = Blue)
                Spacer(Modifier.width(9.dp))
                Text("目标：每天 " + sleepGoal + " 小时", color = Color(0xFF245394), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            ProgressTrack((duration ?: 0) / (sleepGoal * 60f), color = Color(0xFF78A6FB))
            Spacer(Modifier.height(7.dp))
            Text(
                if (duration == null) "等待记录"
                else if (duration >= sleepGoal * 60) "太棒了！已达成目标"
                else "距离目标还差 " + HealthMath.sleepText(sleepGoal * 60 - duration),
                fontSize = 12.sp, color = DarkGreen
            )
        }
        SmallLabel("以起床日期归档。入睡时间晚于起床时间时，自动按跨午夜计算。")
    }
}

@Composable
private fun TimeEntry(title: String, time: String, click: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(65.dp).clickable(onClick = click),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(9.dp), color = Pale) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(time, fontWeight = FontWeight.Medium, color = Ink, fontSize = 16.sp)
                Spacer(Modifier.width(18.dp))
                Icon(Icons.Default.Schedule, null, tint = Ink, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private val activities = listOf(
    "快走" to "🚶", "跑步" to "🏃", "骑行" to "🚴", "游泳" to "🏊",
    "力量训练" to "🏋️", "瑜伽" to "🧘", "球类" to "⚽", "其他" to "•••"
)

@Composable
fun ExerciseScreen(
    date: LocalDate, day: DayRecord, onBack: () -> Unit, save: (DayRecord) -> Unit
) {
    val context = LocalContext.current
    var type by remember(date, day) { mutableStateOf(day.exerciseType ?: "快走") }
    var minutes by remember(date, day) { mutableStateOf(day.exerciseMinutes?.toString().orEmpty()) }
    var calories by remember(date, day) { mutableStateOf(
        if (day.caloriesEstimated) "" else day.exerciseCalories?.toString().orEmpty()
    ) }
    var expanded by remember { mutableStateOf(false) }
    val duration = minutes.toIntOrNull()
    val estimate = duration?.let { HealthMath.exerciseKcal(type, it, day.weightKg) }
    PageColumn("运动记录", date, onBack = onBack) {
        Spacer(Modifier.height(12.dp))
        AppCard {
            Text("运动类型", color = Ink, fontSize = 14.sp)
            Spacer(Modifier.height(7.dp))
            Box {
                OutlinedButton(
                    onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(type, modifier = Modifier.weight(1f), color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Ink)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    activities.forEach { (name, emoji) ->
                        DropdownMenuItem(text = { Text(emoji + "  " + name) }, onClick = {
                            type = name; expanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            NumberField("运动时间", minutes, { minutes = it }, "分钟", false)
            Spacer(Modifier.height(12.dp))
            NumberField("消耗千卡", calories, { calories = it }, "千卡", false)
            if (calories.isBlank()) {
                Spacer(Modifier.height(6.dp))
                SmallLabel(
                    if (estimate != null) "留空时估算：" + estimate + " 千卡（按记录的体重估算）"
                    else "填写消耗千卡；记录体重后也可自动估算"
                )
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton {
                val kcal = calories.toIntOrNull() ?: estimate
                if (duration == null || duration !in 1..1440 ||
                    (calories.isNotBlank() && (kcal == null || kcal !in 0..20000))
                ) showMessage(context, "请检查运动分钟数或千卡数值")
                else save(day.copy(
                    exerciseType = type, exerciseMinutes = duration,
                    exerciseCalories = kcal, caloriesEstimated = calories.isBlank() && kcal != null
                ))
            }
        }
        AppCard {
            SectionHeading("常用运动")
            Spacer(Modifier.height(11.dp))
            activities.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (name, emoji) ->
                        Column(
                            Modifier.weight(1f).background(
                                if (type == name) SoftGreen else Pale, RoundedCornerShape(9.dp)
                            ).clickable { type = name }.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(emoji, fontSize = 24.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(name, fontSize = 10.sp, color = if (type == name) DarkGreen else Slate)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        SmallLabel("运动热量使用标准代谢当量和记录体重粗略估算，实际消耗因人而异。")
    }
}
