package com.example.tabletopcompanion.model.game

data class GameState(
    val currentRound: Int = 1,
    val currentPlayerId: String? = null,
    val currentPhaseName: String? = null,
    val activeTemplateId: String? = null
)
