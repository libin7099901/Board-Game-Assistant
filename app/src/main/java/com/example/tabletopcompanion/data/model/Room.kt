package com.example.tabletopcompanion.data.model

data class Room(
    val roomId: String,
    val roomName: String,
    val host: Player,
    val players: List<Player>
    // Password can be added later
)
