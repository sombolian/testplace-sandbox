package com.clashroyale.clone.ui.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.clashroyale.clone.R
import com.clashroyale.clone.data.ArenaDatabase
import com.clashroyale.clone.data.CardDatabase
import com.clashroyale.clone.data.PlayerData
import com.clashroyale.clone.models.Player

class HomeActivity : AppCompatActivity() {

    private lateinit var player: Player
    private lateinit var trophyText: TextView
    private lateinit var arenaText: TextView
    private lateinit var playerNameText: TextView
    private lateinit var battleButton: View
    private lateinit var deckContainer: LinearLayout
    private lateinit var statsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        player = PlayerData.getPlayer(this)

        trophyText = findViewById(R.id.trophyText)
        arenaText = findViewById(R.id.arenaText)
        playerNameText = findViewById(R.id.playerNameText)
        battleButton = findViewById(R.id.battleButton)
        deckContainer = findViewById(R.id.deckContainer)
        statsText = findViewById(R.id.statsText)

        updateUI()
        setupBattleButton()
        setupDeckDisplay()
        setupBottomNav()

        // Animate cards in
        animateCardsIn()
    }

    override fun onResume() {
        super.onResume()
        player = PlayerData.getPlayer(this)
        updateUI()
        setupDeckDisplay()
    }

    private fun updateUI() {
        val arena = ArenaDatabase.getArenaForTrophies(player.trophies)

        playerNameText.text = player.name
        trophyText.text = "🏆 ${player.trophies}"
        arenaText.text = arena.name

        val nextArena = ArenaDatabase.getNextArena(arena)
        val progressText = if (nextArena != null) {
            "${player.trophies}/${nextArena.trophyRequired}"
        } else {
            "${player.trophies} (Max Arena!)"
        }

        statsText.text = "W: ${player.wins} | L: ${player.losses} | Streak: ${player.winStreak}\n$progressText"

        // Arena-themed gradient background
        val root = findViewById<View>(R.id.homeRoot)
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(arena.primaryColor.toInt(), arena.secondaryColor.toInt(), Color.rgb(20, 20, 30))
        )
        root.background = gradient
    }

    private fun setupBattleButton() {
        battleButton.setOnClickListener {
            // Scale animation
            it.animate()
                .scaleX(0.9f).scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            val intent = Intent(this, BattleActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                        .start()
                }
                .start()
        }
    }

    private fun setupDeckDisplay() {
        deckContainer.removeAllViews()

        // Top row (4 cards)
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        // Bottom row (4 cards)
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        for ((index, cardId) in player.deck.withIndex()) {
            val card = CardDatabase.getCardById(cardId) ?: continue
            val cardView = createMiniCardView(card, index)
            if (index < 4) topRow.addView(cardView)
            else bottomRow.addView(cardView)
        }

        deckContainer.addView(topRow)
        deckContainer.addView(bottomRow)
    }

    private fun createMiniCardView(card: com.clashroyale.clone.models.Card, index: Int): View {
        val cardLayout = FrameLayout(this).apply {
            val size = (resources.displayMetrics.widthPixels / 5.5f).toInt()
            layoutParams = LinearLayout.LayoutParams(size, (size * 1.3f).toInt()).apply {
                marginStart = 4; marginEnd = 4
            }
        }

        val cardBg = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val grad = GradientDrawable()
            grad.cornerRadius = 12f
            grad.setColor(card.rarity.color.toInt())
            grad.setStroke(2, Color.argb(100, 255, 255, 255))
            background = grad
        }

        val nameText = TextView(this).apply {
            text = "${card.iconEmoji}\n${card.name}"
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
        }

        val elixirBadge = TextView(this).apply {
            text = "${card.elixirCost}"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val badgeSize = 28
            layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                gravity = Gravity.TOP or Gravity.START
                marginStart = 4; topMargin = 4
            }
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(Color.rgb(156, 39, 176))
            background = bg
            paint.isFakeBoldText = true
        }

        cardLayout.addView(cardBg)
        cardLayout.addView(nameText)
        cardLayout.addView(elixirBadge)

        cardLayout.setOnClickListener {
            val intent = Intent(this, DeckBuilderActivity::class.java)
            startActivity(intent)
        }

        return cardLayout
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.navBattle)?.setOnClickListener {
            startActivity(Intent(this, BattleActivity::class.java))
        }
        findViewById<View>(R.id.navDeck)?.setOnClickListener {
            startActivity(Intent(this, DeckBuilderActivity::class.java))
        }
    }

    private fun animateCardsIn() {
        deckContainer.post {
            for (i in 0 until deckContainer.childCount) {
                val row = deckContainer.getChildAt(i) as? ViewGroup ?: continue
                for (j in 0 until row.childCount) {
                    val card = row.getChildAt(j)
                    card.alpha = 0f
                    card.translationY = 100f
                    card.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setStartDelay((i * 4 + j) * 80L)
                        .setInterpolator(OvershootInterpolator(1.2f))
                        .start()
                }
            }
        }
    }
}
