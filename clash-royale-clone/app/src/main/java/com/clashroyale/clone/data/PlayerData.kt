package com.clashroyale.clone.data

import android.content.Context
import android.content.SharedPreferences
import com.clashroyale.clone.models.Player
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PlayerData {
    private const val PREFS_NAME = "clash_royale_clone_prefs"
    private const val KEY_PLAYER = "player_data"
    private val gson = Gson()

    private var cachedPlayer: Player? = null

    fun getPlayer(context: Context): Player {
        cachedPlayer?.let { return it }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PLAYER, null)

        val player = if (json != null) {
            try {
                gson.fromJson(json, Player::class.java)
            } catch (e: Exception) {
                createNewPlayer()
            }
        } else {
            createNewPlayer()
        }

        cachedPlayer = player
        return player
    }

    fun savePlayer(context: Context, player: Player) {
        cachedPlayer = player
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PLAYER, gson.toJson(player)).apply()
    }

    private fun createNewPlayer(): Player {
        val starterCards = CardDatabase.getStarterCards()
        val cardLevels = mutableMapOf<Int, Int>()
        starterCards.forEach { cardLevels[it] = 1 }

        return Player(
            name = "Player",
            trophies = 0,
            deck = CardDatabase.getDefaultDeck().toMutableList(),
            cardCollection = cardLevels,
            gold = 1000,
            gems = 100
        )
    }

    fun resetPlayer(context: Context) {
        cachedPlayer = null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
