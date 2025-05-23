package com.example.tabletopcompanion.data.model

import java.io.Serializable

data class Room(
    val roomId: String,
    val roomName: String,
    val host: Player,
    val players: List<Player>, // Player is Serializable
    val selectedTemplateId: String? = null,
    val gameTemplateName: String? = null,
    val currentGameState: String = "SETUP"
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
    // Ensure no direct reference to GameTemplate or other non-serializable complex objects here
    // @Transient private var fullGameTemplate: GameTemplate? = null // Example if it were needed
}
