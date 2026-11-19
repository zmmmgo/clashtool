package com.example.clashtool

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.clashtool.data.ClashAccount
import com.example.clashtool.data.ClashTask
import com.example.clashtool.data.TaskCategory
import com.example.clashtool.data.TaskRepository
import com.example.clashtool.notification.AlarmScheduler
import com.example.clashtool.ui.AccountManageDialog
import com.example.clashtool.ui.JsonImportDialog
import com.example.clashtool.ui.TaskCard
import com.example.clashtool.ui.TaskEditDialog
import com.example.clashtool.ui.UpdateDialog
import com.example.clashtool.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化通知渠道
        AlarmScheduler.createNotificationChannel(this)

        setContent {
            ClashToolTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repository = remember { TaskRepository(context) }

    var accountList by remember { mutableStateOf(repository.getAllAccounts()) }
    var currentAccount by remember { mutableStateOf(repository.getCurrentAccount()) }
    var taskList by remember { mutableStateOf(repository.getTasksForCurrentAccount()) }

    var editingTask by remember { mutableStateOf<ClashTask?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var githubOwner by remember { mutableStateOf(repository.getGithubOwner()) }
    var githubRepo by remember { mutableStateOf(repository.getGithubRepo()) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    // 每秒驱动倒计时更新 (绑定生命周期，退到后台完全暂停以节省电池)
    var currentTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                delay(1000)
                currentTick = System.currentTimeMillis()
            }
        }
    }

    // Android 13+ 通知权限申请
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "请授予通知权限，否则升级完成时无法收到手机提醒！", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val categories = listOf("全部") + TaskCategory.values().map { it.displayName }

    // 筛选当前分类
    val filteredTasks = remember(taskList, selectedCategoryIndex, currentTick) {
        if (selectedCategoryIndex == 0) {
            taskList
        } else {
            val targetCategory = TaskCategory.values()[selectedCategoryIndex - 1]
            taskList.filter { it.category == targetCategory }
        }
    }

    // 统计当前账号的数据
    val runningCount = taskList.count { it.isRunning && !it.isFinished }
    val finishedCount = taskList.count { it.isFinished }
    val idleCount = taskList.count { !it.isRunning }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⚔️ 部落冲突 提醒助手",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                },
                actions = {
                    // 粘贴 JSON 快速导入按钮
                    Button(
                        onClick = { showJsonImportDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("📋 粘贴JSON", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // 账号管理按钮
                    IconButton(onClick = { showAccountDialog = true }) {
                        Text("⚙️", fontSize = 18.sp)
                    }

                    // 检查更新按钮
                    IconButton(onClick = { showUpdateDialog = true }) {
                        Text("🚀", fontSize = 18.sp)
                    }

                    // 快速测试通知按钮
                    TextButton(
                        onClick = {
                            val testTask = ClashTask(
                                id = "test_reminder",
                                accountId = currentAccount.id,
                                accountName = currentAccount.name,
                                category = TaskCategory.HOME_BUILDER,
                                slotName = "测试工人",
                                targetName = "5秒测试升级",
                                startTimeMs = System.currentTimeMillis(),
                                totalDurationMs = 5000L,
                                isRunning = true
                            )
                            AlarmScheduler.scheduleTaskAlarm(context, testTask)
                            Toast.makeText(context, "已安排测试通知，请在5秒后查看手机通知栏！", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("🔔 测试", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 🌟 账号直显栏（直接在主页并排显示所有账号，点击立即秒切）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(accountList, key = { it.id }) { acc ->
                        val isSelected = acc.id == currentAccount.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GoldPrimary else DarkCard,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GoldPrimary else DarkCardBorder
                            ),
                            modifier = Modifier.clickable {
                                repository.setCurrentAccountId(acc.id)
                                currentAccount = acc
                                accountList = repository.getAllAccounts()
                                taskList = repository.getTasksForCurrentAccount()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelected) "👑 " else "🛡️ ",
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = acc.name,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 13.sp
                                )
                                if (acc.townHallLevel > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${acc.townHallLevel}本",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black.copy(alpha = 0.8f) else GoldPrimary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // "+ 新增号" 按钮
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.clickable { showAccountDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("+ 新增号", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 状态概览横幅
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(label = "升级进行中", count = runningCount, color = GoldPrimary)
                    VerticalDivider(modifier = Modifier.height(28.dp), color = DarkCardBorder)
                    StatItem(label = "已完成待收", count = finishedCount, color = GemGreen)
                    VerticalDivider(modifier = Modifier.height(28.dp), color = DarkCardBorder)
                    StatItem(label = "空闲摸鱼", count = idleCount, color = TextSecondary)
                }
            }

            // 分类筛选 Tab 栏
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = DarkBackground,
                contentColor = GoldPrimary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategoryIndex == index) GoldPrimary else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 任务列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onEditClick = { editingTask = task },
                        onClearClick = {
                            AlarmScheduler.cancelTaskAlarm(context, task.id)
                            repository.clearTask(task.id)
                            taskList = repository.getTasksForCurrentAccount()
                            Toast.makeText(context, "${task.slotName} 已重置为空闲", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // 弹出设置/修改倒计时对话框
    editingTask?.let { task ->
        TaskEditDialog(
            task = task,
            onDismiss = { editingTask = null },
            onConfirm = { targetName, durationMs ->
                val updatedTask = task.copy(
                    targetName = targetName,
                    accountId = currentAccount.id,
                    accountName = currentAccount.name,
                    startTimeMs = System.currentTimeMillis(),
                    totalDurationMs = durationMs,
                    isRunning = true
                )
                repository.saveTask(updatedTask)
                AlarmScheduler.scheduleTaskAlarm(context, updatedTask)
                taskList = repository.getTasksForCurrentAccount()
                editingTask = null
                Toast.makeText(context, "已开启 [${currentAccount.name}] ${task.slotName} 倒计时提醒！", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 弹出账号管理对话框
    if (showAccountDialog) {
        AccountManageDialog(
            accounts = accountList,
            currentAccountId = currentAccount.id,
            onSelectAccount = { selectedAcc ->
                repository.setCurrentAccountId(selectedAcc.id)
                currentAccount = selectedAcc
                accountList = repository.getAllAccounts()
                taskList = repository.getTasksForCurrentAccount()
                showAccountDialog = false
                Toast.makeText(context, "已切换到账号：${selectedAcc.name}", Toast.LENGTH_SHORT).show()
            },
            onAddAccount = { name, tag, token, thLevel, clan ->
                val newAcc = repository.addAccount(name, tag, token, thLevel, clan)
                currentAccount = newAcc
                accountList = repository.getAllAccounts()
                taskList = repository.getTasksForCurrentAccount()
                showAccountDialog = false
                Toast.makeText(context, "已创建并切换到：${newAcc.name}", Toast.LENGTH_SHORT).show()
            },
            onUpdateAccount = { updatedAcc ->
                repository.updateAccount(updatedAcc)
                accountList = repository.getAllAccounts()
                currentAccount = repository.getCurrentAccount()
                taskList = repository.getTasksForCurrentAccount()
            },
            onDeleteAccount = { accId ->
                val success = repository.deleteAccount(accId)
                if (success) {
                    accountList = repository.getAllAccounts()
                    currentAccount = repository.getCurrentAccount()
                    taskList = repository.getTasksForCurrentAccount()
                    Toast.makeText(context, "已删除该账号", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAccountDialog = false }
        )
    }

    // 弹出 JSON 粘贴导入对话框
    if (showJsonImportDialog) {
        JsonImportDialog(
            currentAccountId = currentAccount.id,
            currentAccountName = currentAccount.name,
            onDismiss = { showJsonImportDialog = false },
            onImportSuccess = { parsed ->
                // 1. 先清除当前账号旧任务的所有系统闹钟
                val oldTasks = repository.getTasksForCurrentAccount()
                for (old in oldTasks) {
                    AlarmScheduler.cancelTaskAlarm(context, old.id)
                }
                // 2. 批量替换新任务列表
                repository.replaceTasksForAccount(currentAccount.id, parsed.activeTasks)
                // 3. 注册新任务的系统定时闹钟
                for (task in parsed.activeTasks) {
                    if (task.isRunning) {
                        AlarmScheduler.scheduleTaskAlarm(context, task)
                    }
                }
                val updatedAccount = currentAccount.copy(
                    playerTag = parsed.playerTag,
                    townHallLevel = parsed.townHallLevel
                )
                repository.updateAccount(updatedAccount)
                currentAccount = updatedAccount
                accountList = repository.getAllAccounts()
                taskList = repository.getTasksForCurrentAccount()
                showJsonImportDialog = false

                val activeCount = parsed.activeTasks.count { it.isRunning }
                Toast.makeText(
                    context,
                    "🎉 成功同步！已安排 ${activeCount} 个正在升级的工程并开启通知提醒！",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // 弹出 GitHub 版本更新检测对话框
    if (showUpdateDialog) {
        UpdateDialog(
            initialOwner = githubOwner,
            initialRepo = githubRepo,
            onSaveRepo = { newOwner, newRepo ->
                repository.saveGithubRepo(newOwner, newRepo)
                githubOwner = newOwner
                githubRepo = newRepo
            },
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}
