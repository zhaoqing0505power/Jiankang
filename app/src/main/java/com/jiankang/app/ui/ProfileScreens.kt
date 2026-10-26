package com.jiankang.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.jiankang.app.data.AppSettings
import com.jiankang.app.data.HealthDatabase
import com.jiankang.app.model.FoodCatalog
import com.jiankang.app.notifications.ReminderScheduler
import com.jiankang.app.notifications.WorkdayBreakReminder
import com.jiankang.app.notifications.WorkdayBreakTimes
import java.time.LocalDate
import org.json.JSONObject

@Composable
fun ProfileScreen(date: LocalDate, settings: AppSettings, go: (Page) -> Unit) {
    PageColumn("我的") {
        Spacer(Modifier.height(10.dp))
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorIcon(Icons.Default.Bedtime, Blue)
                Spacer(Modifier.width(10.dp))
                Text("健康目标", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            GoalLine("☾", "睡眠", "≥ " + settings.sleepGoalHours + " 小时/天")
            GoalLine("🥗", "饮食", "地中海饮食")
            GoalLine("🏃", "运动", "≥ " + settings.weeklyExerciseGoal + " 分钟/周")
            Spacer(Modifier.height(7.dp))
            TextButton(onClick = { go(Page.GOALS) }, modifier = Modifier.align(Alignment.End)) {
                Text("调整目标", fontSize = 12.sp, color = Green)
            }
        }
        AppCard {
            SettingRow(Icons.Outlined.PersonOutline, "个人信息") { go(Page.BODY) }
            HorizontalDivider(color = Line)
            SettingRow(Icons.Outlined.Restaurant, "常用食物") { go(Page.FAVORITES) }
            HorizontalDivider(color = Line)
            SettingRow(Icons.Outlined.DirectionsRun, "运动类型") { go(Page.SPORTS) }
            HorizontalDivider(color = Line)
            SettingRow(Icons.Outlined.NotificationsNone, "提醒设置", subtitle = "每日记录提醒") { go(Page.REMINDERS) }
            HorizontalDivider(color = Line)
            SettingRow(Icons.Outlined.PhoneAndroid, "手机使用时间", subtitle = "每日限额、提醒和系统专注模式") { go(Page.SCREEN_TIME) }
            HorizontalDivider(color = Line)
            SettingRow(Icons.Outlined.FileDownload, "数据备份与导出") { go(Page.BACKUP) }
            HorizontalDivider(color = Line)
            SettingRow(Icons.Outlined.Info, "关于我们") { go(Page.ABOUT) }
        }
        AppCard {
            SettingRow(Icons.Outlined.Spa, "营养建议", subtitle = "地中海饮食原则与今日评分") { go(Page.DIET) }
            HorizontalDivider(color = Line)
            SettingRow(Icons.Outlined.FavoriteBorder, "疾病预防知识", subtitle = "心脑血管、癌症预防与就医知识") { go(Page.HEALTH_GUIDE) }
        }
        SmallLabel("健康日记 · 所有测量数据仅存放在本机")
    }
}

@Composable
private fun GoalLine(emoji: String, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 18.sp, modifier = Modifier.width(36.dp))
        Text(label, fontSize = 12.sp, color = Slate, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, color = Slate)
    }
}

