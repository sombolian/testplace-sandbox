package com.clashroyale.clone.data

import com.clashroyale.clone.models.*

object CardDatabase {

    val allCards: List<Card> by lazy { troops + spells + buildings }

    val troops: List<Card> = listOf(
        // --- COMMON TROOPS ---
        Card(
            id = 1, name = "Knight", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 3, hitpoints = 1452, damage = 167, attackSpeed = 1.1f,
            moveSpeed = 1.2f, range = 1.2f, targetType = TargetType.GROUND,
            description = "A tough melee fighter. He charges into battle alone!",
            iconEmoji = "⚔️"
        ),
        Card(
            id = 2, name = "Archers", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 3, hitpoints = 304, damage = 107, attackSpeed = 1.1f,
            moveSpeed = 1.0f, range = 5.5f, targetType = TargetType.GROUND_AND_AIR,
            spawnCount = 2, description = "A pair of unarmored ranged attackers.",
            iconEmoji = "🏹"
        ),
        Card(
            id = 3, name = "Goblins", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 2, hitpoints = 202, damage = 120, attackSpeed = 1.1f,
            moveSpeed = 1.5f, range = 1.0f, targetType = TargetType.GROUND,
            spawnCount = 3, description = "Three fast, unarmored melee attackers.",
            iconEmoji = "👺"
        ),
        Card(
            id = 4, name = "Minions", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 3, hitpoints = 252, damage = 84, attackSpeed = 1.0f,
            moveSpeed = 1.5f, range = 1.5f, targetType = TargetType.GROUND_AND_AIR,
            isFlying = true, spawnCount = 3, description = "Three fast flying melee fighters.",
            iconEmoji = "🦇"
        ),
        Card(
            id = 5, name = "Barbarians", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 5, hitpoints = 670, damage = 120, attackSpeed = 1.4f,
            moveSpeed = 1.0f, range = 1.0f, targetType = TargetType.GROUND,
            spawnCount = 4, description = "A horde of melee attackers with moderate hit points.",
            iconEmoji = "🪓"
        ),
        Card(
            id = 6, name = "Spear Goblins", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 2, hitpoints = 167, damage = 75, attackSpeed = 1.7f,
            moveSpeed = 1.2f, range = 5.0f, targetType = TargetType.GROUND_AND_AIR,
            spawnCount = 3, description = "Three unarmored ranged attackers.",
            iconEmoji = "🗡️"
        ),
        Card(
            id = 7, name = "Skeleton Army", type = CardType.TROOP, rarity = Rarity.EPIC,
            elixirCost = 3, hitpoints = 67, damage = 67, attackSpeed = 1.0f,
            moveSpeed = 1.2f, range = 0.5f, targetType = TargetType.GROUND,
            spawnCount = 15, description = "Spawns a swarm of Skeletons!",
            iconEmoji = "💀"
        ),
        Card(
            id = 8, name = "Fire Spirits", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 2, hitpoints = 91, damage = 178, attackSpeed = 0.3f,
            moveSpeed = 1.5f, range = 2.0f, targetType = TargetType.GROUND_AND_AIR,
            spawnCount = 3, splashRadius = 1.5f,
            description = "Kamikaze fire spirits that deal splash damage.",
            iconEmoji = "🔥"
        ),
        Card(
            id = 9, name = "Royal Giant", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 6, hitpoints = 2544, damage = 180, attackSpeed = 1.7f,
            moveSpeed = 0.8f, range = 6.5f, targetType = TargetType.BUILDINGS,
            description = "Slow but tanky ranged building targeter.",
            iconEmoji = "👑"
        ),
        Card(
            id = 10, name = "Elite Barbarians", type = CardType.TROOP, rarity = Rarity.COMMON,
            elixirCost = 6, hitpoints = 1010, damage = 254, attackSpeed = 1.4f,
            moveSpeed = 1.5f, range = 1.0f, targetType = TargetType.GROUND,
            spawnCount = 2, description = "Two fast, hard-hitting barbarians.",
            iconEmoji = "⚡"
        ),

        // --- RARE TROOPS ---
        Card(
            id = 11, name = "Giant", type = CardType.TROOP, rarity = Rarity.RARE,
            elixirCost = 5, hitpoints = 3344, damage = 211, attackSpeed = 1.5f,
            moveSpeed = 0.8f, range = 1.0f, targetType = TargetType.BUILDINGS,
            description = "Slow but durable. Targets buildings.",
            iconEmoji = "🗿"
        ),
        Card(
            id = 12, name = "Musketeer", type = CardType.TROOP, rarity = Rarity.RARE,
            elixirCost = 4, hitpoints = 720, damage = 181, attackSpeed = 1.0f,
            moveSpeed = 1.0f, range = 6.0f, targetType = TargetType.GROUND_AND_AIR,
            description = "Ranged attacker with high damage per second.",
            iconEmoji = "🔫"
        ),
        Card(
            id = 13, name = "Valkyrie", type = CardType.TROOP, rarity = Rarity.RARE,
            elixirCost = 4, hitpoints = 1654, damage = 221, attackSpeed = 1.5f,
            moveSpeed = 1.0f, range = 1.2f, targetType = TargetType.GROUND,
            splashRadius = 1.5f, description = "Tough melee splash fighter.",
            iconEmoji = "💃"
        ),
        Card(
            id = 14, name = "Hog Rider", type = CardType.TROOP, rarity = Rarity.RARE,
            elixirCost = 4, hitpoints = 1696, damage = 264, attackSpeed = 1.6f,
            moveSpeed = 2.0f, range = 1.0f, targetType = TargetType.BUILDINGS,
            description = "Fast building-targeting troop. Hog Ridaaaa!",
            iconEmoji = "🐗"
        ),
        Card(
            id = 15, name = "Wizard", type = CardType.TROOP, rarity = Rarity.RARE,
            elixirCost = 5, hitpoints = 720, damage = 234, attackSpeed = 1.4f,
            moveSpeed = 1.0f, range = 5.5f, targetType = TargetType.GROUND_AND_AIR,
            splashRadius = 1.5f, description = "Area damage wizard. Fries both air and ground.",
            iconEmoji = "🧙"
        ),
        Card(
            id = 16, name = "Mini P.E.K.K.A", type = CardType.TROOP, rarity = Rarity.RARE,
            elixirCost = 4, hitpoints = 1056, damage = 572, attackSpeed = 1.7f,
            moveSpeed = 1.2f, range = 1.2f, targetType = TargetType.GROUND,
            description = "Small but packs a huge punch. Pancakes!",
            iconEmoji = "🤖"
        ),
        Card(
            id = 17, name = "Three Musketeers", type = CardType.TROOP, rarity = Rarity.RARE,
            elixirCost = 9, hitpoints = 720, damage = 181, attackSpeed = 1.0f,
            moveSpeed = 1.0f, range = 6.0f, targetType = TargetType.GROUND_AND_AIR,
            spawnCount = 3, description = "Triple the firepower!",
            iconEmoji = "🎖️"
        ),
        Card(
            id = 18, name = "Battle Ram", type = CardType.TROOP, rarity = Rarity.RARE,
            elixirCost = 4, hitpoints = 756, damage = 318, attackSpeed = 1.0f,
            moveSpeed = 1.5f, range = 1.0f, targetType = TargetType.BUILDINGS,
            chargeSpeed = 2.5f, chargeDamageMultiplier = 2f,
            description = "Two Barbarians holding a log charge buildings!",
            iconEmoji = "🪵"
        ),

        // --- EPIC TROOPS ---
        Card(
            id = 19, name = "Prince", type = CardType.TROOP, rarity = Rarity.EPIC,
            elixirCost = 5, hitpoints = 1615, damage = 325, attackSpeed = 1.4f,
            moveSpeed = 1.2f, range = 1.85f, targetType = TargetType.GROUND,
            chargeSpeed = 2.5f, chargeDamageMultiplier = 2f,
            description = "Charges at enemies, dealing double damage!",
            iconEmoji = "🐎"
        ),
        Card(
            id = 20, name = "Baby Dragon", type = CardType.TROOP, rarity = Rarity.EPIC,
            elixirCost = 4, hitpoints = 1152, damage = 133, attackSpeed = 1.5f,
            moveSpeed = 1.2f, range = 3.5f, targetType = TargetType.GROUND_AND_AIR,
            isFlying = true, splashRadius = 1.0f,
            description = "Flying splash damage. Cute but deadly.",
            iconEmoji = "🐉"
        ),
        Card(
            id = 21, name = "Witch", type = CardType.TROOP, rarity = Rarity.EPIC,
            elixirCost = 5, hitpoints = 720, damage = 69, attackSpeed = 0.7f,
            moveSpeed = 1.0f, range = 5.5f, targetType = TargetType.GROUND_AND_AIR,
            splashRadius = 1.0f, description = "Summons Skeletons and does splash damage.",
            iconEmoji = "🧹"
        ),
        Card(
            id = 22, name = "Balloon", type = CardType.TROOP, rarity = Rarity.EPIC,
            elixirCost = 5, hitpoints = 1396, damage = 798, attackSpeed = 3.0f,
            moveSpeed = 1.0f, range = 0.5f, targetType = TargetType.BUILDINGS,
            isFlying = true, description = "Slow flying bomber. Devastating to buildings!",
            iconEmoji = "🎈"
        ),
        Card(
            id = 23, name = "P.E.K.K.A", type = CardType.TROOP, rarity = Rarity.EPIC,
            elixirCost = 7, hitpoints = 3760, damage = 678, attackSpeed = 1.8f,
            moveSpeed = 0.7f, range = 1.2f, targetType = TargetType.GROUND,
            description = "A very slow, very powerful armored fighter.",
            iconEmoji = "🦾"
        ),
        Card(
            id = 24, name = "Goblin Barrel", type = CardType.TROOP, rarity = Rarity.EPIC,
            elixirCost = 3, hitpoints = 202, damage = 120, attackSpeed = 1.1f,
            moveSpeed = 1.5f, range = 1.0f, targetType = TargetType.GROUND,
            spawnCount = 3, description = "Throw a barrel of Goblins anywhere in the Arena!",
            iconEmoji = "🛢️"
        ),
        Card(
            id = 25, name = "X-Bow", type = CardType.BUILDING, rarity = Rarity.EPIC,
            elixirCost = 6, hitpoints = 1330, damage = 26, attackSpeed = 0.3f,
            moveSpeed = 0f, range = 11.5f, targetType = TargetType.GROUND,
            lifetime = 40f, description = "Long range building that targets ground!",
            iconEmoji = "🏹"
        ),

        // --- LEGENDARY TROOPS ---
        Card(
            id = 26, name = "Mega Knight", type = CardType.TROOP, rarity = Rarity.LEGENDARY,
            elixirCost = 7, hitpoints = 3300, damage = 222, attackSpeed = 1.7f,
            moveSpeed = 0.9f, range = 1.2f, targetType = TargetType.GROUND,
            splashRadius = 1.8f, description = "Jumps on enemies dealing splash. Deploy damage!",
            iconEmoji = "👹"
        ),
        Card(
            id = 27, name = "Lava Hound", type = CardType.TROOP, rarity = Rarity.LEGENDARY,
            elixirCost = 7, hitpoints = 3150, damage = 45, attackSpeed = 1.3f,
            moveSpeed = 0.7f, range = 2.0f, targetType = TargetType.BUILDINGS,
            isFlying = true, description = "Tanky flying unit. Splits into Lava Pups on death!",
            iconEmoji = "🌋"
        ),
        Card(
            id = 28, name = "Sparky", type = CardType.TROOP, rarity = Rarity.LEGENDARY,
            elixirCost = 6, hitpoints = 1200, damage = 1100, attackSpeed = 4.0f,
            moveSpeed = 0.7f, range = 5.0f, targetType = TargetType.GROUND,
            splashRadius = 1.0f, description = "Charges up for massive electric blast!",
            iconEmoji = "⚡"
        ),
        Card(
            id = 29, name = "Inferno Dragon", type = CardType.TROOP, rarity = Rarity.LEGENDARY,
            elixirCost = 4, hitpoints = 1070, damage = 30, attackSpeed = 0.4f,
            moveSpeed = 1.0f, range = 4.0f, targetType = TargetType.GROUND_AND_AIR,
            isFlying = true, description = "Flying dragon with ramping inferno beam!",
            iconEmoji = "🐲"
        ),
        Card(
            id = 30, name = "Lumberjack", type = CardType.TROOP, rarity = Rarity.LEGENDARY,
            elixirCost = 4, hitpoints = 1060, damage = 200, attackSpeed = 0.7f,
            moveSpeed = 1.5f, range = 1.0f, targetType = TargetType.GROUND,
            description = "Fast melee fighter. Drops Rage spell on death!",
            iconEmoji = "🪓"
        ),
        Card(
            id = 31, name = "Electro Wizard", type = CardType.TROOP, rarity = Rarity.LEGENDARY,
            elixirCost = 4, hitpoints = 590, damage = 200, attackSpeed = 1.7f,
            moveSpeed = 1.0f, range = 5.0f, targetType = TargetType.GROUND_AND_AIR,
            description = "Stuns targets with each attack! Zaps on deploy!",
            iconEmoji = "⚡"
        ),
        Card(
            id = 32, name = "Golem", type = CardType.TROOP, rarity = Rarity.EPIC,
            elixirCost = 8, hitpoints = 4256, damage = 195, attackSpeed = 2.5f,
            moveSpeed = 0.5f, range = 1.0f, targetType = TargetType.BUILDINGS,
            description = "Massive tank. Splits into Golemites on death!",
            iconEmoji = "🪨"
        )
    )

