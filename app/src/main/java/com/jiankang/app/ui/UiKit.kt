package com.jiankang.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val Green = Color(0xFF109C60)
val DarkGreen = Color(0xFF087C4B)
val Pale = Color(0xFFF8FBFD)
val SoftGreen = Color(0xFFE2FAEF)
val Ink = Color(0xFF15202D)
val Slate = Color(0xFF7D889B)
val Line = Color(0xFFEDF1F5)
val Blue = Color(0xFF548CEE)
val Orange = Color(0xFFFF942B)
val Violet = Color(0xFF8883E6)
val Coral = Color(0xFFFE6868)

@Composable
fun JiankangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green, onPrimary = Color.White, background = Pale,
            surface = Color.White, onSurface = Ink, outline = Line
        ),
        content = content
    )
}

fun showMessage(context: Context, message: String) =
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

fun openDatePicker(context: Context, date: LocalDate, onSelected: (LocalDate) -> Unit) {
    DatePickerDialog(context, { _, year, month, day ->
        onSelected(LocalDate.of(year, month + 1, day))
    }, date.year, date.monthValue - 1, date.dayOfMonth).show()
}

fun openTimePicker(context: Context, time: String, onSelected: (String) -> Unit) {
    val parsed = try { LocalTime.parse(time) } catch (_: Exception) { LocalTime.of(20, 0) }
    TimePickerDialog(context, { _, h, m ->
        onSelected(String.format(Locale.ROOT, "%02d:%02d", h, m))
    }, parsed.hour, parsed.minute, true).show()
}

fun friendlyDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy年M月d日  EEEE", Locale.CHINA))

@Composable
fun PageColumn(
    title: String,
    date: LocalDate? = null,
    onBack: (() -> Unit)? = null,
    onDate: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(Pale)) {
        AppHeader(title, date, onBack, onDate)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Spacer(Modifier.height(4.dp))
                content()
                Spacer(Modifier.height(20.dp))
            }
        )
    }
}

@Composable
fun AppHeader(
    title: String, date: LocalDate?, onBack: (() -> Unit)?, onDate: (() -> Unit)?
) {
    Row(
        Modifier.fillMaxWidth().height(if (date == null) 62.dp else 80.dp).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(44.dp), contentAlignment = Alignment.CenterStart) {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "返回", tint = Ink)
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (date != null) {
                Spacer(Modifier.height(3.dp))
                Text(friendlyDate(date), color = Slate, fontSize = 11.sp)
            }
        }
        Box(Modifier.width(44.dp), contentAlignment = Alignment.CenterEnd) {
            if (onDate != null) IconButton(onClick = onDate, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "选择日期", tint = Ink)
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    background: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = background,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun PrimaryButton(text: String = "保存", enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Green)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun ColorIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
    }
}

@Composable
fun SmallLabel(text: String, color: Color = Slate) {
    Text(text, fontSize = 12.sp, color = color)
}

@Composable
fun SectionHeading(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 15.sp, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontSize = 12.sp, color = Slate)
    }
}

@Composable
fun ProgressTrack(progress: Float, color: Color = Green) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
        color = color,
        trackColor = Line
    )
}

@Composable
fun MetricValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Slate, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = Ink)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = Ink)
            if (subtitle != null) Text(subtitle, color = Slate, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun JournalBottomBar(page: Page, go: (Page) -> Unit) {
    Surface(color = Color.White, shadowElevation = 6.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(59.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem("首页", Icons.Outlined.Home, page == Page.HOME, Modifier.weight(1f)) { go(Page.HOME) }
            BottomItem("记录", Icons.Outlined.Assignment, page == Page.RECORD, Modifier.weight(1f)) { go(Page.RECORD) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Green).clickable { go(Page.RECORD) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "新建记录", tint = Color.White)
                }
            }
            BottomItem("数据", Icons.Outlined.BarChart, page == Page.STATS, Modifier.weight(1f)) { go(Page.STATS) }
            BottomItem("我的", Icons.Outlined.Person, page == Page.PROFILE, Modifier.weight(1f)) { go(Page.PROFILE) }
        }
    }
}

@Composable
private fun BottomItem(
    title: String, icon: ImageVector, selected: Boolean, modifier: Modifier, click: () -> Unit
) {
    Column(
        modifier.fillMaxHeight().clickable(onClick = click),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, title, modifier = Modifier.size(22.dp), tint = if (selected) Green else Slate)
        Text(title, fontSize = 10.sp, color = if (selected) Green else Slate)
    }
}

@Composable
fun Tag(text: String, background: Color = SoftGreen, tint: Color = DarkGreen) {
    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(background).padding(horizontal = 9.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.sp, color = tint, fontWeight = FontWeight.SemiBold)
    }
}
