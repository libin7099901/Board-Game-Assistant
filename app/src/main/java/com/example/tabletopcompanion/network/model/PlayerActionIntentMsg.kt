package com.example.tabletopcompanion.network.model

// Ensure P2PMessage and its serialVersionUID are correctly defined in P2PMessage.kt
// Example: sealed class P2PMessage : java.io.Serializable { companion object { const val serialVersionUID: Long = 1L } }

data class PlayerActionIntentMsg(
    val playerId: String,
    val actionId: String,
    val parameters: Map<String, String>? = null // Parameters from input fields, if any
) : P2PMessage() {
    companion object {
        private const val serialVersionUID: Long = 1L // Keep consistent with P2PMessage
    }
}
