package com.jiankang.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankang.app.data.HealthDatabase
import com.jiankang.app.model.DayRecord
import com.jiankang.app.model.HealthMath
import com.jiankang.app.model.MealEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private enum class Period(val label: String) { WEEK("周"), MONTH("月"), YEAR("年") }

private data class ChartWindow(val start: LocalDate, val end: LocalDate, val ticks: List<LocalDate>) {
    val label: String
        get() = if (start.year == end.year && start.monthValue == end.monthValue)
            start.format(DateTimeFormatter.ofPattern("M月d日")) + " – " +
                end.format(DateTimeFormatter.ofPattern("M月d日"))
        else start.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) + " – " +
            end.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
}

private fun window(date: LocalDate, period: Period, offset: Int): ChartWindow {
    return when (period) {
        Period.WEEK -> {
            val monday = date.plusWeeks(offset.toLong()).with(DayOfWeek.MONDAY)
            ChartWindow(monday, monday.plusDays(6), (0..6).map { monday.plusDays(it.toLong()) })
        }
        Period.MONTH -> {
            val first = date.plusMonths(offset.toLong()).withDayOfMonth(1)
            ChartWindow(first, first.plusMonths(1).minusDays(1),
                (0 until first.lengthOfMonth()).map { first.plusDays(it.toLong()) })
        }
        Period.YEAR -> {
            val first = date.plusYears(offset.toLong()).withDayOfYear(1)
            ChartWindow(first, first.plusYears(1).minusDays(1),
                (0..11).map { first.plusMonths(it.toLong()) })
        }
    }
}

private data class PlotSeries(val label: String, val values: List<Double?>, val color: Color)

private fun plotValues(
    days: List<DayRecord>, ticks: List<LocalDate>, period: String,
    selector: (DayRecord) -> Double?, aggregateSum: Boolean = false
): List<Double?> {
    val daily = days.associateBy { it.date }
    if (period != "年") return ticks.map { daily[it.toString()]?.let(selector) }
    return ticks.map { month ->
        val nums = days.filter { it.date.startsWith(month.toString().take(7)) }.mapNotNull(selector)
        if (nums.isEmpty()) null else if (aggregateSum) nums.sum() else nums.average()
    }
}

@Composable
fun StatsScreen(date: LocalDate, db: HealthDatabase, revision: Int) {
    var period by remember { mutableStateOf(Period.WEEK) }
    var offset by remember(period) { mutableIntStateOf(0) }
    val win = remember(date, period, offset) { window(date, period, offset) }
    val days = remember(win, revision) { db.days(win.start.toString(), win.end.toString()) }
    val labels = when (period) {
        Period.WEEK -> listOf(win.start.format(DateTimeFormatter.ofPattern("M/d")),
            win.start.plusDays(3).format(DateTimeFormatter.ofPattern("M/d")),
            win.end.format(DateTimeFormatter.ofPattern("M/d")))
        Period.MONTH -> listOf("1日", "15日", win.end.dayOfMonth.toString() + "日")
        Period.YEAR -> listOf("1月", "6月", "12月")
    }
    PageColumn("数据统计") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Period.entries.forEach { option ->
                Box(
                    Modifier.weight(1f).background(
                        if (option == period) Green else Color(0xFFEFF2F5), RoundedCornerShape(7.dp)
                    ).clickable { period = option }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option.label, color = if (option == period) Color.White else Ink,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { offset-- }) { Icon(Icons.Default.ChevronLeft, "上一周期") }
            Text(win.label, modifier = Modifier.weight(1f), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, color = Ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { if (offset < 0) offset++ }, enabled = offset < 0) {
                Icon(Icons.Default.ChevronRight, "下一周期")
            }
        }
        val periodText = period.label
        TrendCard(
            "体重", "kg", labels,
            listOf(PlotSeries("体重", plotValues(days, win.ticks, periodText) { it.weightKg }, Green))
        )
        TrendCard(
            "血压", "mmHg", labels,
            listOf(
                PlotSeries("收缩压", plotValues(days, win.ticks, periodText) { it.systolic?.toDouble() }, Coral),
                PlotSeries("舒张压", plotValues(days, win.ticks, periodText) { it.diastolic?.toDouble() }, Blue)
            )
        )
        TrendCard(
            "睡眠", "小时", labels,
            listOf(PlotSeries("睡眠时长",
                plotValues(days, win.ticks, periodText) {
                    HealthMath.sleepMinutes(it.sleepStart, it.sleepEnd)?.div(60.0)
                }, Violet))
        )
        TrendCard(
            "运动", "分钟", labels,
            listOf(PlotSeries("运动分钟",
                plotValues(days, win.ticks, periodText,
                    selector = { it.exerciseMinutes?.toDouble() }, aggregateSum = period == Period.YEAR),
                Green))
        )
        SmallLabel("趋势仅显示已录入的数据；缺失日期不会补造测量值。")
    }
}

