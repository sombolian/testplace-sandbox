package com.clashroyale.clone.models

enum class CardType {
    TROOP, SPELL, BUILDING
}

enum class Rarity(val color: Long, val label: String) {
    COMMON(0xFF8E8E8E, "Common"),
    RARE(0xFFFF9800, "Rare"),
    EPIC(0xFF9C27B0, "Epic"),
    LEGENDARY(0xFFFFD700, "Legendary")
}

enum class TargetType {
    GROUND, AIR, GROUND_AND_AIR, BUILDINGS
}

data class Card(
    val id: Int,
    val name: String,
    val type: CardType,
    val rarity: Rarity,
    val elixirCost: Int,
    val hitpoints: Int,
    val damage: Int,
    val attackSpeed: Float, // seconds between attacks
    val moveSpeed: Float, // tiles per second (0 for buildings/spells)
    val range: Float, // attack range in tiles
    val targetType: TargetType,
    val isFlying: Boolean = false,
    val splashRadius: Float = 0f, // 0 = single target
    val spawnCount: Int = 1, // how many units spawn
    val description: String = "",
    val iconEmoji: String = "", // fallback visual identifier
    val lifetime: Float = 0f, // for buildings/spells, seconds
    val spellRadius: Float = 0f, // for spells, area of effect
    val spawnCardId: Int = -1, // for spawner buildings
    val deathSpawnCardId: Int = -1, // what spawns on death (e.g., Golem -> Golemites)
    val chargeSpeed: Float = 0f, // for charge attacks (Prince)
    val chargeDamageMultiplier: Float = 1f
) {
    val dps: Float get() = if (attackSpeed > 0) damage / attackSpeed else 0f
}
