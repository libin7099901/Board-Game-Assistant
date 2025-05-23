package com.example.tabletopcompanion.data.model

data class Player(
    val id: String,
    val username: String,
    val isHost: Boolean = false
)
