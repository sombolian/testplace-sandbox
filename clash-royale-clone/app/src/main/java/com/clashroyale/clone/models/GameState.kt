package com.clashroyale.clone.models

data class BattleResult(
    val playerCrowns: Int,
    val opponentCrowns: Int,
    val isWin: Boolean,
    val trophyChange: Int,
    val battleDurationSeconds: Int
)

enum class GamePhase {
    COUNTDOWN,
    REGULAR_TIME,
    DOUBLE_ELIXIR,
    OVERTIME,
    SUDDEN_DEATH,
    ENDED
}

data class GameState(
    var phase: GamePhase = GamePhase.COUNTDOWN,
    var timeRemaining: Float = 180f, // 3 minutes
    var overtimeRemaining: Float = 60f,
    var playerElixir: Float = 5f,
    var opponentElixir: Float = 5f,
    var elixirRate: Float = 1f, // elixir per second (2x in double elixir)
    var playerCrowns: Int = 0,
    var opponentCrowns: Int = 0,
    var playerKingActivated: Boolean = false,
    var opponentKingActivated: Boolean = false,
    var isPaused: Boolean = false
) {
    val isGameOver: Boolean get() = phase == GamePhase.ENDED ||
            playerCrowns >= 3 || opponentCrowns >= 3

    val maxElixir: Float get() = 10f

    fun addPlayerElixir(amount: Float) {
        playerElixir = (playerElixir + amount).coerceIn(0f, maxElixir)
    }

    fun addOpponentElixir(amount: Float) {
        opponentElixir = (opponentElixir + amount).coerceIn(0f, maxElixir)
    }

    fun canPlayerAfford(cost: Int): Boolean = playerElixir >= cost
    fun canOpponentAfford(cost: Int): Boolean = opponentElixir >= cost
}