    val spells: List<Card> = listOf(
        Card(
            id = 101, name = "Fireball", type = CardType.SPELL, rarity = Rarity.RARE,
            elixirCost = 4, hitpoints = 0, damage = 572, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 2.5f, description = "Annihilate a medium area with a ball of fire.",
            iconEmoji = "🔥"
        ),
        Card(
            id = 102, name = "Arrows", type = CardType.SPELL, rarity = Rarity.COMMON,
            elixirCost = 3, hitpoints = 0, damage = 243, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 4.0f, description = "Arrows rain on a large area.",
            iconEmoji = "🏹"
        ),
        Card(
            id = 103, name = "Zap", type = CardType.SPELL, rarity = Rarity.COMMON,
            elixirCost = 2, hitpoints = 0, damage = 159, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 2.5f, description = "Zaps enemies, briefly stunning them.",
            iconEmoji = "⚡"
        ),
        Card(
            id = 104, name = "Lightning", type = CardType.SPELL, rarity = Rarity.EPIC,
            elixirCost = 6, hitpoints = 0, damage = 877, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 3.5f, description = "Strikes the 3 enemies with most hitpoints.",
            iconEmoji = "🌩️"
        ),
        Card(
            id = 105, name = "Rocket", type = CardType.SPELL, rarity = Rarity.RARE,
            elixirCost = 6, hitpoints = 0, damage = 1232, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 2.0f, description = "Deals massive damage to a small area.",
            iconEmoji = "🚀"
        ),
        Card(
            id = 106, name = "Freeze", type = CardType.SPELL, rarity = Rarity.EPIC,
            elixirCost = 4, hitpoints = 0, damage = 95, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 3.0f, lifetime = 4f,
            description = "Freezes and damages enemy troops and buildings.",
            iconEmoji = "🥶"
        ),
        Card(
            id = 107, name = "Poison", type = CardType.SPELL, rarity = Rarity.EPIC,
            elixirCost = 4, hitpoints = 0, damage = 600, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 3.5f, lifetime = 8f,
            description = "Deals damage over time in a large area.",
            iconEmoji = "☠️"
        ),
        Card(
            id = 108, name = "Rage", type = CardType.SPELL, rarity = Rarity.EPIC,
            elixirCost = 2, hitpoints = 0, damage = 0, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 5.0f, lifetime = 7f,
            description = "Boosts movement and attack speed of troops.",
            iconEmoji = "😤"
        ),
        Card(
            id = 109, name = "Log", type = CardType.SPELL, rarity = Rarity.LEGENDARY,
            elixirCost = 2, hitpoints = 0, damage = 240, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND,
            spellRadius = 3.9f, description = "Rolling log that knocks back ground enemies.",
            iconEmoji = "🪵"
        ),
        Card(
            id = 110, name = "Tornado", type = CardType.SPELL, rarity = Rarity.EPIC,
            elixirCost = 3, hitpoints = 0, damage = 147, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            spellRadius = 5.5f, lifetime = 1f,
            description = "Pulls enemies toward the center.",
            iconEmoji = "🌪️"
        )
    )

