package com.example.tabletopcompanion.network.model

data class PlayerLeftMsg(val playerId: String) : P2PMessage() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
