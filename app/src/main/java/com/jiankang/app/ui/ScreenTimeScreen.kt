package com.jiankang.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.jiankang.app.data.AppSettings
import com.jiankang.app.screentime.ScreenLimitScheduler
import com.jiankang.app.screentime.ScreenTimeReader
import com.jiankang.app.screentime.ScreenUsage
import java.time.LocalDate

@Composable
fun ScreenTimeSummaryCard(
    date: LocalDate, usage: ScreenUsage?, settings: AppSettings, onClick: () -> Unit
) {
    val goal = settings.screenGoalMinutes
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorIcon(Icons.Default.PhoneAndroid, Blue)
            Spacer(Modifier.width(12.dp))
            Text("手机亮屏时间", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Slate)
        }
        Spacer(Modifier.height(10.dp))
        if (usage == null) {
            SmallLabel(
                if (!ScreenTimeReader.supported()) "亮屏统计需 Android 9 或更新系统"
                else if (!ScreenTimeReader.hasUsageAccess(androidx.compose.ui.platform.LocalContext.current))
                    "点此授权读取设备使用情况"
                else "暂无可用亮屏记录（历史数据可能已被系统清理）"
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text((usage.minutes / 60).toString() + " 小时 " +
                    (usage.minutes % 60).toString() + " 分钟", fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("目标 " + (goal / 60) + " 小时 " + (goal % 60) + " 分钟",
                    color = Slate, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            ProgressTrack(usage.minutes.toFloat() / goal, if (usage.minutes >= goal) Coral else Green)
            if (usage.partial) {
                Spacer(Modifier.height(6.dp))
                SmallLabel("系统事件不完整，当前为已观测时长（可能偏低）")
            } else if (usage.minutes >= goal) {
                Spacer(Modifier.height(6.dp))
                Text("已达到今日上限，建议放下手机休息一下。", color = Coral, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ScreenTimeScreen(
    date: LocalDate, usage: ScreenUsage?, settings: AppSettings,
    onBack: () -> Unit, onChanged: () -> Unit
) {
    val context = LocalContext.current
    var limit by remember { mutableStateOf(settings.screenGoalMinutes.toString()) }
    var mode by remember { mutableStateOf(settings.screenLimitMode) }
    var alerts by remember { mutableStateOf(settings.screenAlertEnabled) }
    val authorized = ScreenTimeReader.hasUsageAccess(context)
    val usageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onChanged() }
    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && ScreenTimeReader.hasUsageAccess(context)) {
            settings.screenAlertEnabled = true
            alerts = true
            ScreenLimitScheduler.schedule(context, settings)
            onChanged()
        } else showMessage(context, "未开启通知：需要通知权限和使用情况访问权限")
    }
    PageColumn("手机使用时间", date, onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        AppCard {
            SectionHeading("今日亮屏时长", "目标：" + (settings.screenGoalMinutes / 60) + "小时")
            Spacer(Modifier.height(8.dp))
            Text(
                if (usage == null) "--" else (usage.minutes / 60).toString() + "小时" +
                    (usage.minutes % 60).toString() + "分钟",
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink
            )
            Spacer(Modifier.height(7.dp))
            ProgressTrack((usage?.minutes ?: 0).toFloat() / settings.screenGoalMinutes,
                if (usage != null && usage.minutes >= settings.screenGoalMinutes) Coral else Green)
            Spacer(Modifier.height(8.dp))
            if (usage?.partial == true) SmallLabel("系统事件不完整，统计可能偏低。")
            else if (usage == null) SmallLabel(
                if (authorized) "暂无记录或系统尚未提供亮屏事件。" else "需要手动授权“使用情况访问权限”后才能统计。"
            )
            else SmallLabel("统计交互亮屏状态，不等于手机解锁次数；可能与系统数字健康统计略有差异。")
        }
        AppCard {
            SectionHeading("每日时长上限")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(180, 240, 300, 360).forEach { item ->
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(
                                if (limit.toIntOrNull() == item) SoftGreen else Pale,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { limit = item.toString() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((item / 60).toString() + "小时", color = DarkGreen, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(
                value = limit,
                onValueChange = { limit = it.filter(Char::isDigit).take(4) },
                label = { Text("自定义每日上限（分钟）") },
                supportingText = { Text("允许30–1440分钟；默认300分钟（5小时）", fontSize = 11.sp) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(6.dp))
            PrimaryButton("保存每日上限") {
                val number = limit.toIntOrNull()
                if (number == null || number !in 30..1440) {
                    showMessage(context, "请设定30–1440分钟")
                } else {
                    settings.screenGoalMinutes = number
                    ScreenLimitScheduler.schedule(context, settings)
                    onChanged()
                    showMessage(context, "已保存")
                }
            }
        }
        AppCard {
            SectionHeading("达到上限以后")
            RadioButtonRow(
                "提醒模式",
                "80%和100%时提醒，不影响解锁与应用使用。",
                mode == "remind"
            ) {
                mode = "remind"; settings.screenLimitMode = mode; onChanged()
            }
            HorizontalDivider(color = Line)
            RadioButtonRow(
                "系统协同模式（推荐）",
                "APP提醒 + 你在系统数字健康中设置娱乐应用计时器和专注模式。",
                mode == "focus"
            ) {
                mode = "focus"; settings.screenLimitMode = mode; onChanged()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "普通应用无法强制锁机，也不能静默关闭其他应用。严格限制需设备管理权限或系统功能；电话、急救、导航等必要使用应始终保留。",
                color = Slate, fontSize = 11.sp, lineHeight = 17.sp
            )
        }
        AppCard {
            SectionHeading("权限与提醒")
            if (!ScreenTimeReader.supported()) {
                SmallLabel("Android 8 设备无法取得系统交互亮屏事件；本功能需要 Android 9 及以上版本。")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (authorized) "使用情况访问：已授权" else "使用情况访问：未授权",
                        fontSize = 13.sp, color = if (authorized) DarkGreen else Slate,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) { Text(if (authorized) "管理" else "去授权") }
                }
                HorizontalDivider(color = Line)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("每日阈值提醒", color = Ink, fontSize = 14.sp)
                        SmallLabel("系统约每半小时检查一次；可能延迟，不会强制锁屏。")
                    }
                    Switch(checked = alerts, onCheckedChange = { checked ->
                        if (!checked) {
                            alerts = false
                            settings.screenAlertEnabled = false
                            ScreenLimitScheduler.schedule(context, settings)
                            onChanged()
                        } else if (!authorized) {
                            showMessage(context, "请先授权使用情况访问权限")
                            usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        } else if (Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            alerts = true
                            settings.screenAlertEnabled = true
                            ScreenLimitScheduler.schedule(context, settings)
                            onChanged()
                        }
                    })
                }
            }
        }
        if (mode == "focus") {
            AppCard(background = Color(0xFFE8F7EF)) {
                SectionHeading("设置系统级娱乐应用限制")
                Spacer(Modifier.height(7.dp))
                Text(
                    "依次打开：手机设置 → 数字健康／屏幕使用时间 → 应用计时器／专注模式。自行选择短视频、游戏和社交应用，并设置单独使用额度。",
                    fontSize = 12.sp, color = Ink, lineHeight = 19.sp
                )
                Spacer(Modifier.height(7.dp))
                PrimaryButton("打开系统设置") {
                    try { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    catch (_: Exception) { showMessage(context, "请手动打开手机系统设置") }
                }
                Spacer(Modifier.height(8.dp))
                SmallLabel("具体名称因手机品牌而异；APP不会自动修改系统数字健康设置。")
            }
        }
        SmallLabel("亮屏统计只在你授予权限后读取，并在本机计算，不上传应用使用明细。")
    }
}

@Composable
private fun RadioButtonRow(
    title: String, description: String, selected: Boolean, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(description, fontSize = 12.sp, color = Slate, lineHeight = 17.sp)
        }
    }
}