    val buildings: List<Card> = listOf(
        Card(
            id = 201, name = "Cannon", type = CardType.BUILDING, rarity = Rarity.COMMON,
            elixirCost = 3, hitpoints = 742, damage = 137, attackSpeed = 0.8f,
            moveSpeed = 0f, range = 5.5f, targetType = TargetType.GROUND,
            lifetime = 30f, description = "Defensive building that targets ground troops.",
            iconEmoji = "🔫"
        ),
        Card(
            id = 202, name = "Tesla", type = CardType.BUILDING, rarity = Rarity.COMMON,
            elixirCost = 4, hitpoints = 954, damage = 150, attackSpeed = 1.0f,
            moveSpeed = 0f, range = 5.5f, targetType = TargetType.GROUND_AND_AIR,
            lifetime = 35f, description = "Electrifies ground and air troops.",
            iconEmoji = "⚡"
        ),
        Card(
            id = 203, name = "Inferno Tower", type = CardType.BUILDING, rarity = Rarity.RARE,
            elixirCost = 5, hitpoints = 1408, damage = 30, attackSpeed = 0.4f,
            moveSpeed = 0f, range = 6.0f, targetType = TargetType.GROUND_AND_AIR,
            lifetime = 35f, description = "Ramps up damage over time. Melts tanks!",
            iconEmoji = "🏗️"
        ),
        Card(
            id = 204, name = "Bomb Tower", type = CardType.BUILDING, rarity = Rarity.RARE,
            elixirCost = 4, hitpoints = 1126, damage = 184, attackSpeed = 1.6f,
            moveSpeed = 0f, range = 6.0f, targetType = TargetType.GROUND,
            lifetime = 35f, splashRadius = 1.5f,
            description = "Defensive splash damage building.",
            iconEmoji = "💣"
        ),
        Card(
            id = 205, name = "Goblin Hut", type = CardType.BUILDING, rarity = Rarity.RARE,
            elixirCost = 5, hitpoints = 1000, damage = 0, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            lifetime = 40f, spawnCardId = 6,
            description = "Spawns Spear Goblins to fight for you.",
            iconEmoji = "🏚️"
        ),
        Card(
            id = 206, name = "Tombstone", type = CardType.BUILDING, rarity = Rarity.RARE,
            elixirCost = 3, hitpoints = 422, damage = 0, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND,
            lifetime = 40f, description = "Spawns Skeletons periodically and on death.",
            iconEmoji = "🪦"
        ),
        Card(
            id = 207, name = "Furnace", type = CardType.BUILDING, rarity = Rarity.RARE,
            elixirCost = 4, hitpoints = 1000, damage = 0, attackSpeed = 0f,
            moveSpeed = 0f, range = 0f, targetType = TargetType.GROUND_AND_AIR,
            lifetime = 50f, spawnCardId = 8,
            description = "Spawns Fire Spirits periodically.",
            iconEmoji = "🔥"
        )
    )

    fun getCardById(id: Int): Card? = allCards.find { it.id == id }

    fun getCardsByRarity(rarity: Rarity): List<Card> = allCards.filter { it.rarity == rarity }

    fun getCardsByType(type: CardType): List<Card> = allCards.filter { it.type == type }

    fun getDefaultDeck(): List<Int> = listOf(1, 2, 11, 12, 14, 15, 101, 102)

    fun getStarterCards(): List<Int> = listOf(1, 2, 3, 4, 5, 6, 11, 12, 13, 14, 15, 16, 101, 102, 103, 201, 202)
}
