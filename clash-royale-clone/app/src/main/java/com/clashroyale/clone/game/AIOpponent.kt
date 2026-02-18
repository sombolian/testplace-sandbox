package com.clashroyale.clone.game

import com.clashroyale.clone.models.*
import com.clashroyale.clone.utils.GameConstants
import kotlin.math.abs
import kotlin.random.Random

enum class AIDifficulty(val reactionDelay: Float, val elixirThreshold: Float, val smartness: Float) {
    EASY(3.0f, 8f, 0.3f),
    MEDIUM(1.5f, 6f, 0.6f),
    HARD(0.8f, 5f, 0.85f),
    EXPERT(0.3f, 4f, 0.95f)
}

class AIOpponent(
    private val engine: BattleEngine,
    private val difficulty: AIDifficulty = AIDifficulty.MEDIUM
) {
    private var decisionTimer: Float = 2.0f
    private var lastPlayTime: Float = 0f
    private var consecutivePlays: Int = 0

    // Strategy state
    private var isDefending: Boolean = false
    private var rushMode: Boolean = false
    private var preferredLane: Int = 0 // 0 = left, 1 = right

    fun update(dt: Float, gameTime: Float) {
        if (engine.gameState.phase == GamePhase.COUNTDOWN || engine.gameState.phase == GamePhase.ENDED) return

        decisionTimer -= dt
        if (decisionTimer > 0) return

        // Evaluate situation
        evaluateSituation()

        // Try to play a card
        val played = tryPlayCard(gameTime)

        // Reset timer based on difficulty and whether we played
        decisionTimer = if (played) {
            difficulty.reactionDelay + Random.nextFloat() * 1.5f
        } else {
            0.5f + Random.nextFloat() * 0.5f
        }
    }

    private fun evaluateSituation() {
        // Check if we need to defend
        val playerUnitsOnOurSide = engine.playerEntities.count { it.isAlive && it.y < GameConstants.RIVER_Y }
        val threatLevel = calculateThreatLevel()

        isDefending = threatLevel > 0.4f || playerUnitsOnOurSide >= 3

        // Rush mode in double elixir or when we have an advantage
        rushMode = engine.gameState.phase == GamePhase.DOUBLE_ELIXIR ||
                engine.gameState.phase == GamePhase.OVERTIME ||
                (engine.gameState.opponentCrowns > engine.gameState.playerCrowns && engine.gameState.timeRemaining < 60f)

        // Decide preferred lane
        val leftThreat = engine.playerEntities.count { it.isAlive && it.x < GameConstants.ARENA_WIDTH / 2 && it.y < GameConstants.RIVER_Y }
        val rightThreat = engine.playerEntities.count { it.isAlive && it.x >= GameConstants.ARENA_WIDTH / 2 && it.y < GameConstants.RIVER_Y }

        preferredLane = if (isDefending) {
            if (leftThreat > rightThreat) 0 else 1
        } else {
            // Attack weaker side
            val leftPrincessHp = engine.playerLeftPrincess.hpPercent
            val rightPrincessHp = engine.playerRightPrincess.hpPercent
            if (engine.playerLeftPrincess.isDestroyed) 0
            else if (engine.playerRightPrincess.isDestroyed) 1
            else if (leftPrincessHp < rightPrincessHp) 0
            else 1
        }
    }

    private fun calculateThreatLevel(): Float {
        var threat = 0f
        for (entity in engine.playerEntities) {
            if (!entity.isAlive) continue
            // More threat if closer to our towers
            val distToKing = entity.distanceTo(engine.opponentKing.x, engine.opponentKing.y)
            if (distToKing < 15f) {
                threat += (entity.card.damage * 1f / entity.card.attackSpeed) / 200f
                if (entity.y < GameConstants.RIVER_Y) {
                    threat += 0.2f
                }
            }
        }
        return threat.coerceIn(0f, 1f)
    }

    private fun tryPlayCard(gameTime: Float): Boolean {
        val elixir = engine.gameState.opponentElixir

        // Don't play if below threshold (unless defending or rush mode)
        if (!isDefending && !rushMode && elixir < difficulty.elixirThreshold) return false

        // At minimum need to afford something
        val affordableCards = engine.opponentHand.filter { elixir >= it.elixirCost }
        if (affordableCards.isEmpty()) return false

        // Choose card based on situation
        val cardToPlay = if (isDefending) {
            chooseDefensiveCard(affordableCards)
        } else {
            chooseOffensiveCard(affordableCards)
        }

        cardToPlay ?: return false

        // Choose placement position
        val (x, y) = choosePlacement(cardToPlay)

        return engine.playOpponentCard(cardToPlay, x, y)
    }

    private fun chooseDefensiveCard(cards: List<Card>): Card? {
        // Prefer splash damage against swarms
        val playerSwarmCount = engine.playerEntities.count { it.isAlive && it.card.spawnCount > 2 }

        if (playerSwarmCount > 0) {
            val splashCards = cards.filter { it.splashRadius > 0 || it.type == CardType.SPELL }
            if (splashCards.isNotEmpty() && Random.nextFloat() < difficulty.smartness) {
                return splashCards.random()
            }
        }

        // Prefer high DPS troops for defense
        val defensiveCards = cards.filter { it.type == CardType.TROOP || it.type == CardType.BUILDING }
            .sortedByDescending { it.dps }

        if (defensiveCards.isNotEmpty()) {
            // Smart AI picks best, dumb AI picks random
            return if (Random.nextFloat() < difficulty.smartness) {
                defensiveCards.first()
            } else {
                defensiveCards.random()
            }
        }

        return cards.randomOrNull()
    }

    private fun chooseOffensiveCard(cards: List<Card>): Card? {
        val tankCards = cards.filter { it.type == CardType.TROOP && it.hitpoints > 1500 }
        val supportCards = cards.filter { it.type == CardType.TROOP && it.hitpoints <= 1500 && it.range > 3f }
        val bridgeSpamCards = cards.filter { it.type == CardType.TROOP && (it.moveSpeed >= 1.5f || it.targetType == TargetType.BUILDINGS) }

        // Check if we already have a tank in front
        val hasActiveTank = engine.opponentEntities.any { it.isAlive && it.card.hitpoints > 1500 && it.y > GameConstants.RIVER_Y - 5f }

        // Build push behind tank
        if (hasActiveTank && supportCards.isNotEmpty() && Random.nextFloat() < difficulty.smartness) {
            return supportCards.random()
        }

        // Rush mode - prefer fast cards
        if (rushMode && bridgeSpamCards.isNotEmpty() && Random.nextFloat() < 0.6f) {
            return bridgeSpamCards.random()
        }

        // Start push with tank from back
        if (!hasActiveTank && tankCards.isNotEmpty() && engine.gameState.opponentElixir >= 7f && Random.nextFloat() < difficulty.smartness) {
            return tankCards.random()
        }

        // Default - play something reasonable
        val troops = cards.filter { it.type == CardType.TROOP }
        return if (troops.isNotEmpty()) troops.random() else cards.randomOrNull()
    }

    private fun choosePlacement(card: Card): Pair<Float, Float> {
        val laneX = if (preferredLane == 0) {
            3f + Random.nextFloat() * 3f
        } else {
            12f + Random.nextFloat() * 3f
        }

        return when {
            isDefending -> {
                // Place near threats
                val threat = engine.playerEntities.filter { it.isAlive && it.y < GameConstants.RIVER_Y }
                    .minByOrNull { it.y }

                if (threat != null) {
                    val defX = threat.x.coerceIn(2f, GameConstants.ARENA_WIDTH - 2f)
                    val defY = (threat.y - 2f).coerceIn(2f, GameConstants.RIVER_Y - 1f)
                    Pair(defX, defY)
                } else {
                    Pair(laneX, 8f)
                }
            }
            card.type == CardType.BUILDING -> {
                // Place buildings centrally behind river
                Pair(GameConstants.ARENA_WIDTH / 2f + Random.nextFloat() * 2f - 1f, 10f + Random.nextFloat() * 2f)
            }
            card.type == CardType.SPELL -> {
                // Target enemy clusters or towers
                val cluster = findEnemyCluster()
                cluster ?: Pair(laneX, 22f)
            }
            card.hitpoints > 2000 -> {
                // Tanks from back
                Pair(laneX, 4f + Random.nextFloat() * 2f)
            }
            card.targetType == TargetType.BUILDINGS || card.moveSpeed >= 1.5f -> {
                // Bridge spam - at the bridge
                Pair(if (preferredLane == 0) GameConstants.BRIDGE_LEFT_X else GameConstants.BRIDGE_RIGHT_X,
                    GameConstants.BRIDGE_Y - 0.5f)
            }
            else -> {
                // Medium troops - behind river
                Pair(laneX, 8f + Random.nextFloat() * 4f)
            }
        }
    }

    private fun findEnemyCluster(): Pair<Float, Float>? {
        val enemies = engine.playerEntities.filter { it.isAlive }
        if (enemies.size < 2) return null

        var bestX = 0f
        var bestY = 0f
        var bestCount = 0

        for (entity in enemies) {
            val nearby = enemies.count { it.distanceTo(entity) < 3f }
            if (nearby > bestCount) {
                bestCount = nearby
                bestX = entity.x
                bestY = entity.y
            }
        }

        return if (bestCount >= 2) Pair(bestX, bestY) else null
    }
}
