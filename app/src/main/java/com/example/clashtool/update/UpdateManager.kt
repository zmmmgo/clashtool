package com.example.clashtool.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.clashtool.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileSize: Long,
    val htmlUrl: String
)

object UpdateManager {

    const val DEFAULT_GITHUB_OWNER = "zmmmgo"
    const val DEFAULT_GITHUB_REPO = "clashtool"

    /**
     * 从 GitHub Releases API 获取最新版本信息
     */
    suspend fun checkGithubUpdate(
        owner: String = DEFAULT_GITHUB_OWNER,
        repo: String = DEFAULT_GITHUB_REPO
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/${owner.trim()}/${repo.trim()}/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ClashTool-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val jsonStr = reader.use { it.readText() }
                    val json = JSONObject(jsonStr)

                    val tagName = json.optString("tag_name", "").trim()
                    val title = json.optString("name", tagName)
                    val notes = json.optString("body", "暂无更新说明")
                    val htmlUrl = json.optString("html_url", "")

                    // 清理版本号（如 "v1.1.0" -> "1.1.0"）
                    val cleanLatestVersion = tagName.removePrefix("v").removePrefix("V")
                    val currentVersion = BuildConfig.VERSION_NAME

                    // 在 assets 中查找 .apk 文件
                    var apkUrl = ""
                    var apkSize = 0L
                    val assetsArr = json.optJSONArray("assets")
                    if (assetsArr != null && assetsArr.length() > 0) {
                        for (i in 0 until assetsArr.length()) {
                            val asset = assetsArr.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true) || apkUrl.isEmpty()) {
                                apkUrl = asset.optString("browser_download_url", "")
                                apkSize = asset.optLong("size", 0L)
                                if (name.endsWith(".apk", ignoreCase = true)) break
                            }
                        }
                    }

                    if (apkUrl.isEmpty()) {
                        apkUrl = htmlUrl
                    }

                    val hasUpdate = isNewerVersion(cleanLatestVersion, currentVersion)

                    Result.success(
                        UpdateInfo(
                            hasUpdate = hasUpdate,
                            currentVersion = currentVersion,
                            latestVersion = cleanLatestVersion.ifEmpty { currentVersion },
                            releaseTitle = title,
                            releaseNotes = notes,
                            downloadUrl = apkUrl,
                            fileSize = apkSize,
                            htmlUrl = htmlUrl
                        )
                    )
                } else if (responseCode == 404) {
                    Result.failure(Exception("GitHub 仓库 $owner/$repo 尚未发布任何 Release 版本！\n请在 GitHub 仓库发布最新版本并上传 APK。"))
                } else {
                    Result.failure(Exception("检查更新失败 (HTTP $responseCode)"))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 版本号比较算法（支持 x.y.z 等格式）
     */
    fun isNewerVersion(remoteVersion: String, localVersion: String): Boolean {
        if (remoteVersion.isBlank() || localVersion.isBlank()) return false
        try {
            val remoteParts = remoteVersion.replace(Regex("[^0-9.]"), "").split(".").mapNotNull { it.toIntOrNull() }
            val localParts = localVersion.replace(Regex("[^0-9.]"), "").split(".").mapNotNull { it.toIntOrNull() }

            val maxLength = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLength) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 下载 APK 文件到应用专属外部缓存目录，支持实时进度回调
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        targetFileName: String = "ClashTool_update.apk",
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val destFile = File(context.getExternalFilesDir(null) ?: context.cacheDir, targetFileName)
            if (destFile.exists()) {
                destFile.delete()
            }

            var currentUrl = downloadUrl
            var redirects = 0
            while (redirects < 5) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "ClashTool-App")
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == 307 || code == 308
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl != null) {
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: return@withContext Result.failure(Exception("网络连接初始化失败"))
            val totalBytes = conn.contentLengthLong.coerceAtLeast(conn.contentLength.toLong())

            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                        withContext(Dispatchers.Main) {
                            onProgress(progress.coerceIn(0f, 1f), totalRead, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 调起系统安装界面
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                    Toast.makeText(context, "请先允许“安装未知应用”权限以继续更新", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "调起安装失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 在浏览器中打开 Release 页面或下载直链
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
