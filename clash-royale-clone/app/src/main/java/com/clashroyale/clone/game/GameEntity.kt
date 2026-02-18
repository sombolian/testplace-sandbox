package com.clashroyale.clone.game

import com.clashroyale.clone.models.Card
import com.clashroyale.clone.models.CardType
import com.clashroyale.clone.models.TargetType
import com.clashroyale.clone.utils.GameConstants
import kotlin.math.sqrt
import kotlin.math.abs

enum class EntityTeam { PLAYER, OPPONENT }
enum class EntityState { MOVING, ATTACKING, DYING, DEAD, IDLE, CHARGING }

open class GameEntity(
    val card: Card,
    val team: EntityTeam,
    var x: Float,
    var y: Float,
    var currentHp: Int = card.hitpoints,
    var state: EntityState = EntityState.IDLE
) {
    var maxHp: Int = card.hitpoints
    var targetEntity: GameEntity? = null
    var attackCooldown: Float = 0f
    var stateTimer: Float = 0f
    var deathTimer: Float = 0.5f
    var isFrozen: Boolean = false
    var freezeTimer: Float = 0f
    var isRaged: Boolean = false
    var rageTimer: Float = 0f
    var spawnAnimTimer: Float = 0.3f
    var isSpawning: Boolean = true

    // For charge attacks
    var isCharging: Boolean = false
    var chargeDistanceTraveled: Float = 0f

    // Unique ID for tracking
    val entityId: Long = nextId++

    val isAlive: Boolean get() = currentHp > 0 && state != EntityState.DEAD
    val isDying: Boolean get() = state == EntityState.DYING
    val isFlying: Boolean get() = card.isFlying
    val hpPercent: Float get() = currentHp.toFloat() / maxHp.toFloat()

    val effectiveAttackSpeed: Float get() {
        val base = card.attackSpeed
        return if (isRaged) base * 0.65f else base
    }

    val effectiveMoveSpeed: Float get() {
        val base = card.moveSpeed
        return if (isRaged) base * 1.4f else base
    }

    fun distanceTo(other: GameEntity): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun distanceTo(tx: Float, ty: Float): Float {
        val dx = x - tx
        val dy = y - ty
        return sqrt(dx * dx + dy * dy)
    }

    fun takeDamage(damage: Int) {
        if (!isAlive) return
        currentHp = (currentHp - damage).coerceAtLeast(0)
        if (currentHp <= 0) {
            state = EntityState.DYING
            stateTimer = 0f
        }
    }

    fun canAttack(target: GameEntity): Boolean {
        if (!isAlive || !target.isAlive) return false
        if (isFrozen) return false

        // Check target type compatibility
        when (card.targetType) {
            TargetType.GROUND -> if (target.isFlying) return false
            TargetType.AIR -> if (!target.isFlying) return false
            TargetType.BUILDINGS -> if (target.card.type != CardType.BUILDING && target !is TowerEntity) return false
            TargetType.GROUND_AND_AIR -> { /* can attack anything */ }
        }

        return true
    }

    fun moveToward(tx: Float, ty: Float, dt: Float) {
        if (isFrozen || !isAlive) return
        val dx = tx - x
        val dy = ty - y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.1f) return

        val speed = effectiveMoveSpeed * dt
        x += (dx / dist) * speed
        y += (dy / dist) * speed
    }

    companion object {
        private var nextId: Long = 1
        fun resetIdCounter() { nextId = 1 }
    }
}

class TowerEntity(
    val towerType: TowerType,
    team: EntityTeam,
    x: Float,
    y: Float,
    hp: Int,
    val towerDamage: Int,
    val towerRange: Float,
    val towerAttackSpeed: Float
) : GameEntity(
    card = Card(
        id = -1, name = towerType.label, type = CardType.BUILDING,
        rarity = com.clashroyale.clone.models.Rarity.COMMON,
        elixirCost = 0, hitpoints = hp, damage = towerDamage,
        attackSpeed = towerAttackSpeed, moveSpeed = 0f, range = towerRange,
        targetType = TargetType.GROUND_AND_AIR
    ),
    team = team, x = x, y = y, currentHp = hp
) {
    var isDestroyed: Boolean = false
    var isActive: Boolean = towerType != TowerType.KING // King tower activates when hit or princess destroyed

    init {
        maxHp = hp
        state = EntityState.IDLE
        isSpawning = false
    }
}

enum class TowerType(val label: String) {
    KING("King Tower"),
    PRINCESS_LEFT("Left Princess Tower"),
    PRINCESS_RIGHT("Right Princess Tower")
}

class ProjectileEntity(
    val source: GameEntity,
    val target: GameEntity,
    var projX: Float,
    var projY: Float,
    val damage: Int,
    val speed: Float = 12f,
    val isSplash: Boolean = false,
    val splashRadius: Float = 0f
) {
    var isActive: Boolean = true
    val targetX: Float get() = target.x
    val targetY: Float get() = target.y

    fun update(dt: Float): Boolean {
        if (!isActive) return false
        val dx = target.x - projX
        val dy = target.y - projY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < 0.5f) {
            isActive = false
            return true // hit
        }

        val moveAmount = speed * dt
        projX += (dx / dist) * moveAmount
        projY += (dy / dist) * moveAmount
        return false
    }
}

class SpellEffect(
    val card: Card,
    val team: EntityTeam,
    val centerX: Float,
    val centerY: Float,
    var timer: Float = 0.5f,
    var tickTimer: Float = 0f,
    val totalDuration: Float = 0.5f
) {
    var isActive: Boolean = true
    val radius: Float get() = card.spellRadius
    var hasDealtDamage: Boolean = false

    fun update(dt: Float) {
        timer -= dt
        tickTimer += dt
        if (timer <= 0f) {
            isActive = false
        }
    }

    fun isInRange(entity: GameEntity): Boolean {
        val dx = entity.x - centerX
        val dy = entity.y - centerY
        return sqrt(dx * dx + dy * dy) <= radius
    }
}
