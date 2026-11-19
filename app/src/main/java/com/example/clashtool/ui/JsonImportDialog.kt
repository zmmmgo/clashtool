package com.example.clashtool.ui

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.clashtool.parser.ClashJsonParser
import com.example.clashtool.parser.ParsedVillageData
import com.example.clashtool.ui.theme.*

@Composable
fun JsonImportDialog(
    currentAccountId: String,
    currentAccountName: String,
    onDismiss: () -> Unit,
    onImportSuccess: (ParsedVillageData) -> Unit
) {
    val context = LocalContext.current
    var jsonText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 自动检测剪贴板是否已有 JSON
    LaunchedEffect(Unit) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (item != null && item.trim().startsWith("{") && item.contains("tag")) {
                    jsonText = item.trim()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📋", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text(
                            text = "导入游戏内村庄数据",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "全自动读取所有正在升级的工程与剩余时间",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldPrimary
                        )
                    }
                }

                // 操作指引说明
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 如何在游戏里获取这段数据：",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. 打开《部落冲突》游戏 ➔ 点击「设置（齿轮）」\n" +
                                   "2. 点击「更多设置 (More Settings)」\n" +
                                   "3. 滑到底部找到「Data Export」\n" +
                                   "4. 点击「Export Village data in JSON format」旁的 Copy 按钮！",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // 剪贴板快速粘贴按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "村庄 JSON 数据：",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    TextButton(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    jsonText = clip.trim()
                                    errorMessage = null
                                    Toast.makeText(context, "已粘贴剪贴板内容！", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "剪贴板为空，请先在游戏内复制！", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "读取剪贴板失败，请长按手动粘贴", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("📋 读取剪贴板", color = GoldPrimary, fontSize = 12.sp)
                    }
                }

                // JSON 文本框
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        errorMessage = null
                    },
                    placeholder = { Text("在此粘贴游戏导出的 JSON (例如以 {\"tag\":... 开头)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = DangerRed,
                        fontSize = 12.sp
                    )
                }

                HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                // 底部按钮
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
                            if (jsonText.isBlank()) {
                                errorMessage = "请先粘贴游戏内导出的 JSON 内容！"
                                return@Button
                            }
                            val result = ClashJsonParser.parseVillageJson(
                                jsonString = jsonText,
                                accountId = currentAccountId,
                                accountName = currentAccountName
                            )
                            result.onSuccess { parsed ->
                                onImportSuccess(parsed)
                            }.onFailure { err ->
                                errorMessage = "解析失败：请确保是完整的游戏导出 JSON（${err.message}）"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("🚀 一键同步所有倒计时", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
