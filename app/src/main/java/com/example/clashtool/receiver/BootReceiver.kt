package com.example.clashtool.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.clashtool.data.TaskRepository
import com.example.clashtool.notification.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val repository = TaskRepository(context)
            val tasks = repository.getAllActiveTasksAcrossAllAccounts()
            val now = System.currentTimeMillis()
            for (task in tasks) {
                if (task.isRunning && task.endTimeMs > now) {
                    AlarmScheduler.scheduleTaskAlarm(context, task)
                }
            }
        }
    }
}