@Composable
fun GoalsScreen(settings: AppSettings, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var sleep by remember { mutableStateOf(settings.sleepGoalHours.toString()) }
    var exercise by remember { mutableStateOf(settings.weeklyExerciseGoal.toString()) }
    PageColumn("健康目标", onBack = onBack) {
        AppCard {
            Text("睡眠目标", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(sleep, { sleep = it.filter(Char::isDigit).take(2) }, suffix = { Text("小时/天") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(12.dp))
            Text("运动目标", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(exercise, { exercise = it.filter(Char::isDigit).take(4) },
                suffix = { Text("分钟/周") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(15.dp))
            PrimaryButton {
                val s = sleep.toIntOrNull()
                val e = exercise.toIntOrNull()
                if (s == null || s !in 1..16 || e == null || e !in 1..1000) {
                    showMessage(context, "睡眠目标1–16小时，运动目标1–1000分钟")
                } else {
                    settings.sleepGoalHours = s
                    settings.weeklyExerciseGoal = e
                    onSaved()
                }
            }
        }
        SmallLabel("默认目标：每天睡眠8小时，运动每周150分钟。")
    }
}

@Composable
fun FavoritesScreen(db: HealthDatabase, settings: AppSettings, onBack: () -> Unit, go: (Page) -> Unit) {
    var favorites by remember { mutableStateOf(settings.favorites()) }
    val previous = remember { db.allMeals().associateBy { it.name } }
    PageColumn("常用食物", onBack = onBack) {
        AppCard {
            SectionHeading("已收藏的食物", favorites.size.toString() + " 项")
            if (favorites.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                SmallLabel("暂无收藏，记录三餐时点亮食物旁边的星标即可添加。")
            } else {
                favorites.sorted().forEach { name ->
                    HorizontalDivider(color = Line)
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(FoodCatalog.all.firstOrNull { it.name == name }?.emoji
                            ?: previous[name]?.emoji ?: "🍽️", fontSize = 23.sp)
                        Spacer(Modifier.width(13.dp))
                        Text(name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { settings.toggleFavorite(name); favorites = settings.favorites() }) {
                            Icon(Icons.Default.Star, "移除收藏", tint = Orange)
                        }
                    }
                }
            }
        }
        PrimaryButton("去记录食物") { go(Page.BREAKFAST) }
    }
}

@Composable
fun SportsScreen(onBack: () -> Unit) {
    PageColumn("运动类型", onBack = onBack) {
        AppCard {
            listOf(
                "🚶" to "快走", "🏃" to "跑步", "🚴" to "骑行", "🏊" to "游泳",
                "🏋️" to "力量训练", "🧘" to "瑜伽", "⚽" to "球类", "👟" to "其他"
            ).forEach { (emoji, label) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 23.sp)
                    Spacer(Modifier.width(16.dp))
                    Text(label, fontSize = 15.sp, color = Ink)
                }
                HorizontalDivider(color = Line)
            }
        }
    }
}

@Composable
fun RemindersScreen(settings: AppSettings, onBack: () -> Unit, settingsChanged: () -> Unit) {
    val context = LocalContext.current
    var dailyEnabled by remember { mutableStateOf(settings.reminderEnabled) }
    var selectedTime by remember {
        mutableStateOf(String.format(java.util.Locale.ROOT, "%02d:%02d",
            settings.reminderHour, settings.reminderMinute))
    }
    var breaksEnabled by remember { mutableStateOf(settings.breakReminderEnabled) }
    fun formatTime(minutes: Int) = String.format(
        java.util.Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60
    )
    var breakStart by remember { mutableStateOf(formatTime(settings.breakStartMinutes)) }
    var breakEnd by remember { mutableStateOf(formatTime(settings.breakEndMinutes)) }

    val dailyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            dailyEnabled = true
            settings.reminderEnabled = true
            ReminderScheduler.schedule(context, settings)
            settingsChanged()
        } else showMessage(context, "没有通知权限，日记通知未开启")
    }
    val breakPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Notification permission is optional for vibration; user explicitly enabled breaks.
        breaksEnabled = true
        settings.breakReminderEnabled = true
        WorkdayBreakReminder.schedule(context, settings)
        settingsChanged()
        if (!granted) showMessage(context, "已开启振动提醒，但不会显示系统通知")
    }

    fun applyBreakWindow(newStart: String, newEnd: String) {
        val from = java.time.LocalTime.parse(newStart)
        val until = java.time.LocalTime.parse(newEnd)
        if (!WorkdayBreakTimes.validWindow(from, until)) {
            showMessage(context, "请设置同一天内超过1小时的工作时段")
            return
        }
        breakStart = newStart
        breakEnd = newEnd
        settings.breakStartMinutes = from.hour * 60 + from.minute
        settings.breakEndMinutes = until.hour * 60 + until.minute
        WorkdayBreakReminder.schedule(context, settings)
        settingsChanged()
    }

    PageColumn("提醒设置", onBack = onBack) {
        AppCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("每天提醒记录", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    SmallLabel("每天提醒一次填写健康日记")
                }
                Switch(checked = dailyEnabled, onCheckedChange = { checked ->
                    if (checked && Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        dailyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        dailyEnabled = checked
                        settings.reminderEnabled = checked
                        ReminderScheduler.schedule(context, settings)
                        settingsChanged()
                    }
                })
            }
            HorizontalDivider(color = Line)
            Row(
                Modifier.fillMaxWidth().clickable {
                    openTimePicker(context, selectedTime) { next ->
                        selectedTime = next
                        val parts = next.split(":")
                        settings.reminderHour = parts[0].toInt()
                        settings.reminderMinute = parts[1].toInt()
                        ReminderScheduler.schedule(context, settings)
                        settingsChanged()
                    }
                }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("提醒时间", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(selectedTime, color = Green, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Schedule, "选择时间", tint = Slate)
            }
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorIcon(Icons.Default.AccessibilityNew, Violet)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("工作日久坐振动提醒", fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = Ink)
                    SmallLabel("周一至周五，每坐满1小时提醒起身活动")
                }
                Switch(checked = breaksEnabled, onCheckedChange = { checked ->
                    if (!checked) {
                        breaksEnabled = false
                        settings.breakReminderEnabled = false
                        WorkdayBreakReminder.schedule(context, settings)
                        settingsChanged()
                    } else if (Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        breakPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        breaksEnabled = true
                        settings.breakReminderEnabled = true
                        WorkdayBreakReminder.schedule(context, settings)
                        settingsChanged()
                    }
                })
            }
            Spacer(Modifier.height(9.dp))
            HorizontalDivider(color = Line)
            Row(
                Modifier.fillMaxWidth().clickable {
                    openTimePicker(context, breakStart) { applyBreakWindow(it, breakEnd) }
                }.padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("工作开始", fontSize = 14.sp, modifier = Modifier.weight(1f), color = Ink)
                Text(breakStart, color = Green, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Schedule, "设置开始时间", tint = Slate)
            }
            HorizontalDivider(color = Line)
            Row(
                Modifier.fillMaxWidth().clickable {
                    openTimePicker(context, breakEnd) { applyBreakWindow(breakStart, it) }
                }.padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("工作结束", fontSize = 14.sp, modifier = Modifier.weight(1f), color = Ink)
                Text(breakEnd, color = Green, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Schedule, "设置结束时间", tint = Slate)
            }
            Spacer(Modifier.height(9.dp))
            val startMinutes = settings.breakStartMinutes
            val endMinutes = settings.breakEndMinutes
            val first = startMinutes + 60
            val last = startMinutes + 60 * ((endMinutes - startMinutes - 1) / 60)
            SmallLabel(
                "提醒时段：" + formatTime(first) + "—" + formatTime(last) +
                    "（每小时，工作结束后不提醒）"
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = {
                WorkdayBreakReminder.testVibration(context)
                showMessage(context, "已发送测试振动（以手机振动设置为准）")
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Vibration, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("测试振动")
            }
        }
        SmallLabel("久坐振动提醒默认关闭。系统省电、免打扰或静音模式可能延迟或抑制振动；通知权限被拒绝时仍可尝试振动，但不会弹出通知。不要求精确闹钟权限。")
    }
}

