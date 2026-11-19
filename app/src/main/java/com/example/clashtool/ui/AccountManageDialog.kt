package com.example.clashtool.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.clashtool.data.ClashAccount
import com.example.clashtool.network.ClashApiService
import com.example.clashtool.ui.theme.*
import kotlinx.coroutines.launch

private const val DEFAULT_API_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiIsImtpZCI6IjI4YTMxOGY3LTAwMDAtYTFlYi03ZmExLTJjNzQzM2M2Y2NhNSJ9.eyJpc3MiOiJzdXBlcmNlbGwiLCJhdWQiOiJzdXBlcmNlbGw6Z2FtZWFwaSIsImp0aSI6IjIzMjY2MjZjLTBiNDgtNDI4Zi05N2RjLTUwMTM4MGE3ODQzNyIsImlhdCI6MTc5NTA5ODk3Miwic3ViIjoiZGV2ZWxvcGVyLzI5MDI4NjM0LWM1ODEtNGUyMS05ZmZiLTlhNGNjODZiMzQ5YSIsInNjb3BlcyI6WyJjbGFzaCJdLCJsaW1pdHMiOlt7InRpZXIiOiJkZXZlbG9wZXIvc2lsdmVyIiwidHlwZSI6InRocm90dGxpbmcifSx7ImNpZHJzIjpbIjQ1Ljc5LjIxOC43OSIsIjE4Ny44NC4xNTAuNjgiLCI1Mi42Mi4yMDguMTY3IiwiMTEzLjEyMy4yMDAuNTkiXSwidHlwZSI6ImNsaWVudCJ9XX0.h2Y0RHvA9uAnUownuMWnofR5oIJOQ3MCaGjpJs7XEptjSXHX_fXkBj5EI7hfdStbKe504CC6HLuji2BeeArCOw"

@Composable
fun AccountManageDialog(
    accounts: List<ClashAccount>,
    currentAccountId: String,
    onSelectAccount: (ClashAccount) -> Unit,
    onAddAccount: (name: String, tag: String, token: String, thLevel: Int, clan: String) -> Unit,
    onUpdateAccount: (ClashAccount) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAddSection by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var newPlayerTag by remember { mutableStateOf("") }
    var newApiToken by remember { mutableStateOf(DEFAULT_API_TOKEN) }
    var isSyncing by remember { mutableStateOf(false) }

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
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏰", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "账号与村庄管理",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (!showAddSection) {
                        TextButton(onClick = { showAddSection = true }) {
                            Text("+ 新增账号", color = GoldPrimary, fontSize = 13.sp)
                        }
                    }
                }

                Text(
                    text = "支持通过 Supercell 官方 API 一键同步首领资料与大本营等级。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // 新增账号表单
                if (showAddSection) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "添加新村庄账号",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )

                            OutlinedTextField(
                                value = newPlayerTag,
                                onValueChange = { newPlayerTag = it },
                                label = { Text("玩家标签 (#TAG)") },
                                placeholder = { Text("如：#9V0L8C29") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = newAccountName,
                                onValueChange = { newAccountName = it },
                                label = { Text("村庄名称/备注（可留空点击同步自动获取）") },
                                placeholder = { Text("如：主号、16本小号") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            // 操作按钮：一键联网同步 / 保存
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showAddSection = false }) {
                                    Text("取消", color = TextSecondary)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // 联网自动同步按钮
                                Button(
                                    onClick = {
                                        if (newPlayerTag.isBlank()) {
                                            Toast.makeText(context, "请先输入玩家标签 (如 #XXXXXX)", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isSyncing = true
                                        coroutineScope.launch {
                                            val result = ClashApiService.fetchPlayerProfile(newPlayerTag, newApiToken)
                                            isSyncing = false
                                            result.onSuccess { profile ->
                                                newAccountName = profile.name
                                                onAddAccount(
                                                    profile.name,
                                                    profile.tag,
                                                    newApiToken,
                                                    profile.townHallLevel,
                                                    profile.clanName ?: ""
                                                )
                                                showAddSection = false
                                                Toast.makeText(
                                                    context,
                                                    "🎉 同步成功！欢迎首领 ${profile.name} (${profile.townHallLevel}级大本营)",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, "同步失败: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !isSyncing && newPlayerTag.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("同步中...", color = Color.Black, fontSize = 12.sp)
                                    } else {
                                        Text("🚀 自动同步并添加", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 账号列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        val isSelected = acc.id == currentAccountId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAccount(acc) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkCard else DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) GoldPrimary.copy(alpha = 0.5f) else DarkCardBorder
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isSelected) "👑" else "🛡️",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(end = 10.dp)
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) GoldPrimary else TextPrimary
                                            )
                                            if (acc.townHallLevel > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(GoldPrimary.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${acc.townHallLevel}本",
                                                        fontSize = 10.sp,
                                                        color = GoldPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Row {
                                            if (acc.playerTag.isNotEmpty()) {
                                                Text(
                                                    text = acc.playerTag,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            if (acc.clanName.isNotEmpty()) {
                                                Text(
                                                    text = " • ${acc.clanName}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GemGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "使用中",
                                                fontSize = 11.sp,
                                                color = GemGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        TextButton(onClick = { onSelectAccount(acc) }) {
                                            Text("切换", color = GoldPrimary, fontSize = 12.sp)
                                        }
                                    }

                                    if (accounts.size > 1 && !isSelected) {
                                        IconButton(
                                            onClick = { onDeleteAccount(acc.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("🗑️", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                // 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("完成", color = GoldPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
