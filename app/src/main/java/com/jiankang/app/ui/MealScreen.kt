package com.jiankang.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.imageResource
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankang.app.data.AppSettings
import com.jiankang.app.data.HealthDatabase
import com.jiankang.app.model.FoodCatalog
import com.jiankang.app.model.FoodOption
import com.jiankang.app.model.MealEntry
import com.jiankang.app.model.MealType
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun MealScreen(
    date: LocalDate, meal: MealType,
    db: HealthDatabase, settings: AppSettings,
    onBack: () -> Unit, onSave: () -> Unit
) {
    val context = LocalContext.current
    val selected = remember(date, meal) {
        mutableStateListOf<MealEntry>().apply { addAll(db.meals(date.toString(), meal)) }
    }
    var search by remember(date, meal) { mutableStateOf("") }
    var favoritesMode by remember(date, meal) { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(settings.favorites()) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var customDialog by remember { mutableStateOf(false) }
    val savedCustom = remember(date, meal) {
        db.allMeals().distinctBy { it.name }.filter { saved ->
            FoodCatalog.all.none { it.name == saved.name }
        }.map { saved ->
            FoodOption(
                saved.name, saved.emoji, saved.group,
                (saved.calories * 100.0 / saved.grams.coerceAtLeast(1)).roundToInt(), saved.grams
            )
        }
    }
    val allOptions = FoodCatalog.all + savedCustom
    val shown = if (favoritesMode) allOptions.filter { it.name in favorites }
        else FoodCatalog.recommended(meal)
    val filtered = if (search.isBlank()) shown else allOptions.filter {
        it.name.contains(search.trim(), ignoreCase = true)
    }.distinctBy { it.name }
    val sum = selected.sumOf { it.calories }

    PageColumn(meal.title, date, onBack = onBack) {
        OutlinedTextField(
            value = search, onValueChange = { search = it.take(30) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索食物（如：燕麦、鸡蛋、三文鱼…）", fontSize = 12.sp, color = Slate) },
            leadingIcon = { Icon(Icons.Default.Search, "搜索", tint = Slate) },
            singleLine = true, shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF1F5F8),
                unfocusedContainerColor = Color(0xFFF1F5F8),
                focusedBorderColor = Green, unfocusedBorderColor = Color.Transparent
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterPill("推荐（地中海饮食）", !favoritesMode, Modifier.weight(1f)) { favoritesMode = false; search = "" }
            FilterPill("我的常用", favoritesMode, Modifier.weight(1f)) { favoritesMode = true; search = "" }
        }
        if (filtered.isEmpty()) {
            AppCard {
                Text(if (favoritesMode) "暂无常用食物，可先从推荐食物中收藏。" else "没找到对应食物，可添加自定义食物。", color = Slate)
            }
        } else {
            filtered.chunked(3).forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.forEach { food ->
                        FoodTile(food, Modifier.weight(1f)) {
                            selected.add(food.toEntry(date.toString(), meal))
                            showMessage(context, "已添加：" + food.name)
                        }
                    }
                    repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        TextButton(onClick = { customDialog = true }, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Text("自定义食物", fontSize = 12.sp)
        }
        AppCard {
            SectionHeading("已选择的食物", selected.size.toString() + " 项")
            if (selected.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                SmallLabel("点选上方食物即可添加，点击已选条目可以调整份量。")
            }
            selected.forEachIndexed { idx, item ->
                HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 5.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.emoji, fontSize = 26.sp, modifier = Modifier.width(43.dp))
                    Column(Modifier.weight(1f).clickable { editingIndex = idx }) {
                        Text(item.name, fontSize = 13.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                        SmallLabel(item.grams.toString() + "g", Slate)
                    }
                    Text(item.calories.toString() + " 千卡", fontSize = 12.sp, color = Ink)
                    IconButton(onClick = {
                        settings.toggleFavorite(item.name)
                        favorites = settings.favorites()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (item.name in favorites) Icons.Default.Star else Icons.Default.StarBorder,
                            "收藏常用食物", tint = if (item.name in favorites) Orange else Slate,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(onClick = { selected.removeAt(idx) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, "删除", tint = Slate, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("合计：", color = Ink, fontWeight = FontWeight.Bold)
            Text(sum.toString() + " 千卡", color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    try {
                        db.replaceMeal(date.toString(), meal, selected.toList())
                        onSave()
                    } catch (e: Exception) { showMessage(context, "保存失败：" + (e.message ?: "数据错误")) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.width(110.dp)
            ) {
                Text("保存", color = Color.White)
            }
        }
        SmallLabel("食物热量按常见食品近似计算，请按实际份量调整；此处不构成营养处方。")
    }

    if (editingIndex in selected.indices) {
        EditPortionDialog(
            entry = selected[editingIndex],
            dismiss = { editingIndex = -1 },
            confirm = { changed ->
                selected[editingIndex] = changed
                editingIndex = -1
            }
        )
    }
    if (customDialog) {
        CustomFoodDialog(
            date = date.toString(), meal = meal,
            dismiss = { customDialog = false },
            confirm = { new ->
                selected.add(new)
                customDialog = false
            }
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    Box(
        modifier.background(if (selected) Color(0xFFD8F8E9) else Color(0xFFF1F4F7), RoundedCornerShape(9.dp))
            .clickable(onClick = click).padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) DarkGreen else Ink)
    }
}


private fun foodAtlasIndex(name: String): Int? = when (name) {
    "燕麦粥" -> 0
    "鸡蛋" -> 1
    "希腊酸奶", "奶酪" -> 2
    "水果" -> 3
    "全麦面包" -> 4
    "坚果" -> 5
    "蔬菜沙拉" -> 6
    "三文鱼" -> 7
    "橄榄油" -> 8
    "糙米/全谷物" -> 9
    "蔬菜" -> 10
    "鱼类" -> 11
    "鸡胸肉" -> 11
    "豆类" -> 13
    "西红柿" -> 15
    "绿叶菜" -> 16
    "牛油果" -> 17
    else -> null
}

@Composable
private fun FoodTile(food: FoodOption, modifier: Modifier, click: () -> Unit) {
    Surface(
        modifier = modifier.height(100.dp).clickable(onClick = click),
        color = Color.White, shape = RoundedCornerShape(11.dp), shadowElevation = 1.dp
    ) {
        Column(
            Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.fillMaxWidth().height(55.dp).background(Pale, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val tileIndex = foodAtlasIndex(food.name)
                if (tileIndex != null) {
                    val atlas = androidx.compose.ui.graphics.ImageBitmap.imageResource(
                        com.jiankang.app.R.drawable.food_atlas
                    )
                    androidx.compose.foundation.Canvas(
                        Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    ) {
                        val width = atlas.width / 3
                        val height = atlas.height / 6
                        drawImage(
                            image = atlas,
                            srcOffset = androidx.compose.ui.unit.IntOffset(
                                tileIndex % 3 * width, tileIndex / 3 * height
                            ),
                            srcSize = androidx.compose.ui.unit.IntSize(width, height),
                            dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                            dstSize = androidx.compose.ui.unit.IntSize(
                                size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)
                            )
                        )
                    }
                } else {
                    Text(food.emoji, fontSize = 37.sp)
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                food.name, fontSize = 11.sp, color = Ink, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditPortionDialog(entry: MealEntry, dismiss: () -> Unit, confirm: (MealEntry) -> Unit) {
    val context = LocalContext.current
    var grams by remember(entry) { mutableStateOf(entry.grams.toString()) }
    var calories by remember(entry) { mutableStateOf(entry.calories.toString()) }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("修改份量 · " + entry.name) },
        text = {
            Column {
                OutlinedTextField(
                    grams, { grams = it.filter(Char::isDigit).take(5) },
                    label = { Text("重量（克）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    calories, { calories = it.filter(Char::isDigit).take(6) },
                    label = { Text("估算热量（千卡）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val g = grams.toIntOrNull()
                val k = calories.toIntOrNull()
                if (g == null || g !in 1..10000 || k == null || k !in 0..100000)
                    showMessage(context, "请输入有效重量和热量")
                else confirm(entry.copy(grams = g, calories = k))
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("取消") } }
    )
}

@Composable
private fun CustomFoodDialog(
    date: String, meal: MealType, dismiss: () -> Unit, confirm: (MealEntry) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var kcal by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("other") }
    var expanded by remember { mutableStateOf(false) }
    val groups = listOf(
        "veg" to "蔬菜", "fruit" to "水果", "fish" to "鱼类", "whole" to "全谷物",
        "protein" to "优质蛋白", "oil" to "植物油", "nuts" to "坚果", "dairy" to "乳制品",
        "processed" to "肥肉/加工肉", "sugar" to "精制糖/甜饮料", "other" to "其他"
    )
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("添加自定义食物") },
        text = {
            Column {
                OutlinedTextField(name, { name = it.take(50) }, label = { Text("食物名称") }, singleLine = true)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(grams, { grams = it.filter(Char::isDigit).take(5) },
                    label = { Text("实际食用重量（克）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(kcal, { kcal = it.filter(Char::isDigit).take(6) },
                    label = { Text("实际估算热量（千卡）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(6.dp))
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text("食物类别：" + (groups.firstOrNull { it.first == group }?.second ?: "其他"))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        groups.forEach { (code, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { group = code; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val g = grams.toIntOrNull()
                val c = kcal.toIntOrNull()
                if (name.isBlank() || g == null || g !in 1..10000 || c == null || c !in 0..100000) {
                    showMessage(context, "请填写有效的食物、克数和千卡")
                } else {
                    confirm(MealEntry(
                        date = date, meal = meal.code, name = name.trim(),
                        grams = g, calories = c, group = group, emoji = "🍽️"
                    ))
                }
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("取消") } }
    )
}
