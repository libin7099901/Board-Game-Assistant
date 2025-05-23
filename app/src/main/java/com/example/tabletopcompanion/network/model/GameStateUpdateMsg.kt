package com.example.tabletopcompanion.network.model

data class GameStateUpdateMsg(
    val currentPlayerName: String?,
    val currentPhaseName: String?,
    val gameMessage: String?,
    val indicatorValues: Map<String, String>? // Added indicatorValues
    // Future: Could include full game state variables map if needed
) : P2PMessage() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
