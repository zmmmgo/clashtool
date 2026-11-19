package com.example.clashtool.parser

import com.example.clashtool.data.ClashTask
import com.example.clashtool.data.TaskCategory
import org.json.JSONObject

data class ParsedVillageData(
    val playerTag: String,
    val exportTimestampSec: Long,
    val activeTasks: List<ClashTask>,
    val townHallLevel: Int
)

/**
 * 严格遵照「部落小站」(coc.zmmmgo.cc.cd) 规范命名的村庄 JSON 解析器
 */
object ClashJsonParser {

    fun parseVillageJson(
        jsonString: String,
        accountId: String,
        accountName: String
    ): Result<ParsedVillageData> {
        return try {
            val root = JSONObject(jsonString.trim())
            val tag = root.optString("tag", "#UNKNOWN")
            val exportTimestampSec = root.optLong("timestamp", System.currentTimeMillis() / 1000L)
            val exportTimeMs = exportTimestampSec * 1000L
            val nowMs = System.currentTimeMillis()
            val elapsedMs = (nowMs - exportTimeMs).coerceAtLeast(0L)

            val taskList = mutableListOf<ClashTask>()

            // 1. 解析建筑升级 (buildings)
            val buildingsArr = root.optJSONArray("buildings")
            var builderIndex = 1
            var extraIndex = 1
            var thLevel = 1

            if (buildingsArr != null) {
                for (i in 0 until buildingsArr.length()) {
                    val b = buildingsArr.getJSONObject(i)
                    val dataId = b.optInt("data", 0)
                    val lvl = b.optInt("lvl", 1)

                    if (dataId == 1000000) {
                        if (lvl > thLevel) thLevel = lvl
                    }

                    if (b.has("timer")) {
                        val timerSec = b.optLong("timer", 0L)
                        val remainingMs = (timerSec * 1000L - elapsedMs).coerceAtLeast(0L)
                        val buildingName = getBuildingName(dataId)
                        val isExtra = b.optBoolean("extra", false)

                        val (slotTitle, taskId) = if (isExtra) {
                            Pair("建筑工人 (学徒/额外 $extraIndex)", "${accountId}_hb_extra_${extraIndex++}")
                        } else {
                            val idx = builderIndex++
                            val name = if (idx == 6) "建筑工人 6号 (小博/奥豆)" else "建筑工人 ${idx}号"
                            Pair(name, "${accountId}_hb_$idx")
                        }

                        val task = ClashTask(
                            id = taskId,
                            accountId = accountId,
                            accountName = accountName,
                            category = TaskCategory.HOME_BUILDER,
                            slotName = slotTitle,
                            targetName = "$buildingName 升至 Lv.${lvl + 1}",
                            startTimeMs = nowMs - 1000L,
                            totalDurationMs = remainingMs + 1000L,
                            isRunning = remainingMs > 0L
                        )
                        taskList.add(task)
                    }
                }
            }

            // 补充未满的建筑工人空闲槽位
            while (builderIndex <= 6) {
                val idx = builderIndex
                taskList.add(
                    ClashTask(
                        id = "${accountId}_hb_$idx",
                        accountId = accountId,
                        accountName = accountName,
                        category = TaskCategory.HOME_BUILDER,
                        slotName = if (idx == 6) "建筑工人 6号 (小博/奥豆)" else "建筑工人 ${idx}号",
                        targetName = "",
                        startTimeMs = 0L,
                        totalDurationMs = 0L,
                        isRunning = false
                    )
                )
                builderIndex++
            }

            // 2. 解析实验室科技升级 (units / spells)
            var labFound = false
            val unitsArr = root.optJSONArray("units")
            if (unitsArr != null) {
                for (i in 0 until unitsArr.length()) {
                    val u = unitsArr.getJSONObject(i)
                    if (u.has("timer")) {
                        val dataId = u.optInt("data", 0)
                        val lvl = u.optInt("lvl", 1)
                        val timerSec = u.optLong("timer", 0L)
                        val remainingMs = (timerSec * 1000L - elapsedMs).coerceAtLeast(0L)
                        val unitName = getUnitName(dataId)
                        val isExtra = u.optBoolean("extra", false)

                        val slotName = if (isExtra) "实验助理研究" else "实验室 科技升级"
                        taskList.add(
                            ClashTask(
                                id = "${accountId}_lab_${i}",
                                accountId = accountId,
                                accountName = accountName,
                                category = TaskCategory.LABORATORY,
                                slotName = slotName,
                                targetName = "$unitName 升至 Lv.${lvl + 1}",
                                startTimeMs = nowMs - 1000L,
                                totalDurationMs = remainingMs + 1000L,
                                isRunning = remainingMs > 0L
                            )
                        )
                        if (!isExtra) labFound = true
                    }
                }
            }

            val spellsArr = root.optJSONArray("spells")
            if (spellsArr != null) {
                for (i in 0 until spellsArr.length()) {
                    val s = spellsArr.getJSONObject(i)
                    if (s.has("timer")) {
                        val dataId = s.optInt("data", 0)
                        val lvl = s.optInt("lvl", 1)
                        val timerSec = s.optLong("timer", 0L)
                        val remainingMs = (timerSec * 1000L - elapsedMs).coerceAtLeast(0L)
                        val spellName = getSpellName(dataId)

                        taskList.add(
                            ClashTask(
                                id = "${accountId}_spell_${i}",
                                accountId = accountId,
                                accountName = accountName,
                                category = TaskCategory.LABORATORY,
                                slotName = "实验室 法术升级",
                                targetName = "$spellName 升至 Lv.${lvl + 1}",
                                startTimeMs = nowMs - 1000L,
                                totalDurationMs = remainingMs + 1000L,
                                isRunning = remainingMs > 0L
                            )
                        )
                        labFound = true
                    }
                }
            }

            // 攻城机器在实验室的升级
            val siegeArr = root.optJSONArray("siege_machines")
            if (siegeArr != null) {
                for (i in 0 until siegeArr.length()) {
                    val sm = siegeArr.getJSONObject(i)
                    if (sm.has("timer")) {
                        val dataId = sm.optInt("data", 0)
                        val lvl = sm.optInt("lvl", 1)
                        val timerSec = sm.optLong("timer", 0L)
                        val remainingMs = (timerSec * 1000L - elapsedMs).coerceAtLeast(0L)
                        val siegeName = getSiegeMachineName(dataId)

                        taskList.add(
                            ClashTask(
                                id = "${accountId}_siege_${i}",
                                accountId = accountId,
                                accountName = accountName,
                                category = TaskCategory.LABORATORY,
                                slotName = "实验室 攻城机器升级",
                                targetName = "$siegeName 升至 Lv.${lvl + 1}",
                                startTimeMs = nowMs - 1000L,
                                totalDurationMs = remainingMs + 1000L,
                                isRunning = remainingMs > 0L
                            )
                        )
                        labFound = true
                    }
                }
            }

            if (!labFound) {
                taskList.add(
                    ClashTask(
                        id = "${accountId}_lab_idle",
                        accountId = accountId,
                        accountName = accountName,
                        category = TaskCategory.LABORATORY,
                        slotName = "实验室 科技升级",
                        targetName = "",
                        startTimeMs = 0L,
                        totalDurationMs = 0L,
                        isRunning = false
                    )
                )
            }

            // 3. 解析战宠升级 (pets)
            val petsArr = root.optJSONArray("pets")
            var petFound = false
            if (petsArr != null) {
                for (i in 0 until petsArr.length()) {
                    val p = petsArr.getJSONObject(i)
                    if (p.has("timer")) {
                        val dataId = p.optInt("data", 0)
                        val lvl = p.optInt("lvl", 1)
                        val timerSec = p.optLong("timer", 0L)
                        val remainingMs = (timerSec * 1000L - elapsedMs).coerceAtLeast(0L)
                        val petName = getPetName(dataId)

                        taskList.add(
                            ClashTask(
                                id = "${accountId}_pet_${i}",
                                accountId = accountId,
                                accountName = accountName,
                                category = TaskCategory.HERO_PET,
                                slotName = "战宠小屋",
                                targetName = "$petName 升至 Lv.${lvl + 1}",
                                startTimeMs = nowMs - 1000L,
                                totalDurationMs = remainingMs + 1000L,
                                isRunning = remainingMs > 0L
                            )
                        )
                        petFound = true
                    }
                }
            }

            // 4. 解析英雄升级 (heroes)
            val heroesArr = root.optJSONArray("heroes")
            if (heroesArr != null) {
                for (i in 0 until heroesArr.length()) {
                    val h = heroesArr.getJSONObject(i)
                    if (h.has("timer")) {
                        val dataId = h.optInt("data", 0)
                        val lvl = h.optInt("lvl", 1)
                        val timerSec = h.optLong("timer", 0L)
                        val remainingMs = (timerSec * 1000L - elapsedMs).coerceAtLeast(0L)
                        val heroName = getHeroName(dataId)

                        taskList.add(
                            ClashTask(
                                id = "${accountId}_hero_${i}",
                                accountId = accountId,
                                accountName = accountName,
                                category = TaskCategory.HERO_PET,
                                slotName = "英雄王座",
                                targetName = "$heroName 升至 Lv.${lvl + 1}",
                                startTimeMs = nowMs - 1000L,
                                totalDurationMs = remainingMs + 1000L,
                                isRunning = remainingMs > 0L
                            )
                        )
                        petFound = true
                    }
                }
            }

            if (!petFound) {
                taskList.add(
                    ClashTask(
                        id = "${accountId}_hero_pet_idle",
                        accountId = accountId,
                        accountName = accountName,
                        category = TaskCategory.HERO_PET,
                        slotName = "英雄/战宠 升级",
                        targetName = "",
                        startTimeMs = 0L,
                        totalDurationMs = 0L,
                        isRunning = false
                    )
                )
            }

            // 5. 夜世界工人
            taskList.add(
                ClashTask(
                    id = "${accountId}_bb_1",
                    accountId = accountId,
                    accountName = accountName,
                    category = TaskCategory.BUILDER_BASE,
                    slotName = "夜世界 建筑大师 1号",
                    targetName = "",
                    startTimeMs = 0L,
                    totalDurationMs = 0L,
                    isRunning = false
                )
            )
            taskList.add(
                ClashTask(
                    id = "${accountId}_bb_2",
                    accountId = accountId,
                    accountName = accountName,
                    category = TaskCategory.BUILDER_BASE,
                    slotName = "夜世界 建筑大师 2号 (小博)",
                    targetName = "",
                    startTimeMs = 0L,
                    totalDurationMs = 0L,
                    isRunning = false
                )
            )

            Result.success(
                ParsedVillageData(
                    playerTag = tag,
                    exportTimestampSec = exportTimestampSec,
                    activeTasks = taskList,
                    townHallLevel = thLevel
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 建筑名称对照（严格依据 coc.zmmmgo.cc.cd）
     */
    private fun getBuildingName(id: Int): String {
        return when (id) {
            1000000 -> "大本营"
            1000001 -> "金矿"
            1000002 -> "圣水收集器"
            1000003 -> "圣水瓶"
            1000004 -> "储金罐"
            1000005 -> "加农炮"
            1000006 -> "箭塔"
            1000007 -> "城墙"
            1000009 -> "法师塔"
            1000010 -> "防空火箭"
            1000011 -> "迫击炮"
            1000012 -> "部落城堡"
            1000013 -> "建筑工人小屋"
            1000014 -> "兵营"
            1000015 -> "训练营"
            1000019 -> "实验室"
            1000020 -> "暗黑重油罐"
            1000021 -> "暗黑重油钻井"
            1000023 -> "法术工厂"
            1000024 -> "X连弩"
            1000026 -> "暗黑训练营"
            1000027 -> "特斯拉电磁塔"
            1000028 -> "炸弹塔"
            1000029 -> "地狱之塔"
            1000032 -> "天鹰火炮"
            1000059 -> "攻城机器工坊"
            1000064 -> "战宠小屋"
            1000067 -> "投石炮"
            1000068 -> "战宠小屋"
            1000070 -> "暗黑法术工厂"
            1000071 -> "英雄殿堂"
            1000072 -> "法术塔"
            1000077 -> "巨石碑"
            1000079 -> "多轮齿轮塔"
            1000084 -> "法术塔"
            1000085 -> "跳弹加农炮"
            1000089 -> "多人箭塔"
            1000093 -> "学徒小屋"
            1000097 -> "铁匠铺"
            1000102 -> "熔岩喷发器"
            else -> "村庄工程 ($id)"
        }
    }

    /**
     * 部队/兵种对照（严格依据 coc.zmmmgo.cc.cd）
     */
    private fun getUnitName(id: Int): String {
        return when (id) {
            4000000 -> "野蛮人"
            4000001 -> "弓箭手"
            4000002 -> "巨人"
            4000003 -> "哥布林"
            4000004 -> "炸弹人"
            4000005 -> "气球兵"
            4000006 -> "法师"
            4000007 -> "天使"
            4000008 -> "飞龙"
            4000009 -> "皮卡超人"
            4000010 -> "亡灵"
            4000011 -> "瓦基丽武神"
            4000012 -> "戈仑石人"
            4000013 -> "女巫"
            4000015 -> "熔岩猎犬"
            4000017 -> "巨石投手"
            4000022 -> "飞龙宝宝"
            4000023 -> "掘地矿工"
            4000024 -> "雷电飞龙"
            4000053 -> "大雪怪"
            4000058 -> "龙骑士"
            4000059 -> "雷霆泰坦"
            4000065 -> "戈仑冰人"
            4000082 -> "英雄猎手"
            4000095 -> "守护者学徒"
            4000097 -> "超级部队"
            4000109 -> "根蔓骑士"
            4000110 -> "巨矛投手"
            4000123 -> "建筑工人学徒"
            4000132 -> "德鲁伊"
            4000150 -> "烈焰熔炉"
            4000177 -> "废墟女巫"
            else -> "科技兵种 ($id)"
        }
    }

    /**
     * 法术名称对照（严格依据 coc.zmmmgo.cc.cd）
     */
    private fun getSpellName(id: Int): String {
        return when (id) {
            26000000 -> "雷电法术"
            26000001 -> "疗伤法术"
            26000002 -> "狂暴法术"
            26000003 -> "弹跳法术"
            26000005 -> "冰冻法术"
            26000009 -> "镜像法术"
            26000010 -> "毒药法术"
            26000011 -> "地震法术"
            26000016 -> "急速法术"
            26000017 -> "骷髅法术"
            26000028 -> "蝙蝠法术"
            26000035 -> "隐形法术"
            26000053 -> "回溯法术"
            26000070 -> "蔓生法术"
            26000098 -> "冰障法术"
            26000109 -> "图腾法术"
            26000120 -> "复苏法术"
            26000123 -> "英雄法术"
            else -> "法术 ($id)"
        }
    }

    /**
     * 战宠名称对照（严格依据 coc.zmmmgo.cc.cd）
     */
    private fun getPetName(id: Int): String {
        return when (id) {
            73000000 -> "莱希"
            73000001 -> "闪枭"
            73000002 -> "大牦"
            73000003 -> "独角"
            73000004 -> "冰牙"
            73000007 -> "地兽"
            73000008 -> "毒蜥"
            73000009 -> "凤凰"
            73000010 -> "灵狐"
            73000011 -> "愤怒水母"
            73000016 -> "阿啾"
            73000017 -> "贪婪渡鸦"
            73000018 -> "贪婪渡鸦"
            else -> "战宠 ($id)"
        }
    }

    /**
     * 攻城机器对照（严格依据 coc.zmmmgo.cc.cd）
     */
    private fun getSiegeMachineName(id: Int): String {
        return when (id) {
            4000051 -> "攻城战车"
            4000052 -> "攻城飞艇"
            4000062 -> "攻城气球"
            4000075 -> "攻城训练营"
            4000087 -> "攻城滚木车"
            4000091 -> "攻城烈焰车"
            4000092 -> "攻城钻地机"
            4000135 -> "部队发射器"
            4000188 -> "天空战车"
            else -> "攻城机器 ($id)"
        }
    }

    /**
     * 英雄名称对照（严格依据 coc.zmmmgo.cc.cd）
     */
    private fun getHeroName(id: Int): String {
        return when (id) {
            28000000 -> "野蛮人之王"
            28000001 -> "弓箭女皇"
            28000002 -> "大守护者"
            28000004 -> "飞盾战神"
            28000006 -> "亡灵王子"
            28000007 -> "飞龙公爵"
            else -> "英雄 ($id)"
        }
    }
}
