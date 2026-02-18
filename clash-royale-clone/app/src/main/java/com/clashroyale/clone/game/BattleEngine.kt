package com.clashroyale.clone.game

import com.clashroyale.clone.data.CardDatabase
import com.clashroyale.clone.models.*
import com.clashroyale.clone.utils.GameConstants
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.min

class BattleEngine(
    private val playerDeck: List<Card>,
    private val opponentDeck: List<Card>
) {
    val gameState = GameState()
    val playerEntities = mutableListOf<GameEntity>()
    val opponentEntities = mutableListOf<GameEntity>()
    val projectiles = mutableListOf<ProjectileEntity>()
    val spellEffects = mutableListOf<SpellEffect>()

    // Towers
    lateinit var playerKing: TowerEntity
    lateinit var playerLeftPrincess: TowerEntity
    lateinit var playerRightPrincess: TowerEntity
    lateinit var opponentKing: TowerEntity
    lateinit var opponentLeftPrincess: TowerEntity
    lateinit var opponentRightPrincess: TowerEntity

    val allTowers: List<TowerEntity> get() = listOf(
        playerKing, playerLeftPrincess, playerRightPrincess,
        opponentKing, opponentLeftPrincess, opponentRightPrincess
    )

    // Card hand management
    var playerHand = mutableListOf<Card>()
    var playerNextCard: Card? = null
    private var playerDeckQueue = mutableListOf<Card>()

    var opponentHand = mutableListOf<Card>()
    var opponentNextCard: Card? = null
    private var opponentDeckQueue = mutableListOf<Card>()

    // Callbacks
    var onGameOver: ((BattleResult) -> Unit)? = null
    var onCrownEarned: ((EntityTeam, Int) -> Unit)? = null
    var onTowerDestroyed: ((TowerEntity) -> Unit)? = null

    private var countdownTimer = GameConstants.COUNTDOWN_DURATION
    private var gameTime = 0f

    fun initialize() {
        GameEntity.resetIdCounter()

        // Create towers
        playerKing = TowerEntity(
            TowerType.KING, EntityTeam.PLAYER,
            GameConstants.PLAYER_KING_X, GameConstants.PLAYER_KING_Y,
            GameConstants.KING_TOWER_HP, GameConstants.KING_TOWER_DAMAGE,
            GameConstants.KING_TOWER_RANGE, GameConstants.KING_TOWER_ATTACK_SPEED
        )
        playerLeftPrincess = TowerEntity(
            TowerType.PRINCESS_LEFT, EntityTeam.PLAYER,
            GameConstants.PLAYER_LEFT_PRINCESS_X, GameConstants.PLAYER_LEFT_PRINCESS_Y,
            GameConstants.PRINCESS_TOWER_HP, GameConstants.PRINCESS_TOWER_DAMAGE,
            GameConstants.PRINCESS_TOWER_RANGE, GameConstants.PRINCESS_TOWER_ATTACK_SPEED
        )
        playerRightPrincess = TowerEntity(
            TowerType.PRINCESS_RIGHT, EntityTeam.PLAYER,
            GameConstants.PLAYER_RIGHT_PRINCESS_X, GameConstants.PLAYER_RIGHT_PRINCESS_Y,
            GameConstants.PRINCESS_TOWER_HP, GameConstants.PRINCESS_TOWER_DAMAGE,
            GameConstants.PRINCESS_TOWER_RANGE, GameConstants.PRINCESS_TOWER_ATTACK_SPEED
        )

        opponentKing = TowerEntity(
            TowerType.KING, EntityTeam.OPPONENT,
            GameConstants.OPPONENT_KING_X, GameConstants.OPPONENT_KING_Y,
            GameConstants.KING_TOWER_HP, GameConstants.KING_TOWER_DAMAGE,
            GameConstants.KING_TOWER_RANGE, GameConstants.KING_TOWER_ATTACK_SPEED
        )
        opponentLeftPrincess = TowerEntity(
            TowerType.PRINCESS_LEFT, EntityTeam.OPPONENT,
            GameConstants.OPPONENT_LEFT_PRINCESS_X, GameConstants.OPPONENT_LEFT_PRINCESS_Y,
            GameConstants.PRINCESS_TOWER_HP, GameConstants.PRINCESS_TOWER_DAMAGE,
            GameConstants.PRINCESS_TOWER_RANGE, GameConstants.PRINCESS_TOWER_ATTACK_SPEED
        )
        opponentRightPrincess = TowerEntity(
            TowerType.PRINCESS_RIGHT, EntityTeam.OPPONENT,
            GameConstants.OPPONENT_RIGHT_PRINCESS_X, GameConstants.OPPONENT_RIGHT_PRINCESS_Y,
            GameConstants.PRINCESS_TOWER_HP, GameConstants.PRINCESS_TOWER_DAMAGE,
            GameConstants.PRINCESS_TOWER_RANGE, GameConstants.PRINCESS_TOWER_ATTACK_SPEED
        )

        // Initialize hands
        playerDeckQueue = playerDeck.shuffled().toMutableList()
        opponentDeckQueue = opponentDeck.shuffled().toMutableList()

        repeat(4) {
            playerHand.add(drawFromDeck(playerDeckQueue))
            opponentHand.add(drawFromDeck(opponentDeckQueue))
        }
        playerNextCard = drawFromDeck(playerDeckQueue)
        opponentNextCard = drawFromDeck(opponentDeckQueue)

        gameState.playerElixir = GameConstants.STARTING_ELIXIR
        gameState.opponentElixir = GameConstants.STARTING_ELIXIR
        gameState.phase = GamePhase.COUNTDOWN
        gameState.timeRemaining = GameConstants.REGULAR_TIME
    }

    private fun drawFromDeck(queue: MutableList<Card>): Card {
        if (queue.isEmpty()) {
            // Reshuffle - in a real game this would be the full deck
            queue.addAll(playerDeck.shuffled())
        }
        return queue.removeAt(0)
    }

    fun update(dt: Float) {
        if (gameState.isPaused || gameState.isGameOver) return

        when (gameState.phase) {
            GamePhase.COUNTDOWN -> {
                countdownTimer -= dt
                if (countdownTimer <= 0f) {
                    gameState.phase = GamePhase.REGULAR_TIME
                    gameState.elixirRate = GameConstants.ELIXIR_RATE_NORMAL
                }
                return
            }
            GamePhase.ENDED -> return
            else -> { /* continue */ }
        }

        gameTime += dt

        // Update timer
        gameState.timeRemaining -= dt

        // Phase transitions
        when (gameState.phase) {
            GamePhase.REGULAR_TIME -> {
                if (gameState.timeRemaining <= GameConstants.DOUBLE_ELIXIR_TIME) {
                    gameState.phase = GamePhase.DOUBLE_ELIXIR
                    gameState.elixirRate = GameConstants.ELIXIR_RATE_DOUBLE
                }
                if (gameState.timeRemaining <= 0f) {
                    if (gameState.playerCrowns != gameState.opponentCrowns) {
                        endGame()
                        return
                    } else {
                        gameState.phase = GamePhase.OVERTIME
                        gameState.timeRemaining = GameConstants.OVERTIME_DURATION
                        gameState.elixirRate = GameConstants.ELIXIR_RATE_DOUBLE
                    }
                }
            }
            GamePhase.DOUBLE_ELIXIR -> {
                if (gameState.timeRemaining <= 0f) {
                    if (gameState.playerCrowns != gameState.opponentCrowns) {
                        endGame()
                        return
                    } else {
                        gameState.phase = GamePhase.OVERTIME
                        gameState.timeRemaining = GameConstants.OVERTIME_DURATION
                    }
                }
            }
            GamePhase.OVERTIME -> {
                if (gameState.timeRemaining <= 0f) {
                    endGame()
                    return
                }
            }
            else -> {}
        }

        // Update elixir
        gameState.addPlayerElixir(gameState.elixirRate * dt)
        gameState.addOpponentElixir(gameState.elixirRate * dt)

        // Update all entities
        updateEntities(playerEntities, EntityTeam.PLAYER, dt)
        updateEntities(opponentEntities, EntityTeam.OPPONENT, dt)

        // Update towers
        updateTowers(dt)

        // Update projectiles
        updateProjectiles(dt)

        // Update spell effects
        updateSpellEffects(dt)

        // Clean up dead entities
        cleanupEntities()

        // Check win conditions
        checkWinConditions()
    }

    private fun updateEntities(entities: MutableList<GameEntity>, team: EntityTeam, dt: Float) {
        for (entity in entities) {
            if (!entity.isAlive) continue

            // Spawn animation
            if (entity.isSpawning) {
                entity.spawnAnimTimer -= dt
                if (entity.spawnAnimTimer <= 0f) {
                    entity.isSpawning = false
                    entity.state = EntityState.MOVING
                }
                continue
            }

            // Freeze timer
            if (entity.isFrozen) {
                entity.freezeTimer -= dt
                if (entity.freezeTimer <= 0f) {
                    entity.isFrozen = false
                }
                continue
            }

            // Rage timer
            if (entity.isRaged) {
                entity.rageTimer -= dt
                if (entity.rageTimer <= 0f) {
                    entity.isRaged = false
                }
            }

            // Building entities don't move but can attack
            if (entity.card.type == CardType.BUILDING) {
                updateBuildingEntity(entity, team, dt)
                continue
            }

            // Find target
            val target = findBestTarget(entity, team)
            entity.targetEntity = target

            if (target != null && entity.distanceTo(target) <= entity.card.range) {
                // In range - attack
                entity.state = EntityState.ATTACKING
                entity.attackCooldown -= dt

                if (entity.attackCooldown <= 0f) {
                    performAttack(entity, target)
                    entity.attackCooldown = entity.effectiveAttackSpeed
                    entity.isCharging = false
                    entity.chargeDistanceTraveled = 0f
                }
            } else if (target != null) {
                // Move toward target
                entity.state = EntityState.MOVING

                // Handle charge
                if (entity.card.chargeSpeed > 0 && entity.chargeDistanceTraveled > 2f) {
                    entity.isCharging = true
                    val speed = entity.card.chargeSpeed * dt
                    val dx = target.x - entity.x
                    val dy = target.y - entity.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist > 0.1f) {
                        entity.x += (dx / dist) * speed
                        entity.y += (dy / dist) * speed
                    }
                    entity.chargeDistanceTraveled += speed
                } else {
                    entity.moveToward(target.x, target.y, dt)
                    if (entity.card.chargeSpeed > 0) {
                        entity.chargeDistanceTraveled += entity.effectiveMoveSpeed * dt
                    }
                }
            } else {
                // No target - move toward enemy side
                val targetY = if (team == EntityTeam.PLAYER) 0f else GameConstants.ARENA_HEIGHT
                entity.moveToward(entity.x, targetY, dt)
                entity.state = EntityState.MOVING
            }
        }
    }

    private fun updateBuildingEntity(entity: GameEntity, team: EntityTeam, dt: Float) {
        // Lifetime countdown
        if (entity.card.lifetime > 0) {
            entity.stateTimer += dt
            if (entity.stateTimer >= entity.card.lifetime) {
                entity.currentHp = 0
                entity.state = EntityState.DYING
                return
            }
        }

        // Find and attack targets
        if (entity.card.damage > 0) {
            val target = findBestTarget(entity, team)
            entity.targetEntity = target

            if (target != null && entity.distanceTo(target) <= entity.card.range) {
                entity.attackCooldown -= dt
                if (entity.attackCooldown <= 0f) {
                    performAttack(entity, target)
                    entity.attackCooldown = entity.card.attackSpeed
                }
            }
        }
    }

    private fun findBestTarget(entity: GameEntity, team: EntityTeam): GameEntity? {
        val enemies = if (team == EntityTeam.PLAYER) {
            opponentEntities + listOf(opponentKing, opponentLeftPrincess, opponentRightPrincess)
        } else {
            playerEntities + listOf(playerKing, playerLeftPrincess, playerRightPrincess)
        }

        val validTargets = enemies.filter { target ->
            target.isAlive && !target.isSpawning && entity.canAttack(target) &&
            (target !is TowerEntity || target.isActive || target.towerType != TowerType.KING) &&
            (target !is TowerEntity || !target.isDestroyed)
        }

        if (validTargets.isEmpty()) return null

        // Building targeters only go for buildings/towers
        if (entity.card.targetType == TargetType.BUILDINGS) {
            val buildings = validTargets.filter { it.card.type == CardType.BUILDING || it is TowerEntity }
            if (buildings.isNotEmpty()) {
                return buildings.minByOrNull { entity.distanceTo(it) }
            }
        }

        return validTargets.minByOrNull { entity.distanceTo(it) }
    }

    private fun performAttack(attacker: GameEntity, target: GameEntity) {
        var damage = attacker.card.damage

        // Charge damage multiplier
        if (attacker.isCharging && attacker.card.chargeDamageMultiplier > 1f) {
            damage = (damage * attacker.card.chargeDamageMultiplier).toInt()
        }

        if (attacker.card.range > 3f) {
            // Ranged - create projectile
            projectiles.add(ProjectileEntity(
                source = attacker,
                target = target,
                projX = attacker.x,
                projY = attacker.y,
                damage = damage,
                isSplash = attacker.card.splashRadius > 0,
                splashRadius = attacker.card.splashRadius
            ))
        } else {
            // Melee - instant damage
            if (attacker.card.splashRadius > 0) {
                // Splash damage
                val allTargets = if (attacker.team == EntityTeam.PLAYER) {
                    opponentEntities + listOf(opponentKing, opponentLeftPrincess, opponentRightPrincess)
                } else {
                    playerEntities + listOf(playerKing, playerLeftPrincess, playerRightPrincess)
                }
                allTargets.filter { it.isAlive && attacker.distanceTo(it) <= attacker.card.splashRadius + 0.5f && attacker.canAttack(it) }
                    .forEach { it.takeDamage(damage) }
            } else {
                target.takeDamage(damage)
            }
            checkTowerDamage(target)
        }
    }

    private fun updateTowers(dt: Float) {
        for (tower in allTowers) {
            if (tower.isDestroyed || !tower.isAlive) continue
            if (!tower.isActive) continue

            tower.attackCooldown -= dt
            if (tower.attackCooldown > 0) continue

            val enemies = if (tower.team == EntityTeam.PLAYER) {
                opponentEntities
            } else {
                playerEntities
            }

            val target = enemies.filter { it.isAlive && !it.isSpawning && tower.distanceTo(it) <= tower.towerRange }
                .minByOrNull { tower.distanceTo(it) }

            if (target != null) {
                tower.targetEntity = target
                projectiles.add(ProjectileEntity(
                    source = tower,
                    target = target,
                    projX = tower.x,
                    projY = tower.y,
                    damage = tower.towerDamage
                ))
                tower.attackCooldown = tower.towerAttackSpeed
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val proj = iterator.next()
            if (proj.update(dt)) {
                // Hit target
                if (proj.isSplash && proj.splashRadius > 0) {
                    val allTargets = if (proj.source.team == EntityTeam.PLAYER) {
                        opponentEntities.toList() + allTowers.filter { it.team == EntityTeam.OPPONENT }
                    } else {
                        playerEntities.toList() + allTowers.filter { it.team == EntityTeam.PLAYER }
                    }
                    allTargets.filter {
                        it.isAlive && it.distanceTo(proj.projX, proj.projY) <= proj.splashRadius
                    }.forEach {
                        it.takeDamage(proj.damage)
                        checkTowerDamage(it)
                    }
                } else {
                    if (proj.target.isAlive) {
                        proj.target.takeDamage(proj.damage)
                        checkTowerDamage(proj.target)
                    }
                }
                iterator.remove()
            } else if (!proj.target.isAlive) {
                iterator.remove()
            }
        }
    }

    private fun updateSpellEffects(dt: Float) {
        val iterator = spellEffects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            effect.update(dt)

            if (!effect.hasDealtDamage && effect.card.damage > 0) {
                val targets = if (effect.team == EntityTeam.PLAYER) {
                    opponentEntities.toList() + allTowers.filter { it.team == EntityTeam.OPPONENT }
                } else {
                    playerEntities.toList() + allTowers.filter { it.team == EntityTeam.PLAYER }
                }

                targets.filter { it.isAlive && effect.isInRange(it) }
                    .forEach {
                        it.takeDamage(effect.card.damage)
                        checkTowerDamage(it)
                    }

                // Freeze effect
                if (effect.card.name == "Freeze") {
                    targets.filter { it.isAlive && effect.isInRange(it) && it !is TowerEntity }
                        .forEach {
                            it.isFrozen = true
                            it.freezeTimer = effect.card.lifetime
                        }
                }

                effect.hasDealtDamage = true
            }

            if (!effect.isActive) {
                iterator.remove()
            }
        }
    }

    private fun checkTowerDamage(entity: GameEntity) {
        if (entity !is TowerEntity) return
        if (!entity.isAlive && !entity.isDestroyed) {
            entity.isDestroyed = true
            entity.state = EntityState.DEAD
            onTowerDestroyed?.invoke(entity)

            // Award crown
            if (entity.team == EntityTeam.OPPONENT) {
                gameState.playerCrowns++
                // Activate king tower if princess is destroyed
                if (entity.towerType != TowerType.KING) {
                    opponentKing.isActive = true
                }
                onCrownEarned?.invoke(EntityTeam.PLAYER, gameState.playerCrowns)
            } else {
                gameState.opponentCrowns++
                if (entity.towerType != TowerType.KING) {
                    playerKing.isActive = true
                }
                onCrownEarned?.invoke(EntityTeam.OPPONENT, gameState.opponentCrowns)
            }

            // King tower = 3 crowns
            if (entity.towerType == TowerType.KING) {
                if (entity.team == EntityTeam.OPPONENT) {
                    gameState.playerCrowns = 3
                } else {
                    gameState.opponentCrowns = 3
                }
                endGame()
            }
        }
    }

    private fun cleanupEntities() {
        playerEntities.removeAll { it.state == EntityState.DEAD || (!it.isAlive && !it.isDying) }
        opponentEntities.removeAll { it.state == EntityState.DEAD || (!it.isAlive && !it.isDying) }

        // Update dying entities
        (playerEntities + opponentEntities).filter { it.isDying }.forEach {
            it.deathTimer -= GameConstants.TICK_DURATION
            if (it.deathTimer <= 0f) {
                it.state = EntityState.DEAD
            }
        }
    }

    private fun checkWinConditions() {
        if (gameState.playerCrowns >= 3 || gameState.opponentCrowns >= 3) {
            endGame()
        }
    }

    private fun endGame() {
        gameState.phase = GamePhase.ENDED
        val isWin = gameState.playerCrowns > gameState.opponentCrowns
        val isDraw = gameState.playerCrowns == gameState.opponentCrowns

        val trophyChange = when {
            isWin -> GameConstants.WIN_TROPHIES
            isDraw -> 0
            else -> GameConstants.LOSS_TROPHIES
        }

        onGameOver?.invoke(BattleResult(
            playerCrowns = gameState.playerCrowns,
            opponentCrowns = gameState.opponentCrowns,
            isWin = isWin,
            trophyChange = trophyChange,
            battleDurationSeconds = gameTime.toInt()
        ))
    }

    // --- Player actions ---

    fun playCard(handIndex: Int, x: Float, y: Float): Boolean {
        if (handIndex !in playerHand.indices) return false
        val card = playerHand[handIndex]

        if (!gameState.canPlayerAfford(card.elixirCost)) return false

        // Validate placement zone
        if (y < GameConstants.DEPLOY_ZONE_PLAYER_MIN_Y || y > GameConstants.DEPLOY_ZONE_PLAYER_MAX_Y) return false
        if (x < 0.5f || x > GameConstants.ARENA_WIDTH - 0.5f) return false

        gameState.playerElixir -= card.elixirCost

        spawnCard(card, EntityTeam.PLAYER, x, y)

        // Replace card in hand
        playerHand[handIndex] = playerNextCard!!
        playerNextCard = drawFromDeck(playerDeckQueue)

        return true
    }

    fun playOpponentCard(card: Card, x: Float, y: Float): Boolean {
        if (!gameState.canOpponentAfford(card.elixirCost)) return false

        gameState.opponentElixir -= card.elixirCost
        spawnCard(card, EntityTeam.OPPONENT, x, y)

        // Replace card in opponent hand
        val idx = opponentHand.indexOf(card)
        if (idx >= 0) {
            opponentHand[idx] = opponentNextCard!!
            opponentNextCard = drawFromDeck(opponentDeckQueue)
        }

        return true
    }

    private fun spawnCard(card: Card, team: EntityTeam, x: Float, y: Float) {
        when (card.type) {
            CardType.TROOP -> {
                val count = card.spawnCount
                for (i in 0 until count) {
                    val offsetX = if (count > 1) ((i % 3) - 1) * 0.8f else 0f
                    val offsetY = if (count > 1) (i / 3) * 0.8f else 0f
                    val entity = GameEntity(card, team, x + offsetX, y + offsetY)
                    entity.state = EntityState.IDLE
                    entity.isSpawning = true
                    entity.spawnAnimTimer = 0.3f

                    if (team == EntityTeam.PLAYER) playerEntities.add(entity)
                    else opponentEntities.add(entity)
                }
            }
            CardType.SPELL -> {
                val effect = SpellEffect(
                    card = card,
                    team = team,
                    centerX = x,
                    centerY = y,
                    timer = if (card.lifetime > 0) card.lifetime else 0.5f,
                    totalDuration = if (card.lifetime > 0) card.lifetime else 0.5f
                )
                spellEffects.add(effect)
            }
            CardType.BUILDING -> {
                val entity = GameEntity(card, team, x, y)
                entity.state = EntityState.IDLE
                entity.isSpawning = true
                entity.spawnAnimTimer = 0.5f

                if (team == EntityTeam.PLAYER) playerEntities.add(entity)
                else opponentEntities.add(entity)
            }
        }
    }

    fun getCountdownTime(): Float = countdownTimer
    fun getGameTime(): Float = gameTime
}
