package com.example.clashtool.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class HeroInfo(
    val name: String,
    val level: Int,
    val maxLevel: Int
)

data class ClashPlayerProfile(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val trophies: Int,
    val clanName: String?,
    val heroes: List<HeroInfo>
)

object ClashApiService {

    suspend fun fetchPlayerProfile(
        playerTag: String,
        apiToken: String
    ): Result<ClashPlayerProfile> = withContext(Dispatchers.IO) {
        try {
            val formattedTag = if (playerTag.startsWith("#")) playerTag else "#$playerTag"
            val encodedTag = URLEncoder.encode(formattedTag, "UTF-8")
            val url = URL("https://api.clashofclans.com/v1/players/$encodedTag")

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer ${apiToken.trim()}")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val responseText = reader.use { it.readText() }
                    val json = JSONObject(responseText)

                    val name = json.optString("name", "首领")
                    val townHallLevel = json.optInt("townHallLevel", 1)
                    val trophies = json.optInt("trophies", 0)
                    val clanObj = json.optJSONObject("clan")
                    val clanName = clanObj?.optString("name")

                    val heroesList = mutableListOf<HeroInfo>()
                    val heroesArr = json.optJSONArray("heroes")
                    if (heroesArr != null) {
                        for (i in 0 until heroesArr.length()) {
                            val h = heroesArr.getJSONObject(i)
                            heroesList.add(
                                HeroInfo(
                                    name = h.optString("name", "英雄"),
                                    level = h.optInt("level", 1),
                                    maxLevel = h.optInt("maxLevel", 1)
                                )
                            )
                        }
                    }

                    Result.success(
                        ClashPlayerProfile(
                            tag = formattedTag,
                            name = name,
                            townHallLevel = townHallLevel,
                            trophies = trophies,
                            clanName = clanName,
                            heroes = heroesList
                        )
                    )
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "HTTP $responseCode"
                    }

                    val friendlyMessage = try {
                        val errJson = JSONObject(errorBody)
                        val reason = errJson.optString("reason", "")
                        val message = errJson.optString("message", "")
                        when (reason) {
                            "accessDenied.invalidIp" -> "IP 不在白名单中：$message\n请将该 IP 添加到 Supercell 开发者后台的 Allowed IP 中。"
                            "notFound" -> "未找到该玩家标签，请检查是否输入正确！"
                            else -> message.ifEmpty { "请求失败 (HTTP $responseCode)" }
                        }
                    } catch (e: Exception) {
                        errorBody
                    }

                    Result.failure(Exception(friendlyMessage))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
