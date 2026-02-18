package com.clashroyale.clone.ui.activities

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.clashroyale.clone.R
import com.clashroyale.clone.data.ArenaDatabase
import com.clashroyale.clone.data.CardDatabase
import com.clashroyale.clone.data.PlayerData
import com.clashroyale.clone.game.*
import com.clashroyale.clone.models.*
import com.clashroyale.clone.utils.GameConstants

class BattleActivity : AppCompatActivity() {

    private lateinit var battleView: BattleView
    private lateinit var engine: BattleEngine
    private lateinit var aiOpponent: AIOpponent

    private lateinit var elixirBar: ProgressBar
    private lateinit var elixirText: TextView
    private lateinit var cardHandContainer: LinearLayout
    private lateinit var nextCardView: FrameLayout

    private val player by lazy { PlayerData.getPlayer(this) }
    private var cardViews = mutableListOf<View>()
    private var selectedCardIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        battleView = findViewById(R.id.battleView)
        elixirBar = findViewById(R.id.elixirBar)
        elixirText = findViewById(R.id.elixirText)
        cardHandContainer = findViewById(R.id.cardHandContainer)
        nextCardView = findViewById(R.id.nextCardView)

        setupGame()
        setupCardHand()
        setupElixirUpdater()
    }

    private fun setupGame() {
        val playerCards = player.deck.mapNotNull { CardDatabase.getCardById(it) }

        // Create opponent deck based on player's arena
        val opponentCardIds = generateOpponentDeck()
        val opponentCards = opponentCardIds.mapNotNull { CardDatabase.getCardById(it) }

        engine = BattleEngine(playerCards, opponentCards)
        engine.initialize()

        // Set AI difficulty based on trophies
        val difficulty = when {
            player.trophies < 500 -> AIDifficulty.EASY
            player.trophies < 2000 -> AIDifficulty.MEDIUM
            player.trophies < 4000 -> AIDifficulty.HARD
            else -> AIDifficulty.EXPERT
        }
        aiOpponent = AIOpponent(engine, difficulty)

        battleView.engine = engine
        battleView.aiOpponent = aiOpponent

        val arena = ArenaDatabase.getArenaForTrophies(player.trophies)
        battleView.setArenaTheme(arena.id)

        // Game callbacks
        engine.onGameOver = { result ->
            runOnUiThread { showGameOverDialog(result) }
        }

        engine.onTowerDestroyed = { tower ->
            runOnUiThread {
                // Flash screen effect
                val flashView = findViewById<View>(R.id.flashOverlay)
                flashView?.visibility = View.VISIBLE
                flashView?.alpha = 0.5f
                flashView?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
                    flashView.visibility = View.GONE
                }?.start()
            }
        }

        // Card drag callbacks
        battleView.onCardPlayed = { index ->
            runOnUiThread {
                updateCardHand()
            }
        }
    }

    private fun generateOpponentDeck(): List<Int> {
        val arena = ArenaDatabase.getArenaForTrophies(player.trophies)
        val availableCards = mutableListOf<Int>()

        // Collect all cards unlocked up to current arena
        for (a in ArenaDatabase.arenas) {
            if (a.trophyRequired <= player.trophies + 400) {
                availableCards.addAll(a.unlockedCardIds)
            }
        }

        if (availableCards.size < 8) {
            availableCards.addAll(CardDatabase.getStarterCards())
        }

        // Build a somewhat balanced deck
        val deck = mutableListOf<Int>()
        val shuffled = availableCards.distinct().shuffled()

        // Ensure at least one win condition (building targeter)
        val winCons = shuffled.filter { id ->
            val card = CardDatabase.getCardById(id)
            card != null && card.targetType == TargetType.BUILDINGS && card.type == CardType.TROOP
        }
        if (winCons.isNotEmpty()) deck.add(winCons.first())

        // Ensure at least one spell
        val spells = shuffled.filter { id ->
            val card = CardDatabase.getCardById(id)
            card != null && card.type == CardType.SPELL && !deck.contains(id)
        }
        if (spells.isNotEmpty()) deck.add(spells.first())

        // Fill rest
        for (id in shuffled) {
            if (deck.size >= 8) break
            if (!deck.contains(id)) deck.add(id)
        }

        // Pad with defaults if needed
        while (deck.size < 8) {
            val defaults = CardDatabase.getDefaultDeck()
            for (id in defaults) {
                if (!deck.contains(id) && deck.size < 8) deck.add(id)
            }
        }

        return deck.take(8)
    }

    private fun setupCardHand() {
        cardHandContainer.removeAllViews()
        cardViews.clear()

        for (i in 0 until 4) {
            if (i >= engine.playerHand.size) break
            val card = engine.playerHand[i]
            val cardView = createCardHandView(card, i)
            cardViews.add(cardView)
            cardHandContainer.addView(cardView)
        }

        updateNextCard()
    }

    private fun createCardHandView(card: Card, index: Int): View {
        val cardWidth = resources.displayMetrics.widthPixels / 5
        val cardHeight = (cardWidth * 1.4f).toInt()

        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(cardWidth, cardHeight).apply {
                marginStart = 4; marginEnd = 4
            }
        }

        val bg = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val grad = GradientDrawable()
            grad.cornerRadius = 16f
            grad.setColor(card.rarity.color.toInt())
            grad.setStroke(3, Color.argb(180, 255, 255, 255))
            background = grad
            elevation = 8f
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(4, 4, 4, 4)
        }

        val emojiText = TextView(this).apply {
            text = card.iconEmoji
            textSize = 22f
            gravity = Gravity.CENTER
        }

        val nameText = TextView(this).apply {
            text = card.name
            textSize = 9f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFakeBoldText = true
            maxLines = 1
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
        }

        content.addView(emojiText)
        content.addView(nameText)

        // Elixir cost badge
        val elixirBadge = TextView(this).apply {
            text = "${card.elixirCost}"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFakeBoldText = true
            val badgeSize = 32
            layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                gravity = Gravity.TOP or Gravity.START
                marginStart = 4; topMargin = 4
            }
            val badge = GradientDrawable()
            badge.shape = GradientDrawable.OVAL
            badge.setColor(Color.rgb(156, 39, 176))
            badge.setStroke(2, Color.WHITE)
            background = badge
            elevation = 12f
        }

        frame.addView(bg)
        frame.addView(content)
        frame.addView(elixirBadge)

        // Touch listener for drag to play
        frame.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val canAfford = engine.gameState.canPlayerAfford(card.elixirCost)
                    if (canAfford && engine.gameState.phase != GamePhase.COUNTDOWN && engine.gameState.phase != GamePhase.ENDED) {
                        selectedCardIndex = index
                        v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                        battleView.startCardDrag(index)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (selectedCardIndex == index) {
                        // Forward touch to battle view
                        battleView.onTouchEvent(event)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (selectedCardIndex == index) {
                        battleView.onTouchEvent(event)
                        selectedCardIndex = -1
                    }
                    true
                }
                else -> false
            }
        }

        return frame
    }

    private fun updateCardHand() {
        cardHandContainer.removeAllViews()
        cardViews.clear()

        for (i in 0 until engine.playerHand.size.coerceAtMost(4)) {
            val card = engine.playerHand[i]
            val cardView = createCardHandView(card, i)

            // Animate new card in
            cardView.alpha = 0f
            cardView.translationY = 50f
            cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator())
                .start()

            cardViews.add(cardView)
            cardHandContainer.addView(cardView)
        }

        updateNextCard()
    }

    private fun updateNextCard() {
        nextCardView.removeAllViews()
        val nextCard = engine.playerNextCard ?: return

        val text = TextView(this).apply {
            this.text = "${nextCard.iconEmoji}\n${nextCard.name}"
            textSize = 9f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
        }
        nextCardView.addView(text)
    }

    private fun setupElixirUpdater() {
        val handler = android.os.Handler(mainLooper)
        val updateRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && !isDestroyed) {
                    val elixir = engine.gameState.playerElixir.toInt()
                    elixirBar.progress = (engine.gameState.playerElixir * 10).toInt()
                    elixirText.text = "$elixir/10"

                    // Update card affordability visuals
                    for (i in cardViews.indices) {
                        if (i < engine.playerHand.size) {
                            val canAfford = engine.gameState.canPlayerAfford(engine.playerHand[i].elixirCost)
                            cardViews[i].alpha = if (canAfford) 1f else 0.5f
                        }
                    }

                    handler.postDelayed(this, 50)
                }
            }
        }
        handler.post(updateRunnable)
    }

    private fun showGameOverDialog(result: BattleResult) {
        // Update player data
        if (result.isWin) {
            player.wins++
            player.winStreak++
        } else {
            player.losses++
            player.winStreak = 0
        }
        player.addTrophies(result.trophyChange)
        PlayerData.savePlayer(this, player)

        val title = when {
            result.playerCrowns >= 3 -> "THREE CROWN VICTORY!"
            result.isWin -> "VICTORY!"
            result.playerCrowns == result.opponentCrowns -> "DRAW"
            else -> "DEFEAT"
        }

        val titleColor = when {
            result.isWin -> Color.rgb(76, 175, 80)
            result.playerCrowns == result.opponentCrowns -> Color.rgb(255, 193, 7)
            else -> Color.rgb(244, 67, 54)
        }

        val message = buildString {
            append("${result.playerCrowns} ★ - ★ ${result.opponentCrowns}\n\n")
            if (result.trophyChange > 0) append("🏆 +${result.trophyChange} Trophies\n")
            else if (result.trophyChange < 0) append("🏆 ${result.trophyChange} Trophies\n")
            append("\nTotal: ${player.trophies} 🏆")
        }

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Battle Again") { _, _ ->
                recreate()
            }
            .setNegativeButton("Home") { _, _ ->
                finish()
            }
            .create()

        dialog.show()

        // Style the dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.rgb(76, 175, 80))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.rgb(200, 200, 200))
    }

    override fun onPause() {
        super.onPause()
        battleView.pause()
    }

    override fun onResume() {
        super.onResume()
        battleView.resume()
    }

    override fun onBackPressed() {
        if (engine.gameState.phase != GamePhase.ENDED) {
            AlertDialog.Builder(this)
                .setTitle("Quit Battle?")
                .setMessage("You will lose trophies if you quit!")
                .setPositiveButton("Quit") { _, _ ->
                    player.losses++
                    player.addTrophies(GameConstants.LOSS_TROPHIES)
                    PlayerData.savePlayer(this, player)
                    finish()
                }
                .setNegativeButton("Continue", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
