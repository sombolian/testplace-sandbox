package com.clashroyale.clone.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.clashroyale.clone.data.ArenaDatabase
import com.clashroyale.clone.models.*
import com.clashroyale.clone.utils.GameConstants
import kotlin.math.*

class BattleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    private var isRunning = false

    lateinit var engine: BattleEngine
    lateinit var aiOpponent: AIOpponent

    // Rendering
    private var tileSize: Float = 0f
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f
    private var arenaRect = RectF()

    // Paints
    private val groundPaint = Paint().apply { isAntiAlias = true }
    private val riverPaint = Paint().apply { isAntiAlias = true }
    private val bridgePaint = Paint().apply { isAntiAlias = true; color = Color.rgb(139, 90, 43) }
    private val towerPaint = Paint().apply { isAntiAlias = true }
    private val towerOutlinePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.BLACK }
    private val entityPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; color = Color.WHITE; isFakeBoldText = true }
    private val hpBarBgPaint = Paint().apply { isAntiAlias = true; color = Color.rgb(40, 40, 40) }
    private val hpBarPlayerPaint = Paint().apply { isAntiAlias = true; color = Color.rgb(76, 175, 80) }
    private val hpBarEnemyPaint = Paint().apply { isAntiAlias = true; color = Color.rgb(244, 67, 54) }
    private val projectilePaint = Paint().apply { isAntiAlias = true; color = Color.YELLOW }
    private val spellPaint = Paint().apply { isAntiAlias = true }
    private val shadowPaint = Paint().apply { isAntiAlias = true; color = Color.argb(60, 0, 0, 0) }
    private val deployZonePaint = Paint().apply { isAntiAlias = true; color = Color.argb(30, 76, 175, 80) }
    private val gridPaint = Paint().apply { isAntiAlias = true; color = Color.argb(20, 0, 0, 0); strokeWidth = 1f; style = Paint.Style.STROKE }
    private val countdownPaint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; color = Color.WHITE; isFakeBoldText = true; setShadowLayer(8f, 0f, 0f, Color.BLACK) }
    private val timerPaint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER; color = Color.WHITE; isFakeBoldText = true; setShadowLayer(4f, 0f, 0f, Color.BLACK) }
    private val crownPaint = Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER }

    // Card drag state
    private var isDragging = false
    private var dragCardIndex = -1
    private var dragX = 0f
    private var dragY = 0f
    private var dragGameX = 0f
    private var dragGameY = 0f
    private var isValidPlacement = false

    // UI callback
    var onCardDragStart: ((Int) -> Unit)? = null
    var onCardDragEnd: (() -> Unit)? = null
    var onCardPlayed: ((Int) -> Unit)? = null

    // Arena theme colors
    private var arenaGroundColor = Color.rgb(76, 175, 80)
    private var arenaRiverColor = Color.rgb(66, 165, 245)
    private var arenaDarkGround = Color.rgb(56, 142, 60)

    // Animation timers
    private var animTime = 0f

    // Entity color map
    private val troopColors = mapOf(
        "Knight" to Color.rgb(160, 160, 180),
        "Archers" to Color.rgb(200, 100, 150),
        "Giant" to Color.rgb(220, 180, 120),
        "Musketeer" to Color.rgb(180, 120, 200),
        "Valkyrie" to Color.rgb(255, 120, 50),
        "Hog Rider" to Color.rgb(200, 150, 50),
        "Wizard" to Color.rgb(130, 50, 220),
        "Mini P.E.K.K.A" to Color.rgb(50, 100, 220),
        "Prince" to Color.rgb(100, 200, 100),
        "Baby Dragon" to Color.rgb(100, 220, 100),
        "Witch" to Color.rgb(150, 50, 180),
        "Balloon" to Color.rgb(220, 50, 50),
        "P.E.K.K.A" to Color.rgb(40, 60, 150),
        "Mega Knight" to Color.rgb(180, 50, 50),
        "Golem" to Color.rgb(100, 80, 60),
        "Lava Hound" to Color.rgb(180, 80, 30),
        "Sparky" to Color.rgb(220, 200, 50),
        "Goblins" to Color.rgb(100, 200, 50),
        "Spear Goblins" to Color.rgb(100, 200, 50),
        "Minions" to Color.rgb(120, 80, 200),
        "Barbarians" to Color.rgb(220, 180, 80),
        "Skeleton Army" to Color.rgb(200, 200, 220),
        "Fire Spirits" to Color.rgb(255, 100, 0),
        "Royal Giant" to Color.rgb(180, 160, 200),
        "Elite Barbarians" to Color.rgb(240, 200, 50),
        "Three Musketeers" to Color.rgb(180, 120, 200),
        "Battle Ram" to Color.rgb(139, 90, 43),
        "Goblin Barrel" to Color.rgb(100, 200, 50),
        "Inferno Dragon" to Color.rgb(255, 60, 0),
        "Lumberjack" to Color.rgb(139, 100, 60),
        "Electro Wizard" to Color.rgb(50, 150, 255)
    )

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun setArenaTheme(arenaId: Int) {
        val arena = ArenaDatabase.getArenaById(arenaId)
        if (arena != null) {
            arenaGroundColor = arena.groundColor.toInt()
            arenaRiverColor = arena.riverColor.toInt()
            arenaDarkGround = darkenColor(arenaGroundColor, 0.8f)
        }
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16) and 0xFF) * factor
        val g = ((color shr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        calculateDimensions(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        try {
            gameThread?.join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun calculateDimensions(width: Int, height: Int) {
        // Calculate tile size to fit arena in view
        val availableHeight = height * 0.85f // Leave room for UI
        tileSize = min(width / GameConstants.ARENA_WIDTH, availableHeight / GameConstants.ARENA_HEIGHT)

        val arenaPixelWidth = GameConstants.ARENA_WIDTH * tileSize
        val arenaPixelHeight = GameConstants.ARENA_HEIGHT * tileSize

        offsetX = (width - arenaPixelWidth) / 2f
        offsetY = (height - arenaPixelHeight) / 2f

        arenaRect = RectF(offsetX, offsetY, offsetX + arenaPixelWidth, offsetY + arenaPixelHeight)
    }

    private fun gameToScreenX(gx: Float): Float = offsetX + gx * tileSize
    private fun gameToScreenY(gy: Float): Float = offsetY + gy * tileSize
    private fun screenToGameX(sx: Float): Float = (sx - offsetX) / tileSize
    private fun screenToGameY(sy: Float): Float = (sy - offsetY) / tileSize

    override fun run() {
        var lastTime = System.nanoTime()
        val targetDt = 1_000_000_000L / GameConstants.GAME_TICK_RATE

        while (isRunning) {
            val now = System.nanoTime()
            val elapsed = now - lastTime

            if (elapsed >= targetDt) {
                val dt = elapsed / 1_000_000_000f
                lastTime = now

                // Update game
                if (::engine.isInitialized) {
                    engine.update(dt.coerceAtMost(0.05f))
                    if (::aiOpponent.isInitialized) {
                        aiOpponent.update(dt.coerceAtMost(0.05f), engine.getGameTime())
                    }
                }

                animTime += dt

                // Render
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        drawGame(canvas)
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            } else {
                // Small sleep to avoid busy-waiting
                try {
                    Thread.sleep(1)
                } catch (_: InterruptedException) {}
            }
        }
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(Color.rgb(20, 20, 30))

        if (!::engine.isInitialized) return

        drawArena(canvas)
        drawDeployZone(canvas)
        drawTowers(canvas)
        drawEntities(canvas)
        drawProjectiles(canvas)
        drawSpellEffects(canvas)
        drawDragPreview(canvas)
        drawTimer(canvas)
        drawCrowns(canvas)
        drawCountdown(canvas)
    }

    private fun drawArena(canvas: Canvas) {
        // Main ground
        groundPaint.color = arenaGroundColor
        canvas.drawRect(arenaRect, groundPaint)

        // Checkerboard pattern for visual texture
        val darkGround = Paint().apply { color = arenaDarkGround; isAntiAlias = true }
        for (tx in 0 until GameConstants.ARENA_WIDTH.toInt()) {
            for (ty in 0 until GameConstants.ARENA_HEIGHT.toInt()) {
                if ((tx + ty) % 2 == 0) {
                    canvas.drawRect(
                        gameToScreenX(tx.toFloat()), gameToScreenY(ty.toFloat()),
                        gameToScreenX(tx + 1f), gameToScreenY(ty + 1f),
                        darkGround
                    )
                }
            }
        }

        // River
        riverPaint.color = arenaRiverColor
        val riverTop = gameToScreenY(GameConstants.RIVER_Y - 0.8f)
        val riverBottom = gameToScreenY(GameConstants.RIVER_Y + 0.8f)
        canvas.drawRect(arenaRect.left, riverTop, arenaRect.right, riverBottom, riverPaint)

        // River wave animation
        val wavePaint = Paint().apply {
            color = Color.argb(40, 255, 255, 255); isAntiAlias = true; strokeWidth = 2f; style = Paint.Style.STROKE
        }
        for (i in 0..5) {
            val waveY = riverTop + (riverBottom - riverTop) * (i / 6f)
            val path = Path()
            path.moveTo(arenaRect.left, waveY)
            var wx = arenaRect.left
            while (wx < arenaRect.right) {
                path.quadTo(
                    wx + tileSize * 0.5f, waveY + sin((animTime * 3f + wx * 0.02f).toDouble()).toFloat() * tileSize * 0.15f,
                    wx + tileSize, waveY
                )
                wx += tileSize
            }
            canvas.drawPath(path, wavePaint)
        }

        // Bridges
        val bridgeWidth = tileSize * 2.5f
        val bridgeHeight = tileSize * 2f
        bridgePaint.color = Color.rgb(139, 90, 43)

        // Left bridge
        val lbx = gameToScreenX(GameConstants.BRIDGE_LEFT_X) - bridgeWidth / 2
        canvas.drawRoundRect(
            lbx, riverTop - tileSize * 0.2f,
            lbx + bridgeWidth, riverBottom + tileSize * 0.2f,
            tileSize * 0.2f, tileSize * 0.2f, bridgePaint
        )
        // Bridge planks
        val plankPaint = Paint().apply { color = Color.rgb(160, 110, 60); isAntiAlias = true }
        for (p in 0..3) {
            val py = riverTop + (riverBottom - riverTop) * (p / 4f) + tileSize * 0.05f
            canvas.drawRect(lbx + tileSize * 0.1f, py, lbx + bridgeWidth - tileSize * 0.1f, py + tileSize * 0.15f, plankPaint)
        }

        // Right bridge
        val rbx = gameToScreenX(GameConstants.BRIDGE_RIGHT_X) - bridgeWidth / 2
        canvas.drawRoundRect(
            rbx, riverTop - tileSize * 0.2f,
            rbx + bridgeWidth, riverBottom + tileSize * 0.2f,
            tileSize * 0.2f, tileSize * 0.2f, bridgePaint
        )
        for (p in 0..3) {
            val py = riverTop + (riverBottom - riverTop) * (p / 4f) + tileSize * 0.05f
            canvas.drawRect(rbx + tileSize * 0.1f, py, rbx + bridgeWidth - tileSize * 0.1f, py + tileSize * 0.15f, plankPaint)
        }

        // Arena boundary lines
        val linePaint = Paint().apply { color = Color.argb(80, 255, 255, 255); strokeWidth = 2f; style = Paint.Style.STROKE }
        canvas.drawRect(arenaRect, linePaint)

        // Center line
        canvas.drawLine(arenaRect.left, gameToScreenY(GameConstants.RIVER_Y), arenaRect.right, gameToScreenY(GameConstants.RIVER_Y), linePaint)
    }

    private fun drawDeployZone(canvas: Canvas) {
        if (isDragging) {
            val top = gameToScreenY(GameConstants.DEPLOY_ZONE_PLAYER_MIN_Y)
            val bottom = gameToScreenY(GameConstants.DEPLOY_ZONE_PLAYER_MAX_Y)
            canvas.drawRect(arenaRect.left, top, arenaRect.right, bottom, deployZonePaint)
        }
    }

    private fun drawTowers(canvas: Canvas) {
        for (tower in engine.allTowers) {
            if (tower.isDestroyed) {
                drawDestroyedTower(canvas, tower)
                continue
            }
            drawTower(canvas, tower)
        }
    }

    private fun drawTower(canvas: Canvas, tower: TowerEntity) {
        val sx = gameToScreenX(tower.x)
        val sy = gameToScreenY(tower.y)
        val size = if (tower.towerType == TowerType.KING) tileSize * 2f else tileSize * 1.5f
        val halfSize = size / 2f

        // Shadow
        canvas.drawOval(sx - halfSize, sy + halfSize * 0.3f, sx + halfSize, sy + halfSize * 0.7f, shadowPaint)

        // Tower base
        val isPlayer = tower.team == EntityTeam.PLAYER
        towerPaint.color = if (isPlayer) Color.rgb(50, 120, 220) else Color.rgb(220, 50, 50)

        // Draw tower body
        val bodyRect = RectF(sx - halfSize, sy - halfSize, sx + halfSize, sy + halfSize * 0.5f)
        canvas.drawRoundRect(bodyRect, tileSize * 0.15f, tileSize * 0.15f, towerPaint)
        canvas.drawRoundRect(bodyRect, tileSize * 0.15f, tileSize * 0.15f, towerOutlinePaint)

        // Tower top / crown
        if (tower.towerType == TowerType.KING) {
            val crownColor = if (isPlayer) Color.rgb(30, 90, 180) else Color.rgb(180, 30, 30)
            val topPaint = Paint().apply { color = crownColor; isAntiAlias = true }
            // Crown spikes
            val path = Path()
            path.moveTo(sx - halfSize * 0.7f, sy - halfSize * 0.5f)
            path.lineTo(sx - halfSize * 0.4f, sy - halfSize * 1.1f)
            path.lineTo(sx - halfSize * 0.1f, sy - halfSize * 0.7f)
            path.lineTo(sx + halfSize * 0.1f, sy - halfSize * 1.2f)
            path.lineTo(sx + halfSize * 0.4f, sy - halfSize * 0.7f)
            path.lineTo(sx + halfSize * 0.7f, sy - halfSize * 1.1f)
            path.lineTo(sx + halfSize * 0.7f, sy - halfSize * 0.5f)
            path.close()
            canvas.drawPath(path, topPaint)
            canvas.drawPath(path, towerOutlinePaint)
        } else {
            // Princess tower turret
            val turretPaint = Paint().apply { color = darkenColor(towerPaint.color, 0.85f); isAntiAlias = true }
            canvas.drawCircle(sx, sy - halfSize * 0.3f, halfSize * 0.4f, turretPaint)
            canvas.drawCircle(sx, sy - halfSize * 0.3f, halfSize * 0.4f, towerOutlinePaint)
        }

        // HP bar
        drawHpBar(canvas, sx, sy + halfSize * 0.6f, size, tower.hpPercent, isPlayer)

        // Active indicator for king tower
        if (tower.towerType == TowerType.KING && tower.isActive) {
            val activePaint = Paint().apply { color = Color.argb(60, 255, 255, 0); isAntiAlias = true }
            canvas.drawCircle(sx, sy, halfSize * 1.2f, activePaint)
        }
    }

    private fun drawDestroyedTower(canvas: Canvas, tower: TowerEntity) {
        val sx = gameToScreenX(tower.x)
        val sy = gameToScreenY(tower.y)
        val size = if (tower.towerType == TowerType.KING) tileSize * 2f else tileSize * 1.5f
        val halfSize = size / 2f

        val rubblePaint = Paint().apply { color = Color.rgb(100, 90, 80); isAntiAlias = true }
        canvas.drawOval(sx - halfSize, sy - halfSize * 0.3f, sx + halfSize, sy + halfSize * 0.3f, rubblePaint)

        // Smoke particles
        val smokePaint = Paint().apply { color = Color.argb(40, 100, 100, 100); isAntiAlias = true }
        val smokeOffset = sin(animTime * 2.0).toFloat() * tileSize * 0.2f
        canvas.drawCircle(sx - halfSize * 0.3f, sy - halfSize * 0.5f + smokeOffset, tileSize * 0.3f, smokePaint)
        canvas.drawCircle(sx + halfSize * 0.2f, sy - halfSize * 0.7f + smokeOffset * 0.7f, tileSize * 0.2f, smokePaint)
    }

    private fun drawEntities(canvas: Canvas) {
        val allEntities = (engine.playerEntities + engine.opponentEntities)
            .filter { it.isAlive || it.isDying }
            .sortedBy { it.y } // Draw from top to bottom for proper layering

        for (entity in allEntities) {
            drawEntity(canvas, entity)
        }
    }

    private fun drawEntity(canvas: Canvas, entity: GameEntity) {
        val sx = gameToScreenX(entity.x)
        val sy = gameToScreenY(entity.y)
        val radius = tileSize * getEntityRadius(entity)

        // Spawn animation
        val scale = if (entity.isSpawning) {
            val t = 1f - (entity.spawnAnimTimer / 0.3f)
            0.3f + t * 0.7f
        } else 1f

        val drawRadius = radius * scale

        // Death animation
        if (entity.isDying) {
            val alpha = (entity.deathTimer / 0.5f * 255).toInt().coerceIn(0, 255)
            entityPaint.alpha = alpha
        } else {
            entityPaint.alpha = 255
        }

        // Flying shadow
        if (entity.isFlying) {
            canvas.drawOval(
                sx - drawRadius * 0.7f, sy + drawRadius * 0.8f,
                sx + drawRadius * 0.7f, sy + drawRadius * 1.2f,
                shadowPaint
            )
        } else {
            // Ground shadow
            canvas.drawOval(
                sx - drawRadius, sy + drawRadius * 0.5f,
                sx + drawRadius, sy + drawRadius * 0.9f,
                shadowPaint
            )
        }

        val flyOffset = if (entity.isFlying) -tileSize * 0.5f else 0f
        val drawY = sy + flyOffset

        // Entity body color
        val baseColor = troopColors[entity.card.name] ?: Color.rgb(180, 180, 180)
        val isPlayer = entity.team == EntityTeam.PLAYER

        // Team tint
        entityPaint.color = if (isPlayer) {
            blendColors(baseColor, Color.rgb(50, 120, 220), 0.2f)
        } else {
            blendColors(baseColor, Color.rgb(220, 50, 50), 0.2f)
        }

        // Draw entity based on type
        if (entity.card.type == CardType.BUILDING) {
            drawBuildingEntity(canvas, entity, sx, drawY, drawRadius)
        } else {
            // Troop body
            canvas.drawCircle(sx, drawY, drawRadius, entityPaint)

            // Outline
            val outlinePaint = Paint().apply {
                isAntiAlias = true; style = Paint.Style.STROKE
                strokeWidth = 2f; color = if (isPlayer) Color.rgb(30, 80, 180) else Color.rgb(180, 30, 30)
                alpha = entityPaint.alpha
            }
            canvas.drawCircle(sx, drawY, drawRadius, outlinePaint)

            // Direction indicator (small triangle showing movement direction)
            if (entity.state == EntityState.MOVING && entity.targetEntity != null) {
                val target = entity.targetEntity!!
                val angle = atan2((target.y - entity.y).toDouble(), (target.x - entity.x).toDouble()).toFloat()
                val dirPaint = Paint().apply { color = Color.argb(150, 255, 255, 255); isAntiAlias = true }
                val path = Path()
                val tipDist = drawRadius * 0.8f
                path.moveTo(sx + cos(angle) * tipDist, drawY + sin(angle) * tipDist)
                path.lineTo(
                    sx + cos(angle + 2.5f) * tipDist * 0.5f,
                    drawY + sin(angle + 2.5f) * tipDist * 0.5f
                )
                path.lineTo(
                    sx + cos(angle - 2.5f) * tipDist * 0.5f,
                    drawY + sin(angle - 2.5f) * tipDist * 0.5f
                )
                path.close()
                canvas.drawPath(path, dirPaint)
            }

            // Attack flash
            if (entity.state == EntityState.ATTACKING && entity.attackCooldown > entity.effectiveAttackSpeed * 0.8f) {
                val flashPaint = Paint().apply { color = Color.argb(100, 255, 255, 200); isAntiAlias = true }
                canvas.drawCircle(sx, drawY, drawRadius * 1.3f, flashPaint)
            }

            // Charge glow
            if (entity.isCharging) {
                val chargePaint = Paint().apply { color = Color.argb(80, 255, 200, 0); isAntiAlias = true }
                canvas.drawCircle(sx, drawY, drawRadius * 1.5f, chargePaint)
            }
        }

        // Frozen overlay
        if (entity.isFrozen) {
            val frozenPaint = Paint().apply { color = Color.argb(120, 100, 200, 255); isAntiAlias = true }
            canvas.drawCircle(sx, drawY, drawRadius * 1.1f, frozenPaint)
        }

        // Rage glow
        if (entity.isRaged) {
            val ragePaint = Paint().apply { color = Color.argb(60, 255, 50, 255); isAntiAlias = true }
            canvas.drawCircle(sx, drawY, drawRadius * 1.3f + sin(animTime * 10f) * tileSize * 0.05f, ragePaint)
        }

        // HP bar (only if damaged)
        if (entity.hpPercent < 1f && entity.isAlive) {
            drawHpBar(canvas, sx, drawY - drawRadius - tileSize * 0.15f, drawRadius * 2f, entity.hpPercent, isPlayer)
        }

        // Name label for larger units
        if (entity.card.spawnCount == 1 && !entity.isDying) {
            textPaint.textSize = tileSize * 0.3f
            textPaint.color = Color.WHITE
            textPaint.alpha = entityPaint.alpha
            textPaint.setShadowLayer(2f, 0f, 1f, Color.BLACK)
            canvas.drawText(entity.card.iconEmoji, sx, drawY + tileSize * 0.12f, textPaint)
            textPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        }

        entityPaint.alpha = 255
    }

    private fun drawBuildingEntity(canvas: Canvas, entity: GameEntity, sx: Float, sy: Float, size: Float) {
        val rect = RectF(sx - size, sy - size, sx + size, sy + size * 0.5f)
        canvas.drawRoundRect(rect, tileSize * 0.1f, tileSize * 0.1f, entityPaint)

        val outlinePaint = Paint().apply {
            isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f
            color = if (entity.team == EntityTeam.PLAYER) Color.rgb(30, 80, 180) else Color.rgb(180, 30, 30)
        }
        canvas.drawRoundRect(rect, tileSize * 0.1f, tileSize * 0.1f, outlinePaint)

        // Lifetime bar
        if (entity.card.lifetime > 0 && entity.stateTimer < entity.card.lifetime) {
            val lifePct = 1f - (entity.stateTimer / entity.card.lifetime)
            val barWidth = size * 2f
            val barLeft = sx - barWidth / 2
            val barTop = sy + size * 0.6f
            val lifePaint = Paint().apply { color = Color.rgb(255, 193, 7); isAntiAlias = true }
            canvas.drawRect(barLeft, barTop, barLeft + barWidth, barTop + tileSize * 0.1f, hpBarBgPaint)
            canvas.drawRect(barLeft, barTop, barLeft + barWidth * lifePct, barTop + tileSize * 0.1f, lifePaint)
        }
    }

    private fun getEntityRadius(entity: GameEntity): Float {
        return when {
            entity.card.type == CardType.BUILDING -> 0.55f
            entity.card.hitpoints > 3000 -> 0.5f
            entity.card.hitpoints > 1500 -> 0.4f
            entity.card.spawnCount > 3 -> 0.2f
            entity.card.spawnCount > 1 -> 0.25f
            else -> 0.35f
        }
    }

    private fun drawHpBar(canvas: Canvas, cx: Float, cy: Float, width: Float, percent: Float, isPlayer: Boolean) {
        val barHeight = tileSize * 0.12f
        val barLeft = cx - width / 2f
        canvas.drawRoundRect(barLeft, cy, barLeft + width, cy + barHeight, barHeight / 2, barHeight / 2, hpBarBgPaint)
        val fillPaint = if (isPlayer) hpBarPlayerPaint else hpBarEnemyPaint
        val fillWidth = width * percent
        canvas.drawRoundRect(barLeft, cy, barLeft + fillWidth, cy + barHeight, barHeight / 2, barHeight / 2, fillPaint)
    }

    private fun drawProjectiles(canvas: Canvas) {
        for (proj in engine.projectiles) {
            if (!proj.isActive) continue
            val sx = gameToScreenX(proj.projX)
            val sy = gameToScreenY(proj.projY)
            val size = tileSize * 0.15f

            // Glow
            val glowPaint = Paint().apply { color = Color.argb(100, 255, 255, 100); isAntiAlias = true }
            canvas.drawCircle(sx, sy, size * 2f, glowPaint)

            // Projectile
            projectilePaint.color = if (proj.source.team == EntityTeam.PLAYER) Color.rgb(100, 180, 255) else Color.rgb(255, 100, 100)
            canvas.drawCircle(sx, sy, size, projectilePaint)
        }
    }

    private fun drawSpellEffects(canvas: Canvas) {
        for (effect in engine.spellEffects) {
            if (!effect.isActive) continue
            val sx = gameToScreenX(effect.centerX)
            val sy = gameToScreenY(effect.centerY)
            val radius = effect.radius * tileSize

            val progress = 1f - (effect.timer / effect.totalDuration)
            val alpha = ((1f - progress) * 150).toInt().coerceIn(0, 150)

            val color = when (effect.card.name) {
                "Fireball" -> Color.argb(alpha, 255, 120, 0)
                "Arrows" -> Color.argb(alpha, 200, 200, 200)
                "Zap" -> Color.argb(alpha, 255, 255, 100)
                "Lightning" -> Color.argb(alpha, 255, 255, 200)
                "Rocket" -> Color.argb(alpha, 255, 80, 0)
                "Freeze" -> Color.argb(alpha, 100, 200, 255)
                "Poison" -> Color.argb(alpha, 100, 0, 200)
                "Rage" -> Color.argb(alpha, 255, 0, 255)
                "Log" -> Color.argb(alpha, 139, 90, 43)
                "Tornado" -> Color.argb(alpha, 150, 150, 150)
                else -> Color.argb(alpha, 255, 255, 0)
            }

            spellPaint.color = color
            canvas.drawCircle(sx, sy, radius, spellPaint)

            // Animated ring
            val ringPaint = Paint().apply {
                this.color = Color.argb((alpha * 0.7f).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 3f
            }
            canvas.drawCircle(sx, sy, radius * (0.5f + progress * 0.5f), ringPaint)
        }
    }

    private fun drawDragPreview(canvas: Canvas) {
        if (!isDragging || dragCardIndex < 0) return
        if (!::engine.isInitialized) return
        if (dragCardIndex >= engine.playerHand.size) return

        val card = engine.playerHand[dragCardIndex]
        val sx = gameToScreenX(dragGameX)
        val sy = gameToScreenY(dragGameY)

        // Placement validity indicator
        val valid = dragGameY >= GameConstants.DEPLOY_ZONE_PLAYER_MIN_Y && dragGameY <= GameConstants.DEPLOY_ZONE_PLAYER_MAX_Y
                && dragGameX > 0.5f && dragGameX < GameConstants.ARENA_WIDTH - 0.5f
        isValidPlacement = valid

        val indicatorPaint = Paint().apply {
            color = if (valid) Color.argb(80, 76, 175, 80) else Color.argb(80, 244, 67, 54)
            isAntiAlias = true
        }

        val previewRadius = if (card.type == CardType.SPELL) card.spellRadius * tileSize else tileSize * 1.5f
        canvas.drawCircle(sx, sy, previewRadius, indicatorPaint)

        // Card name
        textPaint.textSize = tileSize * 0.4f
        textPaint.color = Color.WHITE
        textPaint.setShadowLayer(4f, 0f, 2f, Color.BLACK)
        canvas.drawText(card.name, sx, sy - previewRadius - tileSize * 0.3f, textPaint)
        textPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }

    private fun drawTimer(canvas: Canvas) {
        val state = engine.gameState
        val minutes = (state.timeRemaining / 60).toInt()
        val seconds = (state.timeRemaining % 60).toInt()
        val timeStr = String.format("%d:%02d", minutes, seconds)

        timerPaint.textSize = tileSize * 0.7f

        // Phase indicator color
        timerPaint.color = when (state.phase) {
            GamePhase.DOUBLE_ELIXIR -> Color.rgb(255, 193, 7)
            GamePhase.OVERTIME -> Color.rgb(244, 67, 54)
            else -> Color.WHITE
        }

        val timerY = offsetY - tileSize * 0.3f
        canvas.drawText(timeStr, width / 2f, timerY.coerceAtLeast(tileSize * 0.8f), timerPaint)

        // Phase label
        val phaseStr = when (state.phase) {
            GamePhase.DOUBLE_ELIXIR -> "2x ELIXIR"
            GamePhase.OVERTIME -> "OVERTIME"
            else -> ""
        }
        if (phaseStr.isNotEmpty()) {
            timerPaint.textSize = tileSize * 0.35f
            canvas.drawText(phaseStr, width / 2f, timerY.coerceAtLeast(tileSize * 0.8f) + tileSize * 0.5f, timerPaint)
        }
    }

    private fun drawCrowns(canvas: Canvas) {
        val state = engine.gameState
        crownPaint.textSize = tileSize * 0.6f

        // Opponent crowns (top)
        val crownY = offsetY - tileSize * 0.2f
        for (i in 0 until 3) {
            val cx = width / 2f + (i - 1) * tileSize * 0.8f
            crownPaint.color = if (i < state.opponentCrowns) Color.rgb(255, 215, 0) else Color.rgb(80, 80, 80)
            canvas.drawText("★", cx - tileSize * 3.5f, crownY.coerceAtLeast(tileSize * 0.6f), crownPaint)
        }

        // Player crowns
        for (i in 0 until 3) {
            val cx = width / 2f + (i - 1) * tileSize * 0.8f
            crownPaint.color = if (i < state.playerCrowns) Color.rgb(255, 215, 0) else Color.rgb(80, 80, 80)
            canvas.drawText("★", cx + tileSize * 2f, crownY.coerceAtLeast(tileSize * 0.6f), crownPaint)
        }
    }

    private fun drawCountdown(canvas: Canvas) {
        if (engine.gameState.phase != GamePhase.COUNTDOWN) return

        val count = engine.getCountdownTime().toInt() + 1
        countdownPaint.textSize = tileSize * 3f
        countdownPaint.color = Color.WHITE

        // Background overlay
        val overlayPaint = Paint().apply { color = Color.argb(120, 0, 0, 0) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        canvas.drawText(count.toString(), width / 2f, height / 2f + tileSize, countdownPaint)
    }

    // --- Touch handling ---

    fun startCardDrag(cardIndex: Int) {
        isDragging = true
        dragCardIndex = cardIndex
        onCardDragStart?.invoke(cardIndex)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dragX = event.x
                    dragY = event.y
                    dragGameX = screenToGameX(event.x)
                    dragGameY = screenToGameY(event.y)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging && isValidPlacement) {
                    if (::engine.isInitialized && engine.playCard(dragCardIndex, dragGameX, dragGameY)) {
                        onCardPlayed?.invoke(dragCardIndex)
                    }
                }
                isDragging = false
                dragCardIndex = -1
                onCardDragEnd?.invoke()
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                dragCardIndex = -1
                onCardDragEnd?.invoke()
            }
        }
        return true
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val r = ((Color.red(color1) * (1 - ratio)) + (Color.red(color2) * ratio)).toInt()
        val g = ((Color.green(color1) * (1 - ratio)) + (Color.green(color2) * ratio)).toInt()
        val b = ((Color.blue(color1) * (1 - ratio)) + (Color.blue(color2) * ratio)).toInt()
        return Color.rgb(r, g, b)
    }

    fun pause() {
        isRunning = false
        try {
            gameThread?.join(1000)
        } catch (_: InterruptedException) {}
    }

    fun resume() {
        if (!isRunning) {
            isRunning = true
            gameThread = Thread(this)
            gameThread?.start()
        }
    }
}