@Composable
fun BackupScreen(
    db: HealthDatabase, settings: AppSettings, onBack: () -> Unit, afterImport: () -> Unit
) {
    val context = LocalContext.current
    var askImport by remember { mutableStateOf(false) }
    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) try {
            val json = JSONObject(db.exportJson())
            json.put("settings", JSONObject().apply {
                put("sleepGoalHours", settings.sleepGoalHours)
                put("weeklyExerciseGoal", settings.weeklyExerciseGoal)
                put("screenGoalMinutes", settings.screenGoalMinutes)
                put("screenLimitMode", settings.screenLimitMode)
                put("screenAlertEnabled", settings.screenAlertEnabled)
                put("breakStartMinutes", settings.breakStartMinutes)
                put("breakEndMinutes", settings.breakEndMinutes)
                put("favorites", org.json.JSONArray(settings.favorites().sorted()))
            })
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(json.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("无法打开目标文件")
            showMessage(context, "备份已导出")
        } catch (e: Exception) { showMessage(context, "导出失败：" + e.message) }
    }
    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) try {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(db.exportCsv().toByteArray(Charsets.UTF_8))
            } ?: error("无法打开目标文件")
            showMessage(context, "CSV已导出")
        } catch (e: Exception) { showMessage(context, "导出失败：" + e.message) }
    }
    val importJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                val result = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    if (result.size() + n > 12_000_000) error("备份文件过大")
                    result.write(buffer, 0, n)
                }
                result.toByteArray()
            } ?: error("无法打开备份")
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            db.importJson(json.toString())
            val opt = json.optJSONObject("settings")
            if (opt != null) {
                settings.sleepGoalHours = opt.optInt("sleepGoalHours", settings.sleepGoalHours)
                settings.weeklyExerciseGoal = opt.optInt("weeklyExerciseGoal", settings.weeklyExerciseGoal)
                settings.screenGoalMinutes = opt.optInt("screenGoalMinutes", settings.screenGoalMinutes)
                settings.screenLimitMode = opt.optString("screenLimitMode", settings.screenLimitMode)
                    .takeIf { it == "remind" || it == "focus" } ?: "focus"
                // Imported alert settings are disabled until the user grants permissions again.
                settings.screenAlertEnabled = false
                com.jiankang.app.screentime.ScreenLimitScheduler.schedule(context, settings)
                val restoredBreakStart = opt.optInt("breakStartMinutes", settings.breakStartMinutes).coerceIn(0, 1439)
                val restoredBreakEnd = opt.optInt("breakEndMinutes", settings.breakEndMinutes).coerceIn(0, 1439)
                if (WorkdayBreakTimes.validWindow(
                        java.time.LocalTime.of(restoredBreakStart / 60, restoredBreakStart % 60),
                        java.time.LocalTime.of(restoredBreakEnd / 60, restoredBreakEnd % 60))) {
                    settings.breakStartMinutes = restoredBreakStart
                    settings.breakEndMinutes = restoredBreakEnd
                }
                settings.breakReminderEnabled = false
                WorkdayBreakReminder.schedule(context, settings)
                val arr = opt.optJSONArray("favorites")
                if (arr != null) {
                    val desired = (0 until arr.length()).map { arr.optString(it) }.toSet()
                    (settings.favorites() - desired).forEach(settings::toggleFavorite)
                    (desired - settings.favorites()).forEach(settings::toggleFavorite)
                }
            }
            showMessage(context, "数据已恢复")
            afterImport()
        } catch (e: Exception) { showMessage(context, "导入失败：" + e.message) }
    }
    PageColumn("数据备份与导出", onBack = onBack) {
        AppCard {
            SectionHeading("备份健康日记")
            Spacer(Modifier.height(8.dp))
            SmallLabel("导出JSON备份，可在换手机后导入，包含体征、三餐、睡眠、运动、感悟及目标。")
            Spacer(Modifier.height(12.dp))
            PrimaryButton("导出完整备份（JSON）") {
                exportJson.launch("Jiankang-backup-" + LocalDate.now() + ".json")
            }
        }
        AppCard {
            SectionHeading("导出表格")
            Spacer(Modifier.height(8.dp))
            SmallLabel("CSV包含每日测量、心情及感悟，可用Excel打开。")
            Spacer(Modifier.height(12.dp))
            PrimaryButton("导出CSV表格") {
                exportCsv.launch("Jiankang-records-" + LocalDate.now() + ".csv")
            }
        }
        AppCard {
            SectionHeading("恢复备份")
            Spacer(Modifier.height(8.dp))
            Text("导入将替换当前手机内的全部日记记录。建议先导出一份备份。",
                color = Slate, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { askImport = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("从JSON备份恢复")
            }
        }
        SmallLabel("隐私提醒：备份文件包含个人健康信息，请保管好文件并避免公开分享。应用不将数据上传服务器。")
    }
    if (askImport) {
        AlertDialog(
            onDismissRequest = { askImport = false },
            title = { Text("确认恢复备份？") },
            text = { Text("恢复会清除当前全部日记记录。此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    askImport = false
                    importJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { Text("选择备份并恢复") }
            },
            dismissButton = { TextButton(onClick = { askImport = false }) { Text("取消") } }
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    PageColumn("关于我们", onBack = onBack) {
        AppCard {
            Text("🍃 健康日记", fontSize = 24.sp, color = DarkGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Text("版本 1.1.0", fontSize = 13.sp, color = Slate)
            Spacer(Modifier.height(12.dp))
            Text("让每天的饮食、起居和运动记录更简单。", color = Ink)
            Spacer(Modifier.height(8.dp))
            SmallLabel("数据默认保存在设备本地。不需要账号或网络权限。")
            Spacer(Modifier.height(8.dp))
            SmallLabel("饮食热量、活动消耗、BMI分组和习惯评分仅供健康管理参考，不代替专业评估。")
        }
    }
}
