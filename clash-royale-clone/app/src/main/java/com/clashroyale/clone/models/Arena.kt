package com.clashroyale.clone.models

data class Arena(
    val id: Int,
    val name: String,
    val trophyRequired: Int,
    val primaryColor: Long,
    val secondaryColor: Long,
    val accentColor: Long,
    val groundColor: Long,
    val riverColor: Long,
    val description: String,
    val unlockedCardIds: List<Int>
)
