package com.jiankang.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankang.app.model.DayRecord
import java.time.LocalDate

private val moods = listOf("开心", "平静", "感恩", "疲惫", "焦虑", "低落")

@Composable
fun ReflectionScreen(date: LocalDate, day: DayRecord, onBack: () -> Unit, save: (DayRecord) -> Unit) {
    var journal by remember(date, day) { mutableStateOf(day.reflection.orEmpty()) }
    var mood by remember(date, day) { mutableStateOf(day.mood) }
    PageColumn("感悟", date, onBack = onBack) {
        Spacer(Modifier.height(10.dp))
        AppCard {
            Row {
                Icon(Icons.Default.AutoStories, null, tint = Violet)
                Spacer(Modifier.width(8.dp))
                Text("今天有什么值得记下？", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
            }
            Spacer(Modifier.height(12.dp))
            Text("今日心情（可选）", color = Slate, fontSize = 13.sp)
            Spacer(Modifier.height(9.dp))
            moods.chunked(3).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.forEach { option ->
                        MoodChip(option, mood == option, Modifier.weight(1f)) {
                            mood = if (mood == option) null else option
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = journal, onValueChange = { journal = it.take(8000) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                placeholder = { Text("写下今天的收获、心情、思考或值得感恩的事情……", fontSize = 13.sp) },
                minLines = 8, shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet, unfocusedBorderColor = Line
                )
            )
            Spacer(Modifier.height(7.dp))
            Text(journal.length.toString() + "/8000", color = Slate, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Spacer(Modifier.height(12.dp))
            PrimaryButton { save(day.copy(reflection = journal.trim().ifBlank { null }, mood = mood)) }
        }
        SmallLabel("记录完全自愿。感悟和心情不会上传，也不用于心理诊断。")
    }
}

@Composable
private fun MoodChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFFF0EAFD) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Violet else Line)
    ) { Text(label, color = if (selected) Violet else Ink, fontSize = 12.sp) }
}
