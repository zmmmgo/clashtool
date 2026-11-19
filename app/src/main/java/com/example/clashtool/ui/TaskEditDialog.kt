package com.example.clashtool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.clashtool.data.ClashTask
import com.example.clashtool.data.TaskCategory
import com.example.clashtool.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskEditDialog(
    task: ClashTask,
    onDismiss: () -> Unit,
    onConfirm: (targetName: String, durationMs: Long) -> Unit
) {
    var targetName by remember { mutableStateOf(task.targetName.ifEmpty { getDefaultPresetName(task.category) }) }
    var daysText by remember {
        val d = if (task.isRunning) (task.remainingMs / 86400000L).toString() else "1"
        mutableStateOf(d)
    }
    var hoursText by remember {
        val h = if (task.isRunning) ((task.remainingMs % 86400000L) / 3600000L).toString() else "0"
        mutableStateOf(h)
    }
    var minutesText by remember {
        val m = if (task.isRunning) ((task.remainingMs % 3600000L) / 60000L).toString() else "0"
        mutableStateOf(m)
    }

    val days = daysText.toLongOrNull() ?: 0L
    val hours = hoursText.toLongOrNull() ?: 0L
    val minutes = minutesText.toLongOrNull() ?: 0L
    val totalDurationMs = (days * 86400 + hours * 3600 + minutes * 60) * 1000L
    val previewEndTimeMs = System.currentTimeMillis() + totalDurationMs

    val presets = getPresets(task.category)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.category.icon,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = "设置升级任务",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = task.slotName,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldPrimary
                        )
                    }
                }

                // 升级项目输入
                OutlinedTextField(
                    value = targetName,
                    onValueChange = { targetName = it },
                    label = { Text("升级项目名称") },
                    placeholder = { Text("如：大本营、加农炮、野蛮人之王") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // 快捷标签
                Text(
                    text = "常见预设项目：",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (targetName == preset) GoldPrimary.copy(alpha = 0.2f) else DarkCard)
                                .clickable { targetName = preset }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 12.sp,
                                color = if (targetName == preset) GoldPrimary else TextPrimary
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                // 时间输入：天、小时、分钟
                Text(
                    text = "升级所需时长：",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeInputField(
                        value = daysText,
                        onValueChange = { daysText = it.filter { c -> c.isDigit() }.take(2) },
                        unit = "天",
                        modifier = Modifier.weight(1f)
                    )
                    TimeInputField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter { c -> c.isDigit() }.take(2) },
                        unit = "小时",
                        modifier = Modifier.weight(1f)
                    )
                    TimeInputField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() }.take(2) },
                        unit = "分钟",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 快捷加时按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickAddButton("+1小时") {
                        val currentH = hoursText.toIntOrNull() ?: 0
                        hoursText = (currentH + 1).toString()
                    }
                    QuickAddButton("+1天") {
                        val currentD = daysText.toIntOrNull() ?: 0
                        daysText = (currentD + 1).toString()
                    }
                    QuickAddButton("+3天") {
                        val currentD = daysText.toIntOrNull() ?: 0
                        daysText = (currentD + 3).toString()
                    }
                    QuickAddButton("+7天") {
                        val currentD = daysText.toIntOrNull() ?: 0
                        daysText = (currentD + 7).toString()
                    }
                    QuickAddButton("+14天") {
                        val currentD = daysText.toIntOrNull() ?: 0
                        daysText = (currentD + 14).toString()
                    }
                }

                // 预计完成时间预览卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🔔 预计完成时间提醒：",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm (E)", Locale.CHINA)
                        Text(
                            text = if (totalDurationMs > 0) sdf.format(Date(previewEndTimeMs)) else "未设置时长",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalDurationMs > 0) GemGreen else TextSecondary
                        )
                    }
                }

                // 底部操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (totalDurationMs > 0) {
                                onConfirm(targetName.trim().ifEmpty { "升级工程" }, totalDurationMs)
                            }
                        },
                        enabled = totalDurationMs > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("开启计时提醒", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TimeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(unit) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = DarkCardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
fun RowScope.QuickAddButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
    ) {
        Text(text = label, fontSize = 11.sp, color = TextPrimary)
    }
}

private fun getDefaultPresetName(category: TaskCategory): String {
    return when (category) {
        TaskCategory.HOME_BUILDER -> "加农炮"
        TaskCategory.LABORATORY -> "野蛮人研究"
        TaskCategory.HERO_PET -> "野蛮人之王"
        TaskCategory.BUILDER_BASE -> "特斯拉电磁塔"
    }
}

private fun getPresets(category: TaskCategory): List<String> {
    return when (category) {
        TaskCategory.HOME_BUILDER -> listOf("大本营", "加农炮", "箭塔", "跳弹加农炮", "多人箭塔", "地狱之塔", "X连弩", "天鹰火炮", "投石炮", "巨石碑", "法术塔", "法师塔", "城墙")
        TaskCategory.LABORATORY -> listOf("野蛮人", "弓箭手", "隐秘哥布林", "雷电飞龙", "大雪怪", "根蔓骑士", "狂暴法术", "疗伤法术", "冰冻法术", "蔓生法术")
        TaskCategory.HERO_PET -> listOf("野蛮人之王", "弓箭女皇", "大守护者", "飞盾战神", "亡灵王子", "飞龙公爵", "莱希", "闪枭", "大牦", "独角", "冰牙", "地兽", "猛蜥", "凤凰", "灵狐", "愤怒水母", "阿啾", "贪婪渡鸦")
        TaskCategory.BUILDER_BASE -> listOf("建筑大师大本营", "双管加农炮", "防空火炮", "撼地巨石", "巨型加农炮", "超级特斯拉电磁塔", "熔岩火炮", "雷霆皮卡")
    }
}
