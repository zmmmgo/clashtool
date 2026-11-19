package com.example.clashtool.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TaskCategory(val displayName: String, val icon: String) {
    HOME_BUILDER("主基地工人", "🔨"),
    LABORATORY("实验室科技", "⚗️"),
    HERO_PET("英雄与宠物", "👑"),
    BUILDER_BASE("夜世界工人", "⚙️")
}

data class ClashTask(
    val id: String = UUID.randomUUID().toString(),
    val accountId: String = "default_account",
    val accountName: String = "默认大号",
    val category: TaskCategory,
    val slotName: String,
    val targetName: String = "",
    val startTimeMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isRunning: Boolean = false
) {
    val endTimeMs: Long
        get() = if (isRunning && totalDurationMs > 0) startTimeMs + totalDurationMs else 0L

    val remainingMs: Long
        get() {
            if (!isRunning) return 0L
            val rem = endTimeMs - System.currentTimeMillis()
            return if (rem > 0) rem else 0L
        }

    val isFinished: Boolean
        get() = isRunning && remainingMs <= 0L

    val progress: Float
        get() {
            if (!isRunning || totalDurationMs <= 0L) return 0f
            val elapsed = System.currentTimeMillis() - startTimeMs
            val p = elapsed.toFloat() / totalDurationMs.toFloat()
            return p.coerceIn(0f, 1f)
        }

    fun formattedRemaining(): String {
        if (!isRunning) return "空闲中"
        val remSec = remainingMs / 1000
        if (remSec <= 0) return "已完成！"
        val days = remSec / 86400
        val hours = (remSec % 86400) / 3600
        val minutes = (remSec % 3600) / 60
        val seconds = remSec % 60

        return if (days > 0) {
            String.format(Locale.getDefault(), "%d天 %02d小时 %02d分 %02d秒", days, hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d小时 %02d分 %02d秒", hours, minutes, seconds)
        }
    }

    fun formattedEndTime(): String {
        if (!isRunning || endTimeMs <= 0L) return "无计划"
        val sdf = SimpleDateFormat("MM月dd日 HH:mm (E)", Locale.CHINA)
        return sdf.format(Date(endTimeMs))
    }
}
