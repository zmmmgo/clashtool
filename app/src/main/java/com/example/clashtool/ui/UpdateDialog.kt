package com.example.clashtool.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.clashtool.ui.theme.*
import com.example.clashtool.update.UpdateInfo
import com.example.clashtool.update.UpdateManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun UpdateDialog(
    initialOwner: String = UpdateManager.DEFAULT_GITHUB_OWNER,
    initialRepo: String = UpdateManager.DEFAULT_GITHUB_REPO,
    onSaveRepo: (owner: String, repo: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var owner by remember { mutableStateOf(initialOwner) }
    var repo by remember { mutableStateOf(initialRepo) }
    var showRepoConfig by remember { mutableStateOf(false) }

    var isChecking by remember { mutableStateOf(true) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    fun triggerCheck(targetOwner: String, targetRepo: String) {
        isChecking = true
        checkError = null
        updateInfo = null
        coroutineScope.launch {
            val result = UpdateManager.checkGithubUpdate(targetOwner, targetRepo)
            isChecking = false
            result.onSuccess { info ->
                updateInfo = info
            }.onFailure { err ->
                checkError = err.message ?: "网络连接失败，请稍后重试"
            }
        }
    }

    LaunchedEffect(Unit) {
        triggerCheck(owner, repo)
    }

    Dialog(onDismissRequest = {
        if (!isDownloading) onDismiss()
    }) {
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
                // 顶部标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🚀", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "版本更新检查",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    TextButton(onClick = { showRepoConfig = !showRepoConfig }) {
                        Text(
                            text = if (showRepoConfig) "收起配置" else "⚙️ 仓库配置",
                            color = GoldPrimary,
                            fontSize = 12.sp
                        )
                    }
                }

                // GitHub 仓库配置面板（可收放）
                if (showRepoConfig) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "GitHub 源码仓库路径：",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = owner,
                                    onValueChange = { owner = it.trim() },
                                    label = { Text("用户名/组织") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPrimary,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                OutlinedTextField(
                                    value = repo,
                                    onValueChange = { repo = it.trim() },
                                    label = { Text("仓库名") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPrimary,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = {
                                        onSaveRepo(owner, repo)
                                        showRepoConfig = false
                                        triggerCheck(owner, repo)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                ) {
                                    Text("保存并重新检查", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 状态内容区
                if (isChecking) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("正在连接 GitHub 检索最新 Release...", color = TextSecondary, fontSize = 13.sp)
                    }
                } else if (checkError != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("❌ 检查更新未成功", color = DangerRed, fontWeight = FontWeight.Bold)
                        Text(
                            text = checkError ?: "",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onDismiss) {
                                Text("关闭", color = TextSecondary)
                            }
                            Button(
                                onClick = { triggerCheck(owner, repo) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("重试", color = Color.Black)
                            }
                        }
                    }
                } else if (updateInfo != null) {
                    val info = updateInfo!!
                    if (info.hasUpdate) {
                        // 发现新版本
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "发现新版本：v${info.latestVersion}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = "当前: v${info.currentVersion}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            if (info.fileSize > 0) {
                                Text(
                                    text = "📦 安装包大小：${formatFileSize(info.fileSize)}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            // 更新日志说明卡片
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "📝 更新日志：",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = GoldPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = info.releaseNotes,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            // 下载进度展示
                            if (isDownloading) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = GoldPrimary,
                                        trackColor = DarkCardBorder
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "正在下载: ${(downloadProgress * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            color = GoldPrimary
                                        )
                                        if (totalBytes > 0) {
                                            Text(
                                                text = "${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            if (downloadError != null) {
                                Text(
                                    text = "下载中断: $downloadError\n若 GitHub 下载缓慢，建议点击下方在浏览器中直接下载。",
                                    color = DangerRed,
                                    fontSize = 11.sp
                                )
                            }

                            HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                            // 底部操作按钮
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        UpdateManager.openInBrowser(context, info.htmlUrl)
                                    }
                                ) {
                                    Text("🌐 浏览器打开", color = TextSecondary, fontSize = 12.sp)
                                }

                                Row {
                                    if (!isDownloading && downloadedApkFile == null) {
                                        TextButton(onClick = onDismiss) {
                                            Text("稍后再说", color = TextSecondary)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                isDownloading = true
                                                downloadError = null
                                                coroutineScope.launch {
                                                    val result = UpdateManager.downloadApk(
                                                        context = context,
                                                        downloadUrl = info.downloadUrl,
                                                        onProgress = { p, cur, tot ->
                                                            downloadProgress = p
                                                            downloadedBytes = cur
                                                            totalBytes = tot
                                                        }
                                                    )
                                                    isDownloading = false
                                                    result.onSuccess { apkFile ->
                                                        downloadedApkFile = apkFile
                                                        Toast.makeText(context, "下载完成，正在调起安装...", Toast.LENGTH_SHORT).show()
                                                        UpdateManager.installApk(context, apkFile)
                                                    }.onFailure { err ->
                                                        downloadError = err.message ?: "下载失败"
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                        ) {
                                            Text("🚀 立即更新", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (downloadedApkFile != null) {
                                        Button(
                                            onClick = {
                                                UpdateManager.installApk(context, downloadedApkFile!!)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GemGreen)
                                        ) {
                                            Text("✅ 点击安装 APK", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 已是最新版本
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🎉", fontSize = 36.sp)
                            Text(
                                text = "当前已是最新版本 (v${info.currentVersion})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "仓库 $owner/$repo 暂无更高版本需要更新",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("知道了", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}