@Composable
private fun TrendCard(title: String, unit: String, tickLabels: List<String>, series: List<PlotSeries>) {
    val samples = series.flatMap { s -> s.values.filterNotNull() }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title + " (" + unit + ")", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = Ink, modifier = Modifier.weight(1f))
            Text(
                samples.lastOrNull()?.let { number ->
                    String.format(Locale.ROOT, "%.1f", number)
                } ?: "--", color = Ink, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            series.forEach { s ->
                Box(
                    Modifier.size(7.dp).background(s.color, RoundedCornerShape(10.dp))
                        .align(Alignment.CenterVertically)
                )
                Spacer(Modifier.width(5.dp))
                Text(s.label, fontSize = 10.sp, color = Slate)
                Spacer(Modifier.width(11.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        if (samples.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                Text("暂无记录", fontSize = 13.sp, color = Slate)
            }
        } else {
            val minValue = samples.minOrNull() ?: 0.0
            val maxValue = samples.maxOrNull() ?: 1.0
            val spread = (maxValue - minValue).coerceAtLeast(1.0)
            val lower = minValue - spread * 0.15
            val upper = maxValue + spread * 0.15
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                val left = 9.dp.toPx()
                val right = size.width - 9.dp.toPx()
                val top = 10.dp.toPx()
                val bottom = size.height - 10.dp.toPx()
                for (line in 0..3) {
                    val y = top + (bottom - top) * line / 3f
                    drawLine(Line, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
                }
                val range = (upper - lower).coerceAtLeast(0.001)
                series.forEach { s ->
                    val points = s.values
                    if (points.isEmpty()) return@forEach
                    fun point(i: Int, value: Double): Offset {
                        val x = if (points.size == 1) (left + right) / 2f
                        else left + (right - left) * i / (points.size - 1f)
                        val y = bottom - (bottom - top) * ((value - lower) / range).toFloat()
                        return Offset(x, y)
                    }
                    var previous: Offset? = null
                    points.forEachIndexed { index, number ->
                        if (number == null) {
                            previous = null
                        } else {
                            val pos = point(index, number)
                            previous?.let {
                                drawLine(s.color, it, pos, 2.dp.toPx(), cap = StrokeCap.Round)
                            }
                            drawCircle(s.color, radius = 3.dp.toPx(), center = pos)
                            previous = pos
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            tickLabels.forEach { Text(it, color = Slate, fontSize = 10.sp) }
        }
    }
}

@Composable
fun DietScreen(date: LocalDate, meals: List<MealEntry>, onBack: () -> Unit) {
    val score = HealthMath.dietScore(meals)
    PageColumn("营养建议", date, onBack = onBack) {
        Spacer(Modifier.height(10.dp))
        AppCard(background = Color(0xFFDEFAEB)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🥗", fontSize = 61.sp)
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("地中海饮食建议", fontSize = 17.sp, color = DarkGreen,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("多蔬菜水果、鱼类、优质蛋白和植物油，减少肥肉、加工肉、盐和精制糖。",
                        fontSize = 12.sp, color = Ink, lineHeight = 18.sp)
                }
            }
        }
        AppCard {
            Row {
                Column(Modifier.weight(1f)) {
                    DietHeader("✓", "多吃", Green)
                    listOf("蔬菜、水果", "鱼类、海鲜", "全谷物（燕麦、糙米等）", "豆类、坚果",
                        "橄榄油等植物油", "优质蛋白（鸡蛋、鸡肉、豆制品）"
                    ).forEach { DietLine(it, Green) }
                }
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(1.dp).height(190.dp).background(Line))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    DietHeader("✕", "少吃", Coral)
                    listOf("肥肉、动物脂肪", "加工肉（香肠、火腿等）", "高盐食品", "精制淀粉和甜食",
                        "含糖饮料", "油炸食品"
                    ).forEach { DietLine(it, Coral) }
                }
            }
        }
        AppCard {
            SectionHeading("今日饮食评分", "习惯参考")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    score?.toString() ?: "--", fontSize = 32.sp, color = DarkGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(" 分", color = DarkGreen, fontSize = 14.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    ProgressTrack((score ?: 0) / 100f, Green)
                    Spacer(Modifier.height(6.dp))
                    SmallLabel(
                        if (score == null) "先记录三餐，即可查看饮食习惯评分"
                        else "已记录 " + meals.map { it.meal }.distinct().size + "/3 餐，继续保持！"
                    )
                }
            }
        }
        SmallLabel("评分为简化的饮食习惯提示，不能评估个人营养充足性；盐摄入量需要结合实际食盐用量判断。")
    }
}

@Composable
private fun DietHeader(icon: String, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.background(color, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun DietLine(text: String, color: Color) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 6.dp).size(5.dp).background(color, RoundedCornerShape(5.dp)))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, color = Slate, lineHeight = 16.sp)
    }
}
