package com.clashroyale.clone.utils

object GameConstants {
    // Arena dimensions (in game tiles)
    const val ARENA_WIDTH = 18f
    const val ARENA_HEIGHT = 32f

    // Tower positions (in tiles from top-left)
    const val PLAYER_KING_X = 9f
    const val PLAYER_KING_Y = 28f
    const val PLAYER_LEFT_PRINCESS_X = 3f
    const val PLAYER_LEFT_PRINCESS_Y = 24.5f
    const val PLAYER_RIGHT_PRINCESS_X = 15f
    const val PLAYER_RIGHT_PRINCESS_Y = 24.5f

    const val OPPONENT_KING_X = 9f
    const val OPPONENT_KING_Y = 4f
    const val OPPONENT_LEFT_PRINCESS_X = 3f
    const val OPPONENT_LEFT_PRINCESS_Y = 7.5f
    const val OPPONENT_RIGHT_PRINCESS_X = 15f
    const val OPPONENT_RIGHT_PRINCESS_Y = 7.5f

    // Bridge positions
    const val BRIDGE_LEFT_X = 3f
    const val BRIDGE_RIGHT_X = 15f
    const val BRIDGE_Y = 16f
    const val RIVER_Y = 16f

    // Tower stats
    const val KING_TOWER_HP = 4008
    const val KING_TOWER_DAMAGE = 109
    const val KING_TOWER_RANGE = 7f
    const val KING_TOWER_ATTACK_SPEED = 1.0f

    const val PRINCESS_TOWER_HP = 2534
    const val PRINCESS_TOWER_DAMAGE = 109
    const val PRINCESS_TOWER_RANGE = 7.5f
    const val PRINCESS_TOWER_ATTACK_SPEED = 0.8f

    // Game timing
    const val REGULAR_TIME = 180f // 3 minutes
    const val DOUBLE_ELIXIR_TIME = 60f // last minute
    const val OVERTIME_DURATION = 60f
    const val COUNTDOWN_DURATION = 3f
    const val ELIXIR_RATE_NORMAL = 1f / 2.8f // 1 elixir per 2.8 seconds
    const val ELIXIR_RATE_DOUBLE = 1f / 1.4f
    const val STARTING_ELIXIR = 5f

    // Gameplay
    const val MAX_ELIXIR = 10f
    const val TILE_SIZE_RATIO = 1f / ARENA_WIDTH // ratio of screen width per tile
    const val DEPLOY_ZONE_PLAYER_MIN_Y = 16.5f // just past river
    const val DEPLOY_ZONE_PLAYER_MAX_Y = 30f

    // Trophy changes
    const val WIN_TROPHIES = 30
    const val LOSS_TROPHIES = -15

    // Physics
    const val GAME_TICK_RATE = 60 // updates per second
    const val TICK_DURATION = 1f / GAME_TICK_RATE
}
