package com.example.clashtool.data

import java.util.UUID

data class ClashAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val playerTag: String = "",
    val apiToken: String = "",
    val townHallLevel: Int = 0,
    val clanName: String = "",
    val isCurrent: Boolean = false
)
