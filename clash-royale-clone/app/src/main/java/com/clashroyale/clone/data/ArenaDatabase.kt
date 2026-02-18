package com.clashroyale.clone.data

import com.clashroyale.clone.models.Arena

object ArenaDatabase {

    val arenas: List<Arena> = listOf(
        Arena(
            id = 1, name = "Goblin Stadium",
            trophyRequired = 0,
            primaryColor = 0xFF4CAF50,
            secondaryColor = 0xFF388E3C,
            accentColor = 0xFFFF9800,
            groundColor = 0xFF8BC34A,
            riverColor = 0xFF42A5F5,
            description = "Welcome to the Arena! Battle to earn trophies.",
            unlockedCardIds = listOf(1, 2, 3, 6, 102, 103, 201)
        ),
        Arena(
            id = 2, name = "Bone Pit",
            trophyRequired = 400,
            primaryColor = 0xFF795548,
            secondaryColor = 0xFF5D4037,
            accentColor = 0xFFBCAAA4,
            groundColor = 0xFF8D6E63,
            riverColor = 0xFF66BB6A,
            description = "Spooky skeletons lurk in the shadows...",
            unlockedCardIds = listOf(4, 7, 8, 11, 206)
        ),
        Arena(
            id = 3, name = "Barbarian Bowl",
            trophyRequired = 800,
            primaryColor = 0xFFFF5722,
            secondaryColor = 0xFFE64A19,
            accentColor = 0xFFFFAB91,
            groundColor = 0xFFBF360C,
            riverColor = 0xFF42A5F5,
            description = "Home of the Barbarians!",
            unlockedCardIds = listOf(5, 13, 16, 19, 204)
        ),
        Arena(
            id = 4, name = "P.E.K.K.A's Playhouse",
            trophyRequired = 1400,
            primaryColor = 0xFF3F51B5,
            secondaryColor = 0xFF303F9F,
            accentColor = 0xFF7986CB,
            groundColor = 0xFF5C6BC0,
            riverColor = 0xFF7C4DFF,
            description = "Where the machines come alive!",
            unlockedCardIds = listOf(15, 20, 21, 23, 202)
        ),
        Arena(
            id = 5, name = "Spell Valley",
            trophyRequired = 2000,
            primaryColor = 0xFF9C27B0,
            secondaryColor = 0xFF7B1FA2,
            accentColor = 0xFFCE93D8,
            groundColor = 0xFFAB47BC,
            riverColor = 0xFFE040FB,
            description = "Mystical spells fill the air!",
            unlockedCardIds = listOf(22, 104, 106, 107, 203)
        ),
        Arena(
            id = 6, name = "Builder's Workshop",
            trophyRequired = 2600,
            primaryColor = 0xFFFF9800,
            secondaryColor = 0xFFF57C00,
            accentColor = 0xFFFFCC80,
            groundColor = 0xFFFFB74D,
            riverColor = 0xFF42A5F5,
            description = "Where buildings are forged!",
            unlockedCardIds = listOf(9, 18, 25, 205, 207)
        ),
        Arena(
            id = 7, name = "Royal Arena",
            trophyRequired = 3400,
            primaryColor = 0xFF2196F3,
            secondaryColor = 0xFF1976D2,
            accentColor = 0xFF64B5F6,
            groundColor = 0xFF1E88E5,
            riverColor = 0xFF82B1FF,
            description = "The King's home arena!",
            unlockedCardIds = listOf(10, 17, 24, 105, 110)
        ),
        Arena(
            id = 8, name = "Frozen Peak",
            trophyRequired = 4000,
            primaryColor = 0xFF00BCD4,
            secondaryColor = 0xFF0097A7,
            accentColor = 0xFF80DEEA,
            groundColor = 0xFFB2EBF2,
            riverColor = 0xFF18FFFF,
            description = "Ice cold battles at the peak!",
            unlockedCardIds = listOf(27, 29, 31, 108, 109)
        ),
        Arena(
            id = 9, name = "Legendary Arena",
            trophyRequired = 5000,
            primaryColor = 0xFFFFD700,
            secondaryColor = 0xFFFFC107,
            accentColor = 0xFFFFECB3,
            groundColor = 0xFFFFF176,
            riverColor = 0xFFFFD740,
            description = "Only the best of the best battle here!",
            unlockedCardIds = listOf(26, 28, 30, 32)
        )
    )

    fun getArenaForTrophies(trophies: Int): Arena {
        return arenas.lastOrNull { trophies >= it.trophyRequired } ?: arenas.first()
    }

    fun getNextArena(currentArena: Arena): Arena? {
        val idx = arenas.indexOf(currentArena)
        return if (idx < arenas.size - 1) arenas[idx + 1] else null
    }

    fun getArenaById(id: Int): Arena? = arenas.find { it.id == id }
}
