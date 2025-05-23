package com.example.tabletopcompanion.data.model.template

data class CustomComponentConfig(
    val id: String,
    val type: String, // e.g., "player_hand_view"
    val properties: Map<String, String>? // Keep properties simple for now
)
