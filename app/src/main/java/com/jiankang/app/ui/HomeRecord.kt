package com.jiankang.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankang.app.model.DayRecord
import com.jiankang.app.model.HealthMath
import com.jiankang.app.model.MealEntry
import com.jiankang.app.model.MealType
import com.jiankang.app.data.AppSettings
import com.jiankang.app.screentime.ScreenUsage
import java.time.LocalDate
import java.util.Locale

private fun present(value: Double?): String =
    value?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "--"

@Composable
fun HomeScreen(
    date: LocalDate, day: DayRecord, meals: List<MealEntry>,
    sleepGoal: Int, previousHeight: Double?, screenUsage: ScreenUsage?, settings: AppSettings,
    onDate: () -> Unit, go: (Page) -> Unit
) {
    val mealsDone = MealType.entries.count { meal -> meals.any { it.meal == meal.code } }
    val bodyDone = day.heightCm != null || day.weightKg != null || day.systolic != null || day.diastolic != null
    val sleep = HealthMath.sleepMinutes(day.sleepStart, day.sleepEnd)
    val exerciseDone = day.exerciseMinutes != null && day.exerciseMinutes > 0
    val reflectionDone = !day.reflection.isNullOrBlank() || !day.mood.isNullOrBlank()
    val finished = mealsDone + (if (bodyDone) 1 else 0) + (if (sleep != null) 1 else 0) +
        (if (exerciseDone) 1 else 0) + (if (reflectionDone) 1 else 0)
    val progress = finished / 7f
    PageColumn("今天", date, onDate = onDate) {
        Box(Modifier.fillMaxWidth().height(208.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(172.dp)) {
                val sw = 8.dp.toPx()
                drawArc(Color(0xFFD7F6F2), -90f, 360f, false, style = Stroke(sw, cap = StrokeCap.Round))
                drawArc(Green, -90f, 360f * progress, false, style = Stroke(sw, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Eco, null, tint = Green, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(5.dp))
                Text("保持良好习惯", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text("朝着更健康的自己前进！", color = Slate, fontSize = 11.sp)
            }
        }
        AppCard(modifier = Modifier.clickable { go(Page.BODY) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorIcon(Icons.Default.MonitorWeight, Blue)
                Spacer(Modifier.width(12.dp))
                Text("身体数据", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricValue("身高", if (day.heightCm == null && previousHeight == null) "--" else present(day.heightCm ?: previousHeight) + " cm", Modifier.weight(1f))
                MetricValue("体重", if (day.weightKg == null) "--" else present(day.weightKg) + " kg", Modifier.weight(1f))
                MetricValue(
                    "血压", if (day.systolic == null || day.diastolic == null) "--"
                    else day.systolic.toString() + "/" + day.diastolic + " mmHg", Modifier.weight(1.3f)
                )
            }
        }
        AppCard(modifier = Modifier.clickable { go(Page.RECORD) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorIcon(Icons.Default.RestaurantMenu, Color(0xFFFFC038))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("饮食记录", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    SmallLabel("早餐 · 午餐 · 晚餐")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(mealsDone.toString() + "/3", color = DarkGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    SmallLabel("已记录")
                }
            }
        }
        AppCard(modifier = Modifier.clickable { go(Page.SLEEP) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorIcon(Icons.Default.Bedtime, Blue)
                Spacer(Modifier.width(12.dp))
                Text("睡眠", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (sleep == null) "未记录" else String.format(Locale.ROOT, "%.1f / %d 小时", sleep / 60f, sleepGoal),
                    fontSize = 13.sp, color = Ink
                )
            }
            Spacer(Modifier.height(10.dp))
            ProgressTrack((sleep ?: 0) / (sleepGoal * 60f), color = Color(0xFF85AFF9))
        }
        AppCard(modifier = Modifier.clickable { go(Page.EXERCISE) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorIcon(Icons.Default.DirectionsRun, Color(0xFF2AC86B))
                Spacer(Modifier.width(12.dp))
                Text("运动", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (!exerciseDone) "未记录"
                    else day.exerciseMinutes.toString() + " 分钟   " + (day.exerciseCalories?.toString() ?: "--") + " 千卡",
                    fontSize = 12.sp, color = Ink
                )
            }
            Spacer(Modifier.height(10.dp))
            ProgressTrack((day.exerciseMinutes ?: 0) / 30f, Green)
        }
        if (reflectionDone) {
            AppCard(modifier = Modifier.clickable { go(Page.REFLECTION) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorIcon(Icons.Default.AutoStories, Violet)
                    Spacer(Modifier.width(12.dp))
                    Text("今日感悟", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(day.mood ?: "已记录", color = Violet, fontSize = 12.sp)
                }
                if (!day.reflection.isNullOrBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(day.reflection.take(70), color = Slate, fontSize = 12.sp, maxLines = 2)
                }
            }
        }
        ScreenTimeSummaryCard(date, screenUsage, settings) { go(Page.SCREEN_TIME) }
    }
}

@Composable
fun RecordScreen(date: LocalDate, day: DayRecord, meals: List<MealEntry>,
                 onDate: () -> Unit, go: (Page) -> Unit) {
    PageColumn("今天记录", date, onDate = onDate) {
        RecordTile(
            Icons.Default.MonitorWeight, Blue, "身体数据", "身高、体重、血压",
            day.heightCm != null || day.weightKg != null || day.systolic != null
        ) { go(Page.BODY) }
        RecordTile(
            Icons.Default.FreeBreakfast, Orange, "早餐", "记录你吃了什么",
            meals.any { it.meal == MealType.BREAKFAST.code }
        ) { go(Page.BREAKFAST) }
        RecordTile(
            Icons.Default.LunchDining, Color(0xFFFFA53D), "午餐", "记录你吃了什么",
            meals.any { it.meal == MealType.LUNCH.code }
        ) { go(Page.LUNCH) }
        RecordTile(
            Icons.Default.DinnerDining, Coral, "晚餐", "记录你吃了什么",
            meals.any { it.meal == MealType.DINNER.code }
        ) { go(Page.DINNER) }
        RecordTile(
            Icons.Default.Bedtime, Violet, "睡眠", "起止时间、自动计算时长",
            HealthMath.sleepMinutes(day.sleepStart, day.sleepEnd) != null
        ) { go(Page.SLEEP) }
        RecordTile(
            Icons.Default.DirectionsRun, Color(0xFF27C96A), "运动", "运动时间与消耗千卡",
            day.exerciseMinutes != null && day.exerciseMinutes > 0
        ) { go(Page.EXERCISE) }
        RecordTile(
            Icons.Default.AutoStories, Violet, "感悟", "记录今天的心情和生活体会",
            !day.reflection.isNullOrBlank() || !day.mood.isNullOrBlank()
        ) { go(Page.REFLECTION) }
        Spacer(Modifier.height(4.dp))
        Text("点开卡片填写，可补记其他日期。每条记录自动保存在本机。", fontSize = 12.sp, color = Slate)
    }
}

@Composable
private fun RecordTile(
    icon: ImageVector, tint: Color, title: String, description: String,
    done: Boolean, onClick: () -> Unit
) {
    AppCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
            ColorIcon(icon, tint)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                SmallLabel(description)
            }
            if (done) Icon(Icons.Default.CheckCircle, "已记录", tint = Green, modifier = Modifier.size(18.dp))
            else Icon(Icons.Default.ChevronRight, null, tint = Slate)
        }
    }
}
