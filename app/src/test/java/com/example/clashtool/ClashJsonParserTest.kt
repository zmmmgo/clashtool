package com.example.clashtool

import com.example.clashtool.parser.ClashJsonParser
import org.junit.Assert.*
import org.junit.Test

class ClashJsonParserTest {

    @Test
    fun testParseUserVillageJson() {
        val sampleJson = """
            {"tag":"#GCU9YYP2Y","timestamp":1795099460,"helpers":[{"data":93000000,"lvl":8,"helper_cooldown":48221},{"data":93000001,"lvl":12,"helper_cooldown":48221},{"data":93000002,"lvl":7,"helper_cooldown":48221},{"data":93000003,"lvl":1,"helper_cooldown":48221}],"guardians":[{"data":107000000,"lvl":1},{"data":107000001,"lvl":1},{"data":107000008,"lvl":1}],"buildings":[{"data":1000000,"lvl":13,"timer":72092},{"data":1000013,"lvl":17,"gear_up":1},{"data":1000000,"lvl":13,"timer":125024},{"data":1000000,"lvl":13,"timer":125048,"extra":true},{"data":1000067,"lvl":6,"timer":150723},{"data":1000077,"lvl":4,"timer":196843},{"data":1000089,"lvl":2,"timer":127197},{"data":1000089,"lvl":2,"timer":142107},{"data":1000097,"types":[{"data":103000011,"modules":[{"data":102000033,"lvl":1},{"data":102000034,"lvl":1},{"data":102000035,"lvl":1}]},{"data":103000012,"modules":[{"data":102000036,"lvl":1},{"data":102000037,"lvl":1},{"data":102000038,"lvl":1}]},{"data":103000013,"modules":[{"data":102000039,"lvl":3},{"data":102000040,"lvl":4},{"data":102000041,"lvl":6}]}]},{"data":1000000,"lvl":14,"cnt":1},{"data":1000001,"lvl":18,"cnt":1}],"units":[{"data":4000000,"lvl":12},{"data":4000001,"lvl":13,"timer":575393},{"data":4000123,"lvl":5,"timer":65461,"extra":true}],"siege_machines":[{"data":4000051,"lvl":1},{"data":4000052,"lvl":4}],"heroes":[{"data":28000000,"lvl":99}],"spells":[{"data":26000000,"lvl":10}],"pets":[{"data":73000017,"lvl":1,"timer":266707}]}
        """.trimIndent()

        val result = ClashJsonParser.parseVillageJson(
            jsonString = sampleJson,
            accountId = "test_acc",
            accountName = "测试大号"
        )

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("#GCU9YYP2Y", data.playerTag)
        // 验证大本营等级：根据 1000001 为 18 本，而不是误将 1000000 兵营识别为 14 本
        assertEquals(18, data.townHallLevel)

        val runningTasks = data.activeTasks.filter { it.isRunning }
        assertTrue(runningTasks.isNotEmpty())

        // 验证建筑升级：1000000 必须被正确识别为「兵营」，而不是「大本营」
        val armyCampTasks = runningTasks.filter { it.targetName.contains("兵营") }
        assertEquals(3, armyCampTasks.size) // 2个普通兵营升级 + 1个学徒加速兵营

        // 验证升级项目里绝无错误的「大本营」
        val thTasks = runningTasks.filter { it.targetName.contains("大本营") }
        assertTrue("升级项目中不应包含大本营", thTasks.isEmpty())

        // 验证名字符合部落小站规范
        assertTrue(runningTasks.any { it.targetName.contains("投石炮") })
        assertTrue(runningTasks.any { it.targetName.contains("巨石碑") })
        assertTrue(runningTasks.any { it.targetName.contains("火焰喷射器") }) // 1000089 为火焰喷射器
        assertTrue(runningTasks.any { it.targetName.contains("弓箭手") })
        assertTrue(runningTasks.any { it.targetName.contains("贪婪渡鸦") })

        // 验证学徒工人与学徒研究
        assertTrue(runningTasks.any { it.slotName.contains("建筑工人学徒") })
        assertTrue(runningTasks.any { it.slotName.contains("实验室学徒") })

        // 验证倒计时时间精准对齐（不扣除 cross-machine 差异）
        val campTask1 = runningTasks.first { it.targetName == "兵营 升至 Lv.14" }
        assertEquals(72092 * 1000L, campTask1.totalDurationMs)
        assertTrue(campTask1.remainingMs in (72092 * 1000L - 1000L)..(72092 * 1000L))
    }
}
