package com.clashroyale.clone.models

data class Player(
    var name: String = "Player",
    var trophies: Int = 0,
    var highestTrophies: Int = 0,
    var level: Int = 1,
    var experience: Int = 0,
    var gold: Int = 1000,
    var gems: Int = 100,
    var deck: MutableList<Int> = mutableListOf(), // card IDs
    var cardCollection: MutableMap<Int, Int> = mutableMapOf(), // cardId -> level
    var wins: Int = 0,
    var losses: Int = 0,
    var winStreak: Int = 0
) {
    val arenaId: Int get() {
        return when {
            trophies >= 5000 -> 9
            trophies >= 4000 -> 8
            trophies >= 3400 -> 7
            trophies >= 2600 -> 6
            trophies >= 2000 -> 5
            trophies >= 1400 -> 4
            trophies >= 800 -> 3
            trophies >= 400 -> 2
            trophies >= 0 -> 1
            else -> 0
        }
    }

    val winRate: Float get() {
        val total = wins + losses
        return if (total > 0) wins.toFloat() / total else 0f
    }

    fun addTrophies(amount: Int) {
        trophies = (trophies + amount).coerceAtLeast(0)
        if (trophies > highestTrophies) highestTrophies = trophies
    }
}
