package com.example.clashtool.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.clashtool.MainActivity
import com.example.clashtool.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID) ?: ""
        val accountName = intent.getStringExtra(AlarmScheduler.EXTRA_ACCOUNT_NAME) ?: "我的大号"
        val slotName = intent.getStringExtra(AlarmScheduler.EXTRA_SLOT_NAME) ?: "工人"
        val targetName = intent.getStringExtra(AlarmScheduler.EXTRA_TARGET_NAME) ?: "工程"

        AlarmScheduler.createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AlarmScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("【部落冲突】升级完成！🎉")
            .setContentText("[$accountName] $slotName: $targetName 已完成，快上线！")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "【账号】$accountName\n" +
                    "【工程】$slotName - $targetName 已顺利升级完毕！\n" +
                    "工人已处于空闲状态，别让工人摸鱼，快登录该账号安排新工程！"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(taskId.hashCode(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
