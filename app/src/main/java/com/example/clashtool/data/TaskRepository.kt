package com.example.clashtool.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("clash_timer_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TASKS = "key_clash_tasks"
        private const val KEY_ACCOUNTS = "key_clash_accounts"
        private const val KEY_CURRENT_ACCOUNT_ID = "key_current_account_id"
        private const val KEY_GITHUB_OWNER = "key_github_owner"
        private const val KEY_GITHUB_REPO = "key_github_repo"

        fun generateDefaultSlots(accountId: String, accountName: String): List<ClashTask> {
            return listOf(
                ClashTask(id = "${accountId}_hb_1", accountId = accountId, accountName = accountName, category = TaskCategory.HOME_BUILDER, slotName = "建筑工人 1号"),
                ClashTask(id = "${accountId}_hb_2", accountId = accountId, accountName = accountName, category = TaskCategory.HOME_BUILDER, slotName = "建筑工人 2号"),
                ClashTask(id = "${accountId}_hb_3", accountId = accountId, accountName = accountName, category = TaskCategory.HOME_BUILDER, slotName = "建筑工人 3号"),
                ClashTask(id = "${accountId}_hb_4", accountId = accountId, accountName = accountName, category = TaskCategory.HOME_BUILDER, slotName = "建筑工人 4号"),
                ClashTask(id = "${accountId}_hb_5", accountId = accountId, accountName = accountName, category = TaskCategory.HOME_BUILDER, slotName = "建筑工人 5号"),
                ClashTask(id = "${accountId}_hb_6", accountId = accountId, accountName = accountName, category = TaskCategory.HOME_BUILDER, slotName = "建筑工人 6号 (奥豆)"),
                ClashTask(id = "${accountId}_lab_1", accountId = accountId, accountName = accountName, category = TaskCategory.LABORATORY, slotName = "实验室 科技升级"),
                ClashTask(id = "${accountId}_hero_1", accountId = accountId, accountName = accountName, category = TaskCategory.HERO_PET, slotName = "英雄/宠物 升级"),
                ClashTask(id = "${accountId}_bb_1", accountId = accountId, accountName = accountName, category = TaskCategory.BUILDER_BASE, slotName = "夜世界 建筑大师 1号"),
                ClashTask(id = "${accountId}_bb_2", accountId = accountId, accountName = accountName, category = TaskCategory.BUILDER_BASE, slotName = "夜世界 建筑大师 2号 (鲍勃)")
            )
        }
    }

    // --- 账号管理 ---

    fun getAllAccounts(): List<ClashAccount> {
        val jsonStr = prefs.getString(KEY_ACCOUNTS, null)
        if (jsonStr.isNullOrEmpty()) {
            val defaultAcc = ClashAccount(
                id = "acc_main",
                name = "我的主玩大号",
                playerTag = "",
                isCurrent = true
            )
            val list = listOf(defaultAcc)
            saveAllAccounts(list)
            setCurrentAccountId(defaultAcc.id)

            // 初始化大号的默认槽位
            val initialTasks = generateDefaultSlots(defaultAcc.id, defaultAcc.name)
            saveAllTasks(initialTasks)
            return list
        }

        val list = mutableListOf<ClashAccount>()
        try {
            val arr = JSONArray(jsonStr)
            val currentId = getCurrentAccountId()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                list.add(
                    ClashAccount(
                        id = id,
                        name = obj.getString("name"),
                        playerTag = obj.optString("playerTag", ""),
                        apiToken = obj.optString("apiToken", ""),
                        townHallLevel = obj.optInt("townHallLevel", 0),
                        clanName = obj.optString("clanName", ""),
                        isCurrent = (id == currentId)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (list.isEmpty()) {
            val defaultAcc = ClashAccount(id = "acc_main", name = "我的主玩大号", isCurrent = true)
            list.add(defaultAcc)
            saveAllAccounts(list)
            setCurrentAccountId(defaultAcc.id)
        }
        return list
    }

    fun getCurrentAccountId(): String {
        return prefs.getString(KEY_CURRENT_ACCOUNT_ID, "acc_main") ?: "acc_main"
    }

    fun setCurrentAccountId(id: String) {
        prefs.edit().putString(KEY_CURRENT_ACCOUNT_ID, id).apply()
    }

    fun getCurrentAccount(): ClashAccount {
        val accounts = getAllAccounts()
        val currentId = getCurrentAccountId()
        return accounts.find { it.id == currentId } ?: accounts.first()
    }

    fun addAccount(
        name: String,
        tag: String,
        token: String = "",
        thLevel: Int = 0,
        clan: String = ""
    ): ClashAccount {
        val accounts = getAllAccounts().toMutableList()
        val newAccount = ClashAccount(
            name = name.trim().ifEmpty { "新村庄小号" },
            playerTag = tag.trim(),
            apiToken = token.trim(),
            townHallLevel = thLevel,
            clanName = clan,
            isCurrent = true
        )
        accounts.add(newAccount)
        saveAllAccounts(accounts)
        setCurrentAccountId(newAccount.id)

        // 为新账号初始化全套升级槽位
        val newTasks = generateDefaultSlots(newAccount.id, newAccount.name)
        val allTasks = getAllRawTasks().toMutableList()
        allTasks.addAll(newTasks)
        saveAllTasks(allTasks)

        return newAccount
    }

    fun updateAccount(account: ClashAccount) {
        val accounts = getAllAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            accounts[index] = account
            saveAllAccounts(accounts)

            // 如果更新了名字，同时更新该账号下任务的 accountName
            val allTasks = getAllRawTasks().toMutableList()
            var tasksModified = false
            for (i in allTasks.indices) {
                if (allTasks[i].accountId == account.id) {
                    allTasks[i] = allTasks[i].copy(accountName = account.name)
                    tasksModified = true
                }
            }
            if (tasksModified) {
                saveAllTasks(allTasks)
            }
        }
    }

    fun deleteAccount(accountId: String): Boolean {
        val accounts = getAllAccounts().toMutableList()
        if (accounts.size <= 1) return false // 至少保留一个账号

        accounts.removeAll { it.id == accountId }
        saveAllAccounts(accounts)

        // 移除该账号的所有任务
        val allTasks = getAllRawTasks().toMutableList()
        allTasks.removeAll { it.accountId == accountId }
        saveAllTasks(allTasks)

        if (getCurrentAccountId() == accountId) {
            setCurrentAccountId(accounts.first().id)
        }
        return true
    }

    private fun saveAllAccounts(accounts: List<ClashAccount>) {
        val arr = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("playerTag", acc.playerTag)
                put("apiToken", acc.apiToken)
                put("townHallLevel", acc.townHallLevel)
                put("clanName", acc.clanName)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply()
    }

    // --- 任务管理 ---

    fun getTasksForCurrentAccount(): List<ClashTask> {
        val currentAccount = getCurrentAccount()
        val allTasks = getAllRawTasks()
        val currentTasks = allTasks.filter { it.accountId == currentAccount.id }
        if (currentTasks.isEmpty()) {
            val created = generateDefaultSlots(currentAccount.id, currentAccount.name)
            val updated = allTasks + created
            saveAllTasks(updated)
            return created
        }
        return currentTasks
    }

    fun replaceTasksForAccount(accountId: String, newTasks: List<ClashTask>) {
        val allTasks = getAllRawTasks().toMutableList()
        allTasks.removeAll { it.accountId == accountId }
        allTasks.addAll(newTasks)
        saveAllTasks(allTasks)
    }

    fun getAllActiveTasksAcrossAllAccounts(): List<ClashTask> {
        return getAllRawTasks().filter { it.isRunning }
    }

    fun getAllTasks(): List<ClashTask> {
        return getAllRawTasks()
    }

    private fun getAllRawTasks(): List<ClashTask> {
        val jsonStr = prefs.getString(KEY_TASKS, null)
        if (jsonStr.isNullOrEmpty()) return emptyList()

        val list = mutableListOf<ClashTask>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ClashTask(
                        id = obj.getString("id"),
                        accountId = obj.optString("accountId", "acc_main"),
                        accountName = obj.optString("accountName", "大号"),
                        category = TaskCategory.valueOf(obj.getString("category")),
                        slotName = obj.getString("slotName"),
                        targetName = obj.optString("targetName", ""),
                        startTimeMs = obj.optLong("startTimeMs", 0L),
                        totalDurationMs = obj.optLong("totalDurationMs", 0L),
                        isRunning = obj.optBoolean("isRunning", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveTask(task: ClashTask) {
        val tasks = getAllRawTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            tasks[index] = task
        } else {
            tasks.add(task)
        }
        saveAllTasks(tasks)
    }

    fun clearTask(taskId: String): ClashTask? {
        val tasks = getAllRawTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val original = tasks[index]
            val cleared = original.copy(
                targetName = "",
                startTimeMs = 0L,
                totalDurationMs = 0L,
                isRunning = false
            )
            tasks[index] = cleared
            saveAllTasks(tasks)
            return cleared
        }
        return null
    }

    private fun saveAllTasks(tasks: List<ClashTask>) {
        val arr = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("accountId", task.accountId)
                put("accountName", task.accountName)
                put("category", task.category.name)
                put("slotName", task.slotName)
                put("targetName", task.targetName)
                put("startTimeMs", task.startTimeMs)
                put("totalDurationMs", task.totalDurationMs)
                put("isRunning", task.isRunning)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    // --- GitHub 仓库配置 ---
    fun getGithubOwner(): String = prefs.getString(KEY_GITHUB_OWNER, "zmmmgo") ?: "zmmmgo"
    fun getGithubRepo(): String = prefs.getString(KEY_GITHUB_REPO, "clashtool") ?: "clashtool"
    fun saveGithubRepo(owner: String, repo: String) {
        prefs.edit()
            .putString(KEY_GITHUB_OWNER, owner.trim())
            .putString(KEY_GITHUB_REPO, repo.trim())
            .apply()
    }
}
