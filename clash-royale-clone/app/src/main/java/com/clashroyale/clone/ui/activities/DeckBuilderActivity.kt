package com.clashroyale.clone.ui.activities

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.clashroyale.clone.R
import com.clashroyale.clone.data.CardDatabase
import com.clashroyale.clone.data.PlayerData
import com.clashroyale.clone.models.*

class DeckBuilderActivity : AppCompatActivity() {

    private lateinit var player: com.clashroyale.clone.models.Player
    private lateinit var deckGrid: GridLayout
    private lateinit var collectionGrid: GridLayout
    private lateinit var avgElixirText: TextView
    private lateinit var deckTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_builder)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        player = PlayerData.getPlayer(this)

        deckGrid = findViewById(R.id.deckGrid)
        collectionGrid = findViewById(R.id.collectionGrid)
        avgElixirText = findViewById(R.id.avgElixirText)
        deckTitle = findViewById(R.id.deckTitle)

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        refreshDisplay()
    }

    private fun refreshDisplay() {
        updateDeckDisplay()
        updateCollectionDisplay()
        updateAvgElixir()
    }

    private fun updateDeckDisplay() {
        deckGrid.removeAllViews()
        deckGrid.columnCount = 4

        for ((index, cardId) in player.deck.withIndex()) {
            val card = CardDatabase.getCardById(cardId) ?: continue
            val view = createDeckCardView(card, index)
            deckGrid.addView(view)
        }
    }

    private fun updateCollectionDisplay() {
        collectionGrid.removeAllViews()
        collectionGrid.columnCount = 4

        val ownedCards = player.cardCollection.keys
            .mapNotNull { CardDatabase.getCardById(it) }
            .filter { !player.deck.contains(it.id) }
            .sortedWith(compareBy({ it.rarity.ordinal }, { it.elixirCost }, { it.name }))

        for (card in ownedCards) {
            val view = createCollectionCardView(card)
            collectionGrid.addView(view)
        }
    }

    private fun createDeckCardView(card: Card, deckIndex: Int): View {
        val cardWidth = (resources.displayMetrics.widthPixels - 80) / 4
        val cardHeight = (cardWidth * 1.4f).toInt()

        val frame = FrameLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cardWidth; height = cardHeight
                setMargins(4, 4, 4, 4)
            }
        }

        val bg = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val grad = GradientDrawable()
            grad.cornerRadius = 14f
            grad.setColor(card.rarity.color.toInt())
            grad.setStroke(3, Color.rgb(255, 215, 0))
            background = grad
            elevation = 6f
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(4, 8, 4, 4)
        }

        content.addView(TextView(this).apply {
            text = card.iconEmoji
            textSize = 20f
            gravity = Gravity.CENTER
        })

        content.addView(TextView(this).apply {
            text = card.name
            textSize = 9f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFakeBoldText = true
            maxLines = 2
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
        })

        // Elixir badge
        val elixirBadge = createElixirBadge(card.elixirCost)

        // Remove button
        val removeBtn = TextView(this).apply {
            text = "✕"
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val btnSize = 24
            layoutParams = FrameLayout.LayoutParams(btnSize, btnSize).apply {
                gravity = Gravity.TOP or Gravity.END
                marginEnd = 2; topMargin = 2
            }
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(Color.rgb(244, 67, 54))
            background = bg
        }

        frame.addView(bg)
        frame.addView(content)
        frame.addView(elixirBadge)
        frame.addView(removeBtn)

        frame.setOnClickListener {
            // Remove from deck - swap with first available collection card
            val ownedNotInDeck = player.cardCollection.keys
                .filter { !player.deck.contains(it) }
                .firstOrNull()

            if (ownedNotInDeck != null) {
                player.deck[deckIndex] = ownedNotInDeck
                PlayerData.savePlayer(this, player)
                refreshDisplay()
            }
        }

        return frame
    }

    private fun createCollectionCardView(card: Card): View {
        val cardWidth = (resources.displayMetrics.widthPixels - 80) / 4
        val cardHeight = (cardWidth * 1.4f).toInt()

        val frame = FrameLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cardWidth; height = cardHeight
                setMargins(4, 4, 4, 4)
            }
        }

        val bg = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val grad = GradientDrawable()
            grad.cornerRadius = 14f
            grad.setColor(card.rarity.color.toInt())
            grad.setStroke(2, Color.argb(100, 255, 255, 255))
            background = grad
            elevation = 4f
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(4, 8, 4, 4)
        }

        content.addView(TextView(this).apply {
            text = card.iconEmoji
            textSize = 20f
            gravity = Gravity.CENTER
        })

        content.addView(TextView(this).apply {
            text = card.name
            textSize = 9f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFakeBoldText = true
            maxLines = 2
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
        })

        val elixirBadge = createElixirBadge(card.elixirCost)

        frame.addView(bg)
        frame.addView(content)
        frame.addView(elixirBadge)

        frame.setOnClickListener {
            // Add to deck - find first slot to replace (or swap)
            if (player.deck.size < 8) {
                player.deck.add(card.id)
            } else {
                // Show card info / swap
                showCardSwapDialog(card)
            }
            PlayerData.savePlayer(this, player)
            refreshDisplay()
        }

        return frame
    }

    private fun createElixirBadge(cost: Int): TextView {
        return TextView(this).apply {
            text = "$cost"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFakeBoldText = true
            val badgeSize = 28
            layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                gravity = Gravity.TOP or Gravity.START
                marginStart = 4; topMargin = 4
            }
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(Color.rgb(156, 39, 176))
            bg.setStroke(1, Color.WHITE)
            background = bg
        }
    }

    private fun showCardSwapDialog(newCard: Card) {
        val items = player.deck.mapNotNull { id ->
            CardDatabase.getCardById(id)?.let { "${it.iconEmoji} ${it.name} (${it.elixirCost})" }
        }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Swap ${newCard.name} with:")
            .setItems(items) { _, which ->
                val oldCardId = player.deck[which]
                player.deck[which] = newCard.id
                PlayerData.savePlayer(this, player)
                refreshDisplay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAvgElixir() {
        val avg = player.deck.mapNotNull { CardDatabase.getCardById(it) }
            .map { it.elixirCost }
            .average()
        avgElixirText.text = String.format("Average Elixir: %.1f", avg)
    }
}
